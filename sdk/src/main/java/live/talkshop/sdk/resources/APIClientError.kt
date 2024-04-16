package live.talkshop.sdk.resources

enum class APIClientError(private val code: String, private val description: String) {
    AUTHENTICATION_FAILED("AUTHENTICATION_FAILED", "Failed to authenticate"),
    AUTHENTICATION_EXCEPTION("AUTHENTICATION_EXCEPTION", "Exception during authentication"),
    USER_ALREADY_AUTHENTICATED("USER_ALREADY_AUTHENTICATED", "User already authenticated"),
    PERMISSION_DENIED("PERMISSION_DENIED", "Permission denied"),
    SHOW_NOT_FOUND("SHOW_NOT_FOUND", "Wrong Show key"),
    SHOW_UNKNOWN_EXCEPTION("SHOW_UNKNOWN_EXCEPTION", "Unknown exception in Show"),
    EVENT_NOT_FOUND("EVENT_NOT_FOUND", "Wrong Show Key"),
    EVENT_UNKNOWN_EXCEPTION("EVENT_UNKNOWN_EXCEPTION", "Unknown exception in Event"),
    INVALID_USER_TOKEN("INVALID_USER_TOKEN", "Messaging Token Invalid"),
    USER_TOKEN_EXCEPTION("USER_TOKEN_EXCEPTION", "Unknown user token exception"),
    CHANNEL_SUBSCRIPTION_FAILED("CHANNEL_SUBSCRIPTION_FAILED", "Failed to subscribe to channel"),
    MESSAGE_SENDING_FAILED("MESSAGE_SENDING_FAILED", "Failed to send message"),
    MESSAGE_LIST_FAILED("MESSAGE_LIST_FAILED", "Failed to fetch message history"),
    UNKNOWN_EXCEPTION("UNKNOWN_EXCEPTION", "Unknown exception occurred"),
    MESSAGE_ERROR_MESSAGE_MAX_LENGTH(
        "MESSAGE_ERROR_MESSAGE_MAX_LENGTH",
        "Publishing Error: Message exceeds maximum length of 200 characters."
    ),
    CHAT_CONNECTION_ERROR(
        "CHAT_CONNECTION_ERROR",
        "Cannot reconnect to chat due to network issues"
    ),
    CHAT_TIMEOUT("CHAT_TIMEOUT", "The chat you're trying to connect to has timed out"),
    SHOW_NOT_LIVE("SHOW_NOT_LIVE", "The show is not live.");

    override fun toString(): String {
        return "$code: $description"
    }
}