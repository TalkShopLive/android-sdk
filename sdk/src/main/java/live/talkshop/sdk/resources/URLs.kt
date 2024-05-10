package live.talkshop.sdk.resources

import live.talkshop.sdk.core.authentication.isTestMode
import live.talkshop.sdk.resources.Constants.CC_FILENAME_END
import live.talkshop.sdk.utils.helpers.HelperFunctions.isNotEmptyOrNull

object URLs {
    private const val URL_BASE_STAGING = "https://staging.cms.talkshop.live/"
    private const val URL_BASE_PROD = "https://cms.talkshop.live/"
    private const val URL_ASSET_BASE_STAGING = "https://assets-dev.talkshop.live/"
    private const val URL_ASSET_BASE_PROD = "https://assets.talkshop.live/"
    private const val URL_BASE_COLLECTOR_STAGING = "https://staging.collector.talkshop.live/"
    private const val URL_BASE_COLLECTOR_PROD = "https://collector.talkshop.live/"

    private const val URL_BASE_EVENTS_STAGING = "https://staging.events-api.talkshop.live/"
    private const val URL_BASE_EVENTS_PROD = "https://events-api.talkshop.live/"

    private const val PATH_AUTH = "api2/v1/sdk/"
    private const val PATH_SHOW_DETAILS = "api/products/digital/streaming_content/"
    private const val PATH_SHOWS = "api/shows/"
    private const val PATH_PRODUCTS = "api/fetch_multiple_products?per_page=50&"
    private const val PATH_STREAMS_CURRENT = "streams/current/"
    private const val PATH_EVENTS = "events/"
    private const val PATH_EVENT = "event/"
    private const val PATH_INCREMENT = "increment/"
    private const val PATH_GUEST_TOKEN = "chat/guest_token/"
    private const val PATH_FED_TOKEN = "chat/federated_user_token/"
    private const val PATH_MESSAGES = "chat/messages/"
    private const val PATH_COLLECT = "collect"
    private const val PATH_SENDERS_META = "api/messaging/senders/"

    fun createHSLUrl(videoFilename: String): String? {
        return if (isNotEmptyOrNull(videoFilename)) {
            return if (isTestMode) {
                "$URL_ASSET_BASE_STAGING$PATH_EVENTS${videoFilename}"
            } else {
                "$URL_ASSET_BASE_PROD$PATH_EVENTS${videoFilename}"
            }
        } else {
            null
        }
    }

    fun createCCUrl(videoFilename: String): String? {
        return if (isNotEmptyOrNull(videoFilename)) {
            return if (isTestMode) {
                "$URL_ASSET_BASE_STAGING$PATH_EVENTS${
                    videoFilename.replace(
                        Constants.KEY_MP4_EXTENSION,
                        CC_FILENAME_END,
                        true
                    )
                }"
            } else {
                "$URL_ASSET_BASE_PROD$PATH_EVENTS${
                    videoFilename.replace(
                        Constants.KEY_MP4_EXTENSION,
                        CC_FILENAME_END,
                        true
                    )
                }"
            }
        } else {
            null
        }
    }

    fun getCurrentStreamUrl(showKey: String): String {
        return if (isTestMode) {
            "$URL_BASE_STAGING$PATH_SHOWS$showKey/$PATH_STREAMS_CURRENT"
        } else {
            "$URL_BASE_PROD$PATH_SHOWS$showKey/$PATH_STREAMS_CURRENT"
        }
    }

    fun getShowDetailsUrl(showKey: String): String {
        return if (isTestMode) {
            "$URL_BASE_STAGING$PATH_SHOW_DETAILS$showKey"
        } else {
            "$URL_BASE_PROD$PATH_SHOW_DETAILS$showKey"
        }
    }

    fun getMultipleProducts(ids: List<Int>): String {
        val baseUrl = if (isTestMode) URL_BASE_STAGING else URL_BASE_PROD
        val idsQuery = ids.joinToString("&") { "ids[]=$it" }
        return "$baseUrl$PATH_PRODUCTS$idsQuery&order_by=array_order"
    }

    fun getAuthUrl(): String {
        return if (isTestMode) {
            "$URL_BASE_STAGING$PATH_AUTH"
        } else {
            "$URL_BASE_PROD$PATH_AUTH"
        }
    }

    fun getMessagesUrl(eventId: String, timeToken: String): String {
        return if (isTestMode) {
            "$URL_BASE_STAGING$PATH_AUTH$PATH_MESSAGES$eventId/$timeToken"
        } else {
            "$URL_BASE_PROD$PATH_AUTH$PATH_MESSAGES$eventId/$timeToken"
        }
    }

    fun getUserTokenUrl(isGuest: Boolean): String {
        return if (isTestMode) {
            if (isGuest) "$URL_BASE_STAGING$PATH_AUTH$PATH_GUEST_TOKEN" else "$URL_BASE_STAGING$PATH_AUTH$PATH_FED_TOKEN"
        } else {
            if (isGuest) "$URL_BASE_PROD$PATH_AUTH$PATH_GUEST_TOKEN" else "$URL_BASE_PROD$PATH_AUTH$PATH_FED_TOKEN"
        }
    }

    fun getIncrementViewUrl(eventId: String): String {
        return if (isTestMode) {
            "$URL_BASE_EVENTS_STAGING$PATH_EVENT$eventId/$PATH_INCREMENT"
        } else {
            "$URL_BASE_EVENTS_PROD$PATH_EVENT$eventId/$PATH_INCREMENT"
        }
    }

    fun getCollectorUrl(): String {
        return if (isTestMode) {
            "$URL_BASE_COLLECTOR_STAGING$PATH_COLLECT"
        } else {
            "$URL_BASE_COLLECTOR_PROD$PATH_COLLECT"
        }
    }

    fun getUserMetaUrl(uuid: String): String {
        return if (isTestMode) {
            "$URL_BASE_STAGING$PATH_SENDERS_META$uuid"
        } else {
            "$URL_BASE_PROD$PATH_SENDERS_META$uuid"
        }
    }
}