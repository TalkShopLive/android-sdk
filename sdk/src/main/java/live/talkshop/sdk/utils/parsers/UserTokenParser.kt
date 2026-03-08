package live.talkshop.sdk.utils.parsers

import live.talkshop.sdk.core.chat.models.UserTokenModel
import live.talkshop.sdk.resources.Keys.KEY_CHANNELS
import live.talkshop.sdk.resources.Keys.KEY_CHAT
import live.talkshop.sdk.resources.Keys.KEY_CHAT_ID
import live.talkshop.sdk.resources.Keys.KEY_EVENTS
import live.talkshop.sdk.resources.Keys.KEY_NAME
import live.talkshop.sdk.resources.Keys.KEY_PUBLISH_KEY
import live.talkshop.sdk.resources.Keys.KEY_SUBSCRIBE_KEY
import live.talkshop.sdk.resources.Keys.KEY_TOKEN
import live.talkshop.sdk.resources.Keys.KEY_USER_ID
import live.talkshop.sdk.resources.Keys.KEY_USER_ID_CAMEL
import live.talkshop.sdk.utils.Logging
import org.json.JSONObject

internal object UserTokenParser {

    fun fromJsonString(jsonString: String): UserTokenModel? {
        return try {
            val jsonResponse = JSONObject(jsonString)

            val userId = jsonResponse.optString(KEY_USER_ID).ifBlank {
                jsonResponse.optString(KEY_USER_ID_CAMEL)
            }

            val channelsJson = jsonResponse.optJSONObject(KEY_CHANNELS)

            UserTokenModel(
                publishKey = jsonResponse.optString(KEY_PUBLISH_KEY),
                subscribeKey = jsonResponse.optString(KEY_SUBSCRIBE_KEY),
                token = jsonResponse.optString(KEY_TOKEN),
                userId = userId,
                name = jsonResponse.optString(KEY_NAME).ifBlank { userId },
                chatChannel = channelsJson?.optString(KEY_CHAT)?.takeIf { it.isNotBlank() },
                eventsChannel = channelsJson?.optString(KEY_EVENTS)?.takeIf { it.isNotBlank() },
                chatId = if (jsonResponse.has(KEY_CHAT_ID)) jsonResponse.optInt(KEY_CHAT_ID) else null
            )
        } catch (e: Exception) {
            Logging.print(UserTokenParser::class.java, e)
            null
        }
    }
}