package live.talkshop.sdk.core.videoviews

import android.os.SystemClock
import live.talkshop.sdk.core.authentication.isDNT
import live.talkshop.sdk.utils.Collector
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Sends VIDEO_VIEW and VIDEO_ENGAGED_VIEW for the host app's player.
 *
 * The host app forwards its player events to the hooks below and the SDK applies the view rule:
 * - VIDEO_VIEW (`view_rule: first_frame`) once per play, at the first rendered frame while playing.
 * - VIDEO_ENGAGED_VIEW (`view_rule: engaged_3s`) once per play, after 3 s of watch time.
 *   The clock pauses while paused, buffering or while the app is in the background (including PiP).
 * - Pause/resume, seek, buffering, background/foreground, rotation and re-creating the player for
 *   the same source never send a new view. Replay after the end, a different show/stream and
 *   prelive -> live start a new play.
 * - With DNT on, both events are sent anonymously.
 *
 * Create one tracker per player screen and keep it for as long as the screen exists
 * (keep it in a ViewModel so rotation uses the same tracker), then call [release].
 * Every method is thread-safe.
 *
 * @param userId The user id sent as `user_id` (not sent when DNT is on).
 */
class VideoViewTracker(private val userId: String) {

    /**
     * The playback state reported by the host app's player.
     */
    enum class PlaybackState {
        PLAYING,
        PAUSED,
        BUFFERING
    }

    private val stateMachine = VideoViewStateMachine()
    private var engagedTimer: ScheduledFuture<*>? = null

    @Volatile
    private var released = false

    private val visibilityListener = object : AppVisibility.Listener {
        override fun onAppBackgrounded() {
            apply { machine, now -> machine.appBackgrounded(now) }
        }

        override fun onAppForegrounded() {
            apply { machine, now -> machine.appForegrounded(now) }
        }
    }

    init {
        AppVisibility.addListener(visibilityListener)
    }

    // Player hooks

    /**
     * Call when the player loads a stream, and again with every show status refresh.
     *
     * Calling it again with the same show, phase and stream does nothing. A different show,
     * phase (e.g. prelive -> live) or stream starts a new play. Call it before the player
     * starts the new stream. Pass null when nothing is loaded: the engaged clock pauses and
     * the current play is kept.
     */
    fun onSourceChanged(source: VideoViewSource?) {
        apply { machine, now -> machine.sourceChanged(source, now) }
    }

    /**
     * Call when the player renders its first video frame
     * (Media3: `Player.Listener.onRenderedFirstFrame`).
     */
    fun onFirstFrame() {
        apply { machine, now -> machine.firstFrame(now) }
    }

    /**
     * Call whenever the player starts playing, pauses or starts buffering
     * (Media3: `Player.Listener.onIsPlayingChanged` / `onPlaybackStateChanged`).
     */
    fun onPlaybackStateChanged(state: PlaybackState) {
        apply { machine, now -> machine.playbackStateChanged(state, now) }
    }

    /**
     * Call when playback reaches the end (Media3: `Player.STATE_ENDED`).
     * Playing again afterwards is a new play.
     */
    fun onPlaybackEnded() {
        apply { machine, now -> machine.playbackEnded(now) }
    }

    /**
     * Call when the player screen goes away (e.g. `ViewModel.onCleared`).
     * Cancels the pending engaged timer; hooks called afterwards are ignored. Safe to call twice.
     */
    fun release() {
        if (released) return
        released = true
        AppVisibility.removeListener(visibilityListener)
        executor.execute {
            engagedTimer?.cancel(false)
            engagedTimer = null
        }
    }

    // State machine

    /** Captures the time on the calling thread, then runs the transition on the tracker thread. */
    private fun apply(transition: (VideoViewStateMachine, Long) -> List<VideoViewEffect>) {
        if (released) return
        val now = SystemClock.uptimeMillis()
        executor.execute {
            runSafely {
                if (!released) {
                    transition(stateMachine, now).forEach(::perform)
                }
            }
        }
    }

    private fun perform(effect: VideoViewEffect) {
        when (effect) {
            is VideoViewEffect.Send -> send(effect.event, effect.source)
            is VideoViewEffect.ScheduleEngagedTimer -> {
                engagedTimer?.cancel(false)
                val generation = effect.generation
                engagedTimer = executor.schedule({
                    runSafely {
                        if (!released) {
                            stateMachine.engagedTimerFired(generation, SystemClock.uptimeMillis())
                                .forEach(::perform)
                        }
                    }
                }, effect.afterMs, TimeUnit.MILLISECONDS)
            }

            VideoViewEffect.CancelEngagedTimer -> {
                engagedTimer?.cancel(false)
                engagedTimer = null
            }
        }
    }

    private inline fun runSafely(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            // Never crash the host app from the tracker thread; the event is dropped.
        }
    }

    // Sending

    private fun send(event: VideoViewEvent, source: VideoViewSource) {
        val anonymous = isDNT
        Collector.collectVideoView(
            action = event.action,
            viewRule = event.viewRule,
            showId = source.showId,
            showKey = source.showKey,
            eventId = source.eventId,
            storeId = source.storeId,
            videoStatus = source.videoStatus.rawValue,
            showTitle = source.title,
            userId = userId,
            anonymous = anonymous
        )
    }

    private companion object {
        /** One serial thread for all trackers: keeps each tracker's events in order. */
        val executor = ScheduledThreadPoolExecutor(1) { runnable ->
            Thread(runnable, "tsl-videoviews").apply { isDaemon = true }
        }.apply { removeOnCancelPolicy = true }
    }
}
