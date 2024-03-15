package live.talkshop.sdk.utils.networking

import live.talkshop.sdk.core.authentication.isTestMode
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Constants.CC_FILENAME_END
import live.talkshop.sdk.utils.helpers.HelperFunctions.isNotEmptyOrNull

object URLs {
    private const val URL_BASE_STAGING = "https://staging.cms.talkshop.live/"
    private const val URL_BASE_PROD = "https://cms.talkshop.live/"
    private const val URL_ASSET_BASE_STAGING = "https://assets-dev.talkshop.live/"
    private const val URL_ASSET_BASE_PROD = "https://assets.talkshop.live/"

    private const val PATH_AUTH = "api2/v1/sdk/"
    private const val PATH_SHOW_DETAILS = "api/products/digital/streaming_content/"
    private const val PATH_SHOWS = "api/shows/"
    private const val PATH_STREAMS_CURRENT = "streams/current/"
    private const val PATH_EVENTS = "events/"
    private const val PATH_GUEST_TOKEN = "chat/guest_token/"
    private const val PATH_FED_TOKEN = "chat/federated_user_token/"

    fun createHSLUrl(videoFilename: String): String? {
        return if (isNotEmptyOrNull(videoFilename)) {
            return if (isTestMode) {
                "${URL_ASSET_BASE_STAGING}${PATH_EVENTS}${videoFilename}"
            } else {
                "${URL_ASSET_BASE_PROD}${PATH_EVENTS}${videoFilename}"
            }
        } else {
            null
        }
    }

    fun createCCUrl(videoFilename: String): String? {
        return if (isNotEmptyOrNull(videoFilename)) {
            return if (isTestMode) {
                "${URL_ASSET_BASE_STAGING}${PATH_EVENTS}${
                    videoFilename.replace(
                        Constants.KEY_MP4_EXTENSION,
                        CC_FILENAME_END,
                        true
                    )
                }"
            } else {
                "${URL_ASSET_BASE_PROD}${PATH_EVENTS}${
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
            "${URL_BASE_STAGING}${PATH_SHOWS}$showKey/$PATH_STREAMS_CURRENT"
        } else {
            "${URL_BASE_PROD}${PATH_SHOWS}$showKey/$PATH_STREAMS_CURRENT"
        }
    }

    fun getShowDetailsUrl(showKey: String): String {
        return if (isTestMode) {
            "${URL_BASE_STAGING}${PATH_SHOW_DETAILS}$showKey"
        } else {
            "${URL_BASE_PROD}${PATH_SHOW_DETAILS}$showKey"
        }
    }

    fun getAuthUrl(): String {
        return if (isTestMode) {
            "${URL_BASE_STAGING}${PATH_AUTH}"
        } else {
            "${URL_BASE_PROD}${PATH_AUTH}"
        }
    }

    fun getUserTokenUrl(isGuest: Boolean): String {
        return if (isTestMode) {
            if (isGuest) "${URL_BASE_STAGING}${PATH_AUTH}${PATH_GUEST_TOKEN}" else "${URL_BASE_STAGING}${PATH_AUTH}${PATH_FED_TOKEN}"
        } else {
            if (isGuest) "${URL_BASE_PROD}${PATH_AUTH}${PATH_GUEST_TOKEN}" else "${URL_BASE_PROD}${PATH_AUTH}${PATH_FED_TOKEN}"
        }
    }
}