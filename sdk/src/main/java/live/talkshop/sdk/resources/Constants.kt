package live.talkshop.sdk.resources

internal object Constants {
    const val CC_FILENAME_END = ".transcript.vtt"
    const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    const val SDK_KEY = "x-tsl-sdk-key"
    const val AUTH_KEY = "Authorization"
    const val BEARER_KEY = "Bearer"
    const val SHARED_PREFS_NAME ="TalkShopLivePrefs"
    const val KEY_AUTHENTICATED = "authenticated"
    const val KEY_MP4_EXTENSION = ".mp4"

    const val MESSAGE_TYPE_GIPHY = "giphy"
    const val MESSAGE_TYPE_QUESTION = "question"
    const val MESSAGE_TYPE_COMMENT = "comment"

    const val CHANNEL_CHAT_PREFIX = "chat."
    const val CHANNEL_EVENTS_PREFIX = "events."

    const val PLATFORM_TYPE = "mobile"
    const val STATUS_LIVE = "live"

    const val COLLECTOR_CAT_PROCESS = "PROCESS"
    const val COLLECTOR_CAT_PAGE_VIEW = "PAGE_VIEW"
}