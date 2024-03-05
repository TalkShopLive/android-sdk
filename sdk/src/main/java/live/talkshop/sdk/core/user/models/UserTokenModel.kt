package live.talkshop.sdk.core.user.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a model for user token data retrieved from an API.
 *
 * @property publishKey The publish key associated with the user session, used for PubNub publishing.
 * @property subscribeKey The subscribe key associated with the user session, used for PubNub subscription.
 * @property token The authentication token for the user session.
 * @property userId The optional user ID, which may not be present in all responses.
 */
data class UserTokenModel(
    @SerializedName("publish_key")
    val publishKey: String,

    @SerializedName("subscribe_key")
    val subscribeKey: String,

    @SerializedName("token")
    val token: String,

    @SerializedName("user_id")
    val userId: String? = null
)