package live.talkshop.sdk.utils.parsers

import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import live.talkshop.sdk.core.chat.models.MessageModel
import live.talkshop.sdk.resources.Constants.ENUM_MESSAGE_TYPE_COMMENT
import live.talkshop.sdk.resources.Keys.KEY_ID
import live.talkshop.sdk.resources.Keys.KEY_CREATED_AT
import live.talkshop.sdk.resources.Keys.KEY_PLATFORM
import live.talkshop.sdk.resources.Keys.KEY_SENDER
import live.talkshop.sdk.resources.Keys.KEY_TEXT
import live.talkshop.sdk.resources.Keys.KEY_TYPE

object MessageParser {
    /**
     * Parses the payload of a [PNMessageResult] to a [MessageModel].
     *
     * @param message The [PNMessageResult] containing the message payload.
     * @return The parsed [MessageModel] or null if the parsing fails.
     */
    fun parse(message: PNMessageResult): MessageModel? {
        return try {
            val payload = message.message.asJsonObject
            val id = payload.get(KEY_ID)?.asInt
            val createdAt = payload.get(KEY_CREATED_AT)?.asString
            val sender = payload.get(KEY_SENDER)?.asString
            val text = payload.get(KEY_TEXT)?.asString
            val typeString = payload.get(KEY_TYPE)?.asString ?: ENUM_MESSAGE_TYPE_COMMENT
            val type = MessageModel.MessageType.fromString(typeString)
            val platform = payload.get(KEY_PLATFORM)?.asString
            MessageModel(id, createdAt, sender, text, type, platform)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}