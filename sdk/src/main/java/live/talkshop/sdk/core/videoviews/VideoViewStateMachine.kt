package live.talkshop.sdk.core.videoviews

import live.talkshop.sdk.resources.CollectorActions

/**
 * The two view events the tracker sends.
 */
internal enum class VideoViewEvent(val action: CollectorActions, val viewRule: String) {
    VIEW(CollectorActions.VIDEO_VIEW, "first_frame"),
    ENGAGED(CollectorActions.VIDEO_ENGAGED_VIEW, "engaged_3s")
}

/**
 * Side effects the tracker performs after a state machine transition.
 */
internal sealed class VideoViewEffect {
    data class Send(val event: VideoViewEvent, val source: VideoViewSource) : VideoViewEffect()
    data class ScheduleEngagedTimer(val afterMs: Long, val generation: Int) : VideoViewEffect()
    object CancelEngagedTimer : VideoViewEffect()
}

/**
 * Applies the view rule (same as web/embed):
 * - VIDEO_VIEW once per play, at the first rendered frame while the player is playing.
 * - VIDEO_ENGAGED_VIEW once per play, after 3 s of watch time. The clock pauses while paused,
 *   buffering or backgrounded.
 * - A new play starts on replay after the end and when the source changes to a different
 *   show, phase or stream.
 *
 * Pure and synchronous: every input takes the current monotonic time in milliseconds
 * and returns the effects to perform. Not thread-safe; the tracker confines it to one thread.
 */
internal class VideoViewStateMachine {

    companion object {
        const val ENGAGED_THRESHOLD_MS = 3000L
    }

    // State

    var source: VideoViewSource? = null // Current metadata; null = no source.
        private set
    private var playIdentity: VideoViewSource? = null // Identity of the current play; kept while source is null.
    private var isPlaying = false
    private var firstFrameSeen = false
    private var viewSent = false
    private var engagedSent = false
    private var clockStartedAt: Long? = null
    private var accumulatedMs = 0L
    private var timerGeneration = 0
    private var timerArmed = false
    private var endedForReplay = false
    private var backgrounded = false

    // Inputs

    fun sourceChanged(newSource: VideoViewSource?, now: Long): List<VideoViewEffect> {
        if (newSource == null) {
            // No source: pause the clock, keep the play (A -> null -> A is the same play).
            source = null
            return pauseClock(now)
        }

        val effects = mutableListOf<VideoViewEffect>()
        val identity = playIdentity
        if (identity != null) {
            if (newSource.isSamePlay(identity)) {
                // Re-created player, repeated status, live -> transcoding: same play.
                if (identity.playbackUrl == null && newSource.playbackUrl != null) {
                    playIdentity = newSource
                }
            } else {
                // Different show, phase or stream: new play.
                effects += resetPlay(now)
                playIdentity = newSource
            }
        } else {
            playIdentity = newSource
        }
        source = newSource
        return effects + advance(now)
    }

    fun firstFrame(now: Long): List<VideoViewEffect> {
        firstFrameSeen = true
        return advance(now)
    }

    fun playbackStateChanged(state: VideoViewTracker.PlaybackState, now: Long): List<VideoViewEffect> =
        when (state) {
            VideoViewTracker.PlaybackState.PLAYING -> {
                isPlaying = true
                if (endedForReplay) {
                    // Replay of the same item: its frame is already on screen,
                    // and the player will not report a new first frame.
                    endedForReplay = false
                    firstFrameSeen = true
                }
                advance(now)
            }

            VideoViewTracker.PlaybackState.PAUSED,
            VideoViewTracker.PlaybackState.BUFFERING -> {
                isPlaying = false
                pauseClock(now)
            }
        }

    fun playbackEnded(now: Long): List<VideoViewEffect> {
        val effects = resetPlay(now)
        endedForReplay = true
        return effects
    }

    fun appBackgrounded(now: Long): List<VideoViewEffect> {
        backgrounded = true
        return pauseClock(now)
    }

    fun appForegrounded(now: Long): List<VideoViewEffect> {
        backgrounded = false
        return startClock(now)
    }

    fun engagedTimerFired(generation: Int, now: Long): List<VideoViewEffect> {
        if (generation != timerGeneration || !timerArmed || !viewSent || engagedSent) {
            return emptyList()
        }
        timerArmed = false
        bank(now)
        if (accumulatedMs < ENGAGED_THRESHOLD_MS) {
            return startClock(now)
        }
        val source = source ?: return emptyList()
        engagedSent = true
        accumulatedMs = ENGAGED_THRESHOLD_MS
        return listOf(VideoViewEffect.Send(VideoViewEvent.ENGAGED, source))
    }

    // Rule

    private fun advance(now: Long): List<VideoViewEffect> =
        if (viewSent) startClock(now) else maybeSendView(now)

    private fun maybeSendView(now: Long): List<VideoViewEffect> {
        val source = source
        if (viewSent || !firstFrameSeen || !isPlaying || source == null) {
            return emptyList()
        }
        viewSent = true
        val effects = resetEngaged()
        return effects + VideoViewEffect.Send(VideoViewEvent.VIEW, source) + startClock(now)
    }

    private fun startClock(now: Long): List<VideoViewEffect> {
        val source = source
        if (!viewSent || engagedSent || !isPlaying || backgrounded || source == null) {
            return emptyList()
        }
        bank(now)
        clockStartedAt = now
        timerGeneration += 1
        val remaining = ENGAGED_THRESHOLD_MS - minOf(accumulatedMs, ENGAGED_THRESHOLD_MS)
        if (remaining <= 0) {
            timerArmed = false
            clockStartedAt = null
            engagedSent = true
            return listOf(VideoViewEffect.Send(VideoViewEvent.ENGAGED, source))
        }
        timerArmed = true
        return listOf(VideoViewEffect.ScheduleEngagedTimer(remaining, timerGeneration))
    }

    private fun pauseClock(now: Long): List<VideoViewEffect> {
        bank(now)
        if (!timerArmed) {
            return emptyList()
        }
        timerArmed = false
        timerGeneration += 1
        return listOf(VideoViewEffect.CancelEngagedTimer)
    }

    private fun bank(now: Long) {
        val startedAt = clockStartedAt ?: return
        accumulatedMs += if (now > startedAt) now - startedAt else 0
        clockStartedAt = null
    }

    private fun resetEngaged(): List<VideoViewEffect> {
        clockStartedAt = null
        accumulatedMs = 0
        engagedSent = false
        if (!timerArmed) {
            return emptyList()
        }
        timerArmed = false
        timerGeneration += 1
        return listOf(VideoViewEffect.CancelEngagedTimer)
    }

    private fun resetPlay(now: Long): List<VideoViewEffect> {
        val effects = pauseClock(now)
        viewSent = false
        firstFrameSeen = false
        endedForReplay = false
        return effects + resetEngaged()
    }
}
