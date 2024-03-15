package live.talkshop.sdk.core.chat.models

/**
 * Represents a model for user token data retrieved from an API.
 *
 * @property publishKey The publish key associated with the user session, used for PubNub publishing.
 * @property subscribeKey The subscribe key associated with the user session, used for PubNub subscription.
 * @property token The authentication token for the user session.
 * @property userId The user ID, which may not be present in all responses.
 * @property name The optional user name, which may not be present in all responses.
 */
data class UserTokenModel(
    val publishKey: String,
    val subscribeKey: String,
    val token: String,
    val userId: String,
    val name: String
)