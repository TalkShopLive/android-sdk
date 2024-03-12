package live.talkshop.sdk.utils.parsers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import live.talkshop.sdk.core.chat.models.MessageModel
import live.talkshop.sdk.core.chat.models.MessageSender
import live.talkshop.sdk.core.chat.models.SenderModel
import live.talkshop.sdk.resources.Constants.ENUM_MESSAGE_TYPE_COMMENT
import live.talkshop.sdk.resources.Keys.KEY_CHANNEL_CODE
import live.talkshop.sdk.resources.Keys.KEY_ID
import live.talkshop.sdk.resources.Keys.KEY_CREATED_AT
import live.talkshop.sdk.resources.Keys.KEY_IS_VERIFIED
import live.talkshop.sdk.resources.Keys.KEY_NAME
import live.talkshop.sdk.resources.Keys.KEY_PLATFORM
import live.talkshop.sdk.resources.Keys.KEY_PROFILE_URL
import live.talkshop.sdk.resources.Keys.KEY_SENDER
import live.talkshop.sdk.resources.Keys.KEY_TEXT
import live.talkshop.sdk.resources.Keys.KEY_TYPE

object MessageParser {
    /**
     * Parses the payload of a [PNMessageResult] to a [MessageModel].
     *
     * @param message The [JsonObject] containing the message payload.
     * @return The parsed [MessageModel] or null if the parsing fails.
     */
    fun parse(message: JsonObject): MessageModel? {
        return try {
            val id = message.get(KEY_ID)?.asInt
            val createdAt = message.get(KEY_CREATED_AT)?.asString
            val sender = parseSender(message.get(KEY_SENDER))
            val text = message.get(KEY_TEXT)?.asString
            val typeString = message.get(KEY_TYPE)?.asString ?: ENUM_MESSAGE_TYPE_COMMENT
            val type = MessageModel.MessageType.fromString(typeString)
            val platform = message.get(KEY_PLATFORM)?.asString
            MessageModel(id, createdAt, MessageSender.Model(sender), text, type, platform)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parses the sender field which can be a JSON object or a string.
     *
     * @param senderJson The [JsonElement] containing the sender information.
     * @return The parsed sender as a [SenderModel].
     */
    private fun parseSender(senderJson: JsonElement?): SenderModel? {
        return when {
            senderJson == null -> null
            senderJson.isJsonObject -> {
                val senderObject = senderJson.asJsonObject
                SenderModel(
                    id = senderObject.get(KEY_ID)?.asString ?: return null,
                    name = senderObject.get(KEY_NAME).asString,
                    profileUrl = senderObject.get(KEY_PROFILE_URL)?.asString,
                    channelCode = senderObject.get(KEY_CHANNEL_CODE)?.asString,
                    isVerified = senderObject.get(KEY_IS_VERIFIED).asBoolean
                )
            }

            senderJson.isJsonPrimitive -> {
                SenderModel(name = senderJson.asString)
            }

            else -> null
        }
    }
}