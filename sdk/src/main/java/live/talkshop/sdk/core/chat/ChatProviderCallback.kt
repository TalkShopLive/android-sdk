package live.talkshop.sdk.core.chat

import live.talkshop.sdk.core.chat.models.MessageModel

/**
 * Interface defining callbacks for chat-related events in the ChatProvider.
 * Implement this interface to handle various events emitted by the ChatProvider.
 * Add more callback methods for different events if needed.
 */
internal interface ChatProviderCallback {
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
     * Called when a  message is deleted.
     *
     * Implement this method to handle deleted messages. This method provides
     * a [messageId], which contains the id of the deleted message.
     *
     * @param messageId The [String] instance containing the message details.
     */
    fun onMessageDeleted(messageId: String)
}