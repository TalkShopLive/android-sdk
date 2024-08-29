package live.talkshop.sdk.core.chat.models

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
    val id: Long?,
    val createdAt: String?,
    var sender: SenderModel?,
    val text: String?,
    val type: String,
    val platform: String?,
    val timeToken: Long? = null,
    val original: MessageModel? = null,
    var aspectRatio: Long? = null
)