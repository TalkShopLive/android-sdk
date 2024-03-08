package live.talkshop.sdk.utils.parsers

import live.talkshop.sdk.core.user.models.UserTokenModel
import live.talkshop.sdk.resources.Keys.KEY_PUBLISH_KEY
import live.talkshop.sdk.resources.Keys.KEY_SUBSCRIBE_KEY
import live.talkshop.sdk.resources.Keys.KEY_TOKEN
import live.talkshop.sdk.resources.Keys.KEY_USER_ID
import org.json.JSONObject

internal object UserTokenParser {
    /**
     * Parses a JSON string into a UserTokenModel.
     * @param jsonString The JSON response string from the API.
     * @return A UserTokenModel instance or null if parsing fails.
     */
    fun fromJsonString(jsonString: String): UserTokenModel? {
        return try {
            val jsonResponse = JSONObject(jsonString)
            UserTokenModel(
                publishKey = jsonResponse.getString(KEY_PUBLISH_KEY),
                subscribeKey = jsonResponse.getString(KEY_SUBSCRIBE_KEY),
                token = jsonResponse.getString(KEY_TOKEN),
                userId = jsonResponse.getString(KEY_USER_ID)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}