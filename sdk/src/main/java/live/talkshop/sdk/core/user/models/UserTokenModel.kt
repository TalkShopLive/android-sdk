package live.talkshop.sdk.core.user.models

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

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
) {
    companion object {
        /**
         * Parses a JSON string into a UserTokenModel.
         * @param jsonString The JSON response string from the API.
         * @return A UserTokenModel instance or null if parsing fails.
         */
        fun fromJsonString(jsonString: String): UserTokenModel? {
            return try {
                val jsonResponse = JSONObject(jsonString)
                UserTokenModel(
                    publishKey = jsonResponse.getString("publish_key"),
                    subscribeKey = jsonResponse.getString("subscribe_key"),
                    token = jsonResponse.getString("token"),
                    userId = jsonResponse.getString("user_id")
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}