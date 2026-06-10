package live.talkshop.sdk

import kotlinx.coroutines.test.runTest
import live.talkshop.sdk.core.authentication.currentShow
import live.talkshop.sdk.core.authentication.globalShowId
import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.chat.ChatProvider
import live.talkshop.sdk.core.chat.models.UserTokenModel
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.resources.APIClientError
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Offline unit tests for the prelive chat gate: chat initialization must be
 * rejected with SHOW_IS_PRELIVE while the show is in prelive state.
 * No network required — the show is served from the in-memory cache.
 */
class ChatPreliveGateTests {

    @After
    fun tearDown() {
        isAuthenticated = false
        globalShowId = null
        currentShow = null
    }

    private fun cachedShow(status: String?) = ShowModel(
        id = 1,
        showKey = SHOW_KEY,
        name = "Test Show",
        showDescription = null,
        status = status,
        trailerUrl = null,
        hlsPlaybackUrl = null,
        hlsUrl = null,
        airDate = null,
        eventId = null,
        storeId = "",
        cc = null,
        endedAt = null,
        duration = null,
        videoThumbnailUrl = "",
        channelLogo = "",
        channelName = "",
        trailerDuration = null
    )

    private fun initiateChat(status: String?): Pair<APIClientError?, UserTokenModel?> {
        isAuthenticated = true
        globalShowId = SHOW_KEY
        currentShow = cachedShow(status)

        var error: APIClientError? = null
        var token: UserTokenModel? = null
        runTest {
            ChatProvider().initiateChat(SHOW_KEY, "jwt", true) { e, t ->
                error = e
                token = t
            }
        }
        return error to token
    }

    @Test
    fun initiateChat_preliveShow_returnsShowIsPreliveError() {
        val (error, token) = initiateChat("prelive")
        assertEquals(APIClientError.SHOW_IS_PRELIVE, error)
        assertNull(token)
    }

    @Test
    fun initiateChat_preliveShow_statusCheckIsCaseInsensitive() {
        val (error, token) = initiateChat("PreLive")
        assertEquals(APIClientError.SHOW_IS_PRELIVE, error)
        assertNull(token)
    }

    private companion object {
        const val SHOW_KEY = "testShowKey"
    }
}
