package live.talkshop.sdk

import live.talkshop.sdk.core.videoviews.VideoViewEffect
import live.talkshop.sdk.core.videoviews.VideoViewEvent
import live.talkshop.sdk.core.videoviews.VideoViewSource
import live.talkshop.sdk.core.videoviews.VideoViewSource.VideoStatus
import live.talkshop.sdk.core.videoviews.VideoViewStateMachine
import live.talkshop.sdk.core.videoviews.VideoViewTracker.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

/**
 * Offline unit tests for the video view rule (VIDEO_VIEW / VIDEO_ENGAGED_VIEW).
 * The state machine is driven with a manual clock, so no test waits for real time.
 */
class VideoViewStateMachineTests {

    // VIDEO_VIEW

    @Test
    fun view_firstFrameWhilePlaying_sentOnce() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)

        assertEquals(view(at = 0), driver.sent.first())
        assertEquals(1, driver.sent.count { it.event == VideoViewEvent.VIEW })
    }

    @Test
    fun view_firstFrameWhilePaused_waitsForPlaying() {
        val driver = Driver()
        driver.source(VOD_A, at = 0)
        driver.state(PlaybackState.PAUSED, at = 0)
        driver.firstFrame(at = 500)
        driver.state(PlaybackState.PLAYING, at = 2000)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 2000), engaged(at = 5000)), driver.sent)
    }

    @Test
    fun view_playingWithoutFirstFrame_notSent() {
        val driver = Driver()
        driver.source(VOD_A, at = 0)
        driver.state(PlaybackState.PLAYING, at = 0)
        driver.advance(to = 60_000)

        assertEquals(emptyList<Driver.Sent>(), driver.sent)
    }

    @Test
    fun view_noSource_sentWhenSourceArrives() {
        val driver = Driver()
        driver.state(PlaybackState.PLAYING, at = 0)
        driver.firstFrame(at = 0)
        driver.advance(to = 1000)
        assertEquals(emptyList<Driver.Sent>(), driver.sent)

        driver.source(VOD_A, at = 1000)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 1000), engaged(at = 4000)), driver.sent)
    }

    @Test
    fun view_repeatedPlayingAndFirstFrame_noDuplicates() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.state(PlaybackState.PLAYING, at = 500)
        driver.firstFrame(at = 700)
        driver.state(PlaybackState.PLAYING, at = 1000)
        driver.state(PlaybackState.PLAYING, at = 2500)
        driver.state(PlaybackState.PLAYING, at = 4000)
        driver.firstFrame(at = 4500)
        driver.state(PlaybackState.PLAYING, at = 9000)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 0), engaged(at = 3000)), driver.sent)
    }

    // VIDEO_ENGAGED_VIEW

    @Test
    fun engaged_after3sOfPlaying_sentOnce() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 0), engaged(at = 3000)), driver.sent)
        assertNull(driver.pendingTimer)
    }

    @Test
    fun engaged_pauseAt2sAndResume_sentWhen3sOfWatchTimeReached() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.state(PlaybackState.PAUSED, at = 2000)
        driver.state(PlaybackState.PLAYING, at = 10_000)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 0), engaged(at = 11_000)), driver.sent)
    }

    @Test
    fun engaged_buffering_pausesClock() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.state(PlaybackState.BUFFERING, at = 1000)
        driver.state(PlaybackState.PLAYING, at = 4000)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 0), engaged(at = 6000)), driver.sent)
    }

    @Test
    fun engaged_background_pausesClock_noNewView() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.background(at = 1000)
        driver.foreground(at = 5000)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 0), engaged(at = 7000)), driver.sent)
    }

    @Test
    fun engaged_playingReportedWhileInBackground_clockStaysPaused() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.background(at = 1000)
        driver.state(PlaybackState.PLAYING, at = 2000) // e.g. background audio keeps the player playing
        driver.source(VOD_A, at = 2500) // status refresh while in the background
        driver.advance(to = 20_000)
        assertEquals(listOf(view(at = 0)), driver.sent)

        driver.foreground(at = 20_000)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 0), engaged(at = 22_000)), driver.sent)
    }

    @Test
    fun engaged_foregroundWhilePaused_doesNotStartClock() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.background(at = 1000)
        driver.state(PlaybackState.PAUSED, at = 1000)
        driver.foreground(at = 5000)
        driver.advance(to = 20_000)
        assertEquals(listOf(view(at = 0)), driver.sent)

        driver.state(PlaybackState.PLAYING, at = 20_000)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 0), engaged(at = 22_000)), driver.sent)
    }

    @Test
    fun engaged_afterEngagedSent_noMoreEvents() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.advance(to = 4000)
        driver.state(PlaybackState.PAUSED, at = 4000)
        driver.state(PlaybackState.PLAYING, at = 5000)
        driver.background(at = 6000)
        driver.foreground(at = 7000)
        driver.state(PlaybackState.BUFFERING, at = 8000)
        driver.state(PlaybackState.PLAYING, at = 9000)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 0), engaged(at = 3000)), driver.sent)
        assertNull(driver.pendingTimer)
    }

    @Test
    fun engaged_staleTimerAfterPauseAndResume_ignored() {
        val machine = VideoViewStateMachine()
        machine.sourceChanged(VOD_A, now = 0)
        machine.playbackStateChanged(PlaybackState.PLAYING, now = 0)
        val timer = machine.firstFrame(now = 0).lastOrNull() as? VideoViewEffect.ScheduleEngagedTimer
            ?: return fail("Expected an engaged timer")
        machine.playbackStateChanged(PlaybackState.PAUSED, now = 1000)
        machine.playbackStateChanged(PlaybackState.PLAYING, now = 2000) // a new timer for the remaining 2000 ms

        assertEquals(emptyList<VideoViewEffect>(), machine.engagedTimerFired(timer.generation, now = 3000))
    }

    @Test
    fun engaged_nullSource_pausesClockAndKeepsPlay() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.source(null, at = 1000)
        driver.source(VOD_A, at = 5000)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 0), engaged(at = 7000)), driver.sent)
    }

    // New play / same play

    @Test
    fun newPlay_replayAfterEnd_sendsNewViewAndEngaged() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.ended(at = 10_000)
        driver.state(PlaybackState.PAUSED, at = 10_000)
        driver.state(PlaybackState.PLAYING, at = 12_000) // The player reports no new first frame on replay.
        driver.advance(to = 60_000)

        assertEquals(
            listOf(view(at = 0), engaged(at = 3000), view(at = 12_000), engaged(at = 15_000)),
            driver.sent
        )
    }

    @Test
    fun newPlay_replayBeforeEngaged_restartsEngagedClock() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.ended(at = 2000)
        driver.state(PlaybackState.PAUSED, at = 2000)
        driver.state(PlaybackState.PLAYING, at = 5000)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 0), view(at = 5000), engaged(at = 8000)), driver.sent)
    }

    @Test
    fun newPlay_differentShow_sendsNewViewAndEngaged() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.source(VOD_B, at = 10_000)
        driver.firstFrame(at = 10_500)
        driver.advance(to = 60_000)

        assertEquals(
            listOf(
                view(at = 0), engaged(at = 3000),
                view(at = 10_500, showId = 2), engaged(at = 13_500, showId = 2),
            ),
            driver.sent
        )
    }

    @Test
    fun newPlay_differentShowBeforeEngaged_dropsFirstEngaged() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        driver.source(VOD_B, at = 2000)
        driver.firstFrame(at = 2200)
        driver.advance(to = 60_000)

        assertEquals(
            listOf(view(at = 0), view(at = 2200, showId = 2), engaged(at = 5200, showId = 2)),
            driver.sent
        )
    }

    @Test
    fun newPlay_preliveToLiveToVod_sameShow_eachPhaseIsANewPlay() {
        val driver = Driver()
        driver.startPlaying(source(1, VideoStatus.PRELIVE, "https://cdn.example.com/trailer.mp4"), at = 0)
        driver.source(source(1, VideoStatus.LIVE, "https://cdn.example.com/live/index.m3u8"), at = 10_000)
        driver.firstFrame(at = 10_300)
        driver.source(source(1, VideoStatus.VOD, "https://cdn.example.com/vod/index.m3u8"), at = 20_000)
        driver.firstFrame(at = 20_200)
        driver.advance(to = 60_000)

        assertEquals(listOf(0L, 3000L, 10_300L, 13_300L, 20_200L, 23_200L), driver.sent.map { it.at })
        assertEquals(
            listOf(
                VideoStatus.PRELIVE, VideoStatus.PRELIVE,
                VideoStatus.LIVE, VideoStatus.LIVE,
                VideoStatus.VOD, VideoStatus.VOD,
            ),
            driver.sent.map { it.videoStatus }
        )
    }

    @Test
    fun samePlay_recreatedPlayerWithNewUrlToken_noNewView() {
        val driver = Driver()
        driver.startPlaying(source(1, VideoStatus.VOD, "https://cdn.example.com/vod/index.m3u8?token=1"), at = 0)
        driver.source(source(1, VideoStatus.VOD, "https://CDN.example.com/vod/index.m3u8?token=2"), at = 1500)
        driver.state(PlaybackState.BUFFERING, at = 1500)
        driver.firstFrame(at = 1600)
        driver.state(PlaybackState.PLAYING, at = 1600)
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 0), engaged(at = 3100)), driver.sent)
    }

    @Test
    fun samePlay_repeatedStatusRefresh_noNewView() {
        val driver = Driver()
        driver.startPlaying(VOD_A, at = 0)
        for (time in 500L..20_000L step 500L) {
            driver.source(VOD_A, at = time)
        }
        driver.advance(to = 60_000)

        assertEquals(listOf(view(at = 0), engaged(at = 3000)), driver.sent)
    }

    private companion object {
        val VOD_A = source(1, VideoStatus.VOD, "https://cdn.example.com/a/index.m3u8")
        val VOD_B = source(2, VideoStatus.VOD, "https://cdn.example.com/b/index.m3u8")

        fun source(showId: Int, status: VideoStatus, url: String) = VideoViewSource(
            showId = showId,
            showKey = "show-$showId",
            eventId = null,
            storeId = null,
            videoStatus = status,
            playbackUrl = url,
            title = null
        )

        fun view(at: Long, showId: Int = 1) = Driver.Sent(VideoViewEvent.VIEW, at, showId)

        fun engaged(at: Long, showId: Int = 1) = Driver.Sent(VideoViewEvent.ENGAGED, at, showId)
    }
}

/**
 * Drives [VideoViewStateMachine] the way the tracker does: a manual clock in milliseconds
 * and one pending engaged timer (a new schedule replaces it, a cancel removes it).
 */
private class Driver {

    data class Sent(
        val event: VideoViewEvent,
        val at: Long,
        val showId: Int = 1,
        val videoStatus: VideoStatus = VideoStatus.VOD,
    )

    data class Timer(val fireAt: Long, val generation: Int)

    private val machine = VideoViewStateMachine()
    private var now = 0L

    var pendingTimer: Timer? = null
        private set
    val sent = mutableListOf<Sent>()

    fun source(source: VideoViewSource?, at: Long) = input(at) { time -> machine.sourceChanged(source, time) }

    fun firstFrame(at: Long) = input(at) { time -> machine.firstFrame(time) }

    fun state(state: PlaybackState, at: Long) = input(at) { time -> machine.playbackStateChanged(state, time) }

    fun ended(at: Long) = input(at) { time -> machine.playbackEnded(time) }

    fun background(at: Long) = input(at) { time -> machine.appBackgrounded(time) }

    fun foreground(at: Long) = input(at) { time -> machine.appForegrounded(time) }

    /** Source, playing and first frame at the same moment: the usual start of a play. */
    fun startPlaying(source: VideoViewSource, at: Long) {
        this.source(source, at)
        state(PlaybackState.PLAYING, at)
        firstFrame(at)
    }

    /** Moves the clock forward, firing the engaged timer when it is due. Inputs must be in time order. */
    fun advance(to: Long) {
        require(to >= now) { "Driver inputs must be in time order" }
        while (true) {
            val timer = pendingTimer?.takeIf { it.fireAt <= to } ?: break
            now = timer.fireAt
            pendingTimer = null
            perform(machine.engagedTimerFired(timer.generation, now))
        }
        now = to
    }

    private fun input(at: Long, transition: (Long) -> List<VideoViewEffect>) {
        advance(at)
        perform(transition(now))
    }

    private fun perform(effects: List<VideoViewEffect>) {
        effects.forEach { effect ->
            when (effect) {
                is VideoViewEffect.Send ->
                    sent += Sent(effect.event, now, effect.source.showId, effect.source.videoStatus)
                is VideoViewEffect.ScheduleEngagedTimer ->
                    pendingTimer = Timer(now + effect.afterMs, effect.generation)
                VideoViewEffect.CancelEngagedTimer ->
                    pendingTimer = null
            }
        }
    }
}
