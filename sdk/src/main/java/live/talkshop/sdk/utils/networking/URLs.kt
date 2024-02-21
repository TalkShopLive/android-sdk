package live.talkshop.sdk.utils.networking

import live.talkshop.sdk.resources.Constants.CC_FILENAME_END

object URLs {
    private const val URL_BASE = "https://staging.cms.talkshop.live/api/"
    private const val URL_CC_BASE = "https://assets.talkshop.live/events/"

    private const val PATH_SHOW_DETAILS = "products/digital/streaming_content/"
    private const val PATH_SHOWS = "shows/"
    const val PATH_CURRENT_EVENT = "streams/current/"

    fun createCCUrl(videoFilename: String): String {
        return "${URL_CC_BASE}${videoFilename}${CC_FILENAME_END}"
    }

    const val SHOW_DETAILS_ENDPOINT_URL = "${URL_BASE}${PATH_SHOW_DETAILS}"
    const val CURRENT_EVENT_ENDPOINT_URL = "${URL_BASE}${PATH_SHOWS}"
}