package live.talkshop.sdk.resources

enum class APIClientError(
    private var className: String,
    private val code: String,
    private val description: String
) {
    AUTHENTICATION_FAILED("TSL", "AUTHENTICATION_FAILED", "Failed to authenticate"),
    AUTHENTICATION_EXCEPTION("TSL", "AUTHENTICATION_EXCEPTION", "Exception during authentication"),
    USER_ALREADY_AUTHENTICATED("TSL", "USER_ALREADY_AUTHENTICATED", "User already authenticated"),
    PERMISSION_DENIED("TSL", "PERMISSION_DENIED", "Permission denied"),
    SHOW_NOT_FOUND("TSL", "SHOW_NOT_FOUND", "Wrong Show key"),
    SHOW_UNKNOWN_EXCEPTION("TSL", "SHOW_UNKNOWN_EXCEPTION", "Unknown exception in Show"),
    EVENT_NOT_FOUND("TSL", "EVENT_NOT_FOUND", "Wrong Show Key"),
    EVENT_UNKNOWN_EXCEPTION("TSL", "EVENT_UNKNOWN_EXCEPTION", "Unknown exception in Event"),
    INVALID_USER_TOKEN("TSL", "INVALID_USER_TOKEN", "Messaging Token Invalid"),
    USER_TOKEN_EXCEPTION("TSL", "USER_TOKEN_EXCEPTION", "Unknown user token exception"),
    CHANNEL_SUBSCRIPTION_FAILED(
        "TSL",
        "CHANNEL_SUBSCRIPTION_FAILED",
        "Failed to subscribe to channel"
    ),
    MESSAGE_SENDING_FAILED("TSL", "MESSAGE_SENDING_FAILED", "Failed to send message"),
    MESSAGE_LIST_FAILED("TSL", "MESSAGE_LIST_FAILED", "Failed to fetch message history"),
    UNKNOWN_EXCEPTION("TSL", "UNKNOWN_EXCEPTION", "Unknown exception occurred"),
    MESSAGE_ERROR_MESSAGE_MAX_LENGTH(
        "TSL",
        "MESSAGE_ERROR_MESSAGE_MAX_LENGTH",
        "Publishing Error: Message exceeds maximum length of 200 characters."
    ),
    CHAT_CONNECTION_ERROR(
        "TSL",
        "CHAT_CONNECTION_ERROR",
        "Cannot reconnect to chat due to network issues"
    ),
    CHAT_TOKEN_EXPIRED("TSL", "CHAT_TOKEN_EXPIRED", "The chat token has expired"),
    CHAT_TIMEOUT("TSL", "CHAT_TIMEOUT", "The chat you're trying to connect to has timed out"),
    SHOW_NOT_LIVE("TSL", "SHOW_NOT_LIVE", "The show is not live."),
    NO_PRODUCTS_FOUND("TSL", "NO_PRODUCTS_FOUND", "The show does not contain any products."),
    LIKE_COMMENT_FAILED("TSL", "LIKE_COMMENT_FAILED", "The comment could not be liked."),
    MESSAGE_SENDING_GIPHY_DATA_NOT_FOUND("TSL", "MESSAGE_SENDING_GIPHY_DATA_NOT_FOUND", "The Giphy data is missing (such as aspect ratio)"),
    COLLECTOR_EXCEPTION("TSL", "COLLECTOR_EXCEPTION", "The collector failed to collect the event");
    fun from(originatingClass: String): APIClientError {
        this.className = originatingClass
        return this
    }

    override fun toString(): String {
        return "$className.$code: $description"
    }
}