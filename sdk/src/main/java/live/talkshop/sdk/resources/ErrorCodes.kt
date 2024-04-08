package live.talkshop.sdk.resources

object ErrorCodes {
    // Init module
    const val AUTHENTICATION_FAILED = "Failed to authenticate"
    const val AUTHENTICATION_EXCEPTION = "Exception during authentication"
    const val USER_ALREADY_AUTHENTICATED = "User already authenticated"
    const val PERMISSION_DENIED = "Permission denied"

    // Show module
    const val SHOW_NOT_FOUND = "Wrong Show key"
    const val SHOW_UNKNOWN_EXCEPTION = "Unknown exception in Show"
    const val EVENT_NOT_FOUND = "Wrong Show Key"
    const val EVENT_UNKNOWN_EXCEPTION = "Unknown exception in Event"

    // Chat module
    const val INVALID_USER_TOKEN = "Messaging Token Invalid"
    const val USER_TOKEN_EXPIRED = "User token has expired"
    const val USER_TOKEN_EXCEPTION = "Unknown user token exception"
    const val CHANNEL_SUBSCRIPTION_FAILED = "Failed to subscribe to channel"
    const val MESSAGE_SENDING_FAILED = "Failed to send message"
    const val MESSAGE_LIST_FAILED = "Failed to fetch message history"
    const val UNKNOWN_EXCEPTION = "Unknown exception occurred"
}