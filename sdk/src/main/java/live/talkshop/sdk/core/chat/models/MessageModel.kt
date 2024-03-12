package live.talkshop.sdk.core.chat.models

import live.talkshop.sdk.resources.Constants.ENUM_MESSAGE_TYPE_GIPHY
import live.talkshop.sdk.resources.Constants.ENUM_MESSAGE_TYPE_QUESTION

/**
 * Represents a message model in the chat system.
 *
 * @property id The unique identifier for the message, typically representing the creation date in milliseconds.
 * @property createdAt The timestamp when the message was created, formatted as a string.
 * @property sender The user ID of the message sender, obtained from the backend after creating a messaging token.
 * @property text The actual text content of the message.
 * @property type The type of the message, which helps in categorizing the message content.
 * @property platform An identifier for the platform from which the message was sent, e.g., "sdk".
 */
data class MessageModel(
    val id: Int?,
    val createdAt: String?,
    val sender: MessageSender?,
    val text: String?,
    val type: MessageType,
    val platform: String?
) {
    /**
     * Enum for identifying the type of the message.
     */
    enum class MessageType {
        COMMENT,
        QUESTION,
        GIPHY;

        companion object {
            /**
             * Converts a string value to its corresponding [MessageType] enum.
             *
             * @param type The string representation of the message type.
             * @return The corresponding [MessageType] enum.
             */
            fun fromString(type: String): MessageType = when (type.lowercase()) {
                ENUM_MESSAGE_TYPE_QUESTION -> QUESTION
                ENUM_MESSAGE_TYPE_GIPHY -> GIPHY
                else -> COMMENT
            }
        }
    }
}

/**
 * Sealed class to represent the sender of a message, which can be either a SenderModel or a String.
 */
sealed class MessageSender {
    data class Model(val senderModel: SenderModel?) : MessageSender()
    data class StringSender(val senderString: String) : MessageSender()
}