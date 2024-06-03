package live.talkshop.sdk.core.chat

import live.talkshop.sdk.core.chat.models.MessageModel
import live.talkshop.sdk.resources.APIClientError

/**
 * Interface defining callbacks for chat-related events in the ChatProvider.
 * Implement this interface to handle various events emitted by the ChatProvider.
 * Add more callback methods for different events if needed.
 */
interface ChatCallback {
    /**
     * Called when a new message is received on the publish channel.
     *
     * Implement this method to handle incoming messages. This method provides
     * a [MessageModel] instance, which contains details about the received message,
     * including its content, sender, and other metadata.
     *
     * @param message The [MessageModel] instance containing the message details.
     */
    fun onMessageReceived(message: MessageModel)

    /**
     * Called when a message is deleted.
     *
     * Implement this method to handle deleted messages. This method provides
     * a [messageId], which contains the id of the deleted message.
     *
     * @param messageId The [Int] instance containing the message details.
     */
    fun onMessageDeleted(messageId: Long)

    /**
     * Called when an error occurs.
     *
     * Implement this method to handle error handling. This method provides
     * a [error], which contains the error string.
     *
     * @param error The [APIClientError] instance containing the error details.
     */
    fun onStatusChange(error: APIClientError)

    /**
     * Called when a comment is liked.
     *
     * @param messageId The Long instance containing the message ID.
     */
    fun onLikeComment(messageId: Long)

    /**
     * Called when a comment is unliked.
     *
     * @param messageId The Long instance containing the message ID.
     */
    fun onUnlikeComment(messageId: Long)
}