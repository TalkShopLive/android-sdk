package live.talkshop.sdk.utils.networking

import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Constants.CC_FILENAME_END

object URLs {
    private const val URL_BASE = "https://staging.cms.talkshop.live/"
    private const val URL_CC_BASE = "https://assets.talkshop.live/events/"

    private const val PATH_AUTH = "/api2/v1/sdk/"
    private const val PATH_SHOW_DETAILS = "api/products/digital/streaming_content/"
    private const val PATH_SHOWS = "api/shows/"
    const val PATH_CURRENT_EVENT = "streams/current/"

    fun createHSLUrl(videoFilename: String): String? {
        return if (videoFilename.isNullOrEmpty()) {
            "${URL_CC_BASE}${videoFilename}"
        } else {
            null
        }
    }

    fun createCCUrl(videoFilename: String): String? {
        return if (videoFilename.isNullOrEmpty()) {
            "${URL_CC_BASE}${
                videoFilename.replace(
                    Constants.KEY_MP4_EXTENSION,
                    CC_FILENAME_END,
                    true
                )
            }"
        } else {
            null
        }
    }

    const val URL_SHOW_DETAILS_ENDPOINT = "${URL_BASE}${PATH_SHOW_DETAILS}"
    const val URL_CURRENT_EVENT_ENDPOINT = "${URL_BASE}${PATH_SHOWS}"
    const val URL_AUTH_ENDPOINT = "${URL_BASE}${PATH_AUTH}"
}