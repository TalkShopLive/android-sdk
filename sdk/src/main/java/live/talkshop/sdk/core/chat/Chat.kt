package live.talkshop.sdk.core.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.chat.models.MessageModel
import live.talkshop.sdk.core.user.models.UserTokenModel

/**
 * Represents a chat session, encapsulating the logic to initiate and manage chat functionalities.
 *
 * @property showKey The unique identifier of the show or chat session.
 * @property jwt The JSON Web Token used for authentication.
 * @property isGuest A boolean indicating whether the user is a guest.
 */
class Chat(private val showKey: String, private val jwt: String, private val isGuest: Boolean) {
    interface ChatCallback {
        fun onMessageReceived(message: MessageModel)
    }

    /**
     * Secondary constructor that initiates a chat session using the provided parameters.
     *
     * @param showKey The unique identifier of the show or chat session.
     * @param jwt The JSON Web Token used for authentication.
     * @param isGuest A boolean indicating whether the user is a guest.
     * @param callback A callback function to be invoked with the result of the chat initiation.
     */
    constructor(
        showKey: String,
        jwt: String,
        isGuest: Boolean,
        callback: ((String?, UserTokenModel?) -> Unit)?
    ) : this(showKey, jwt, isGuest) {
        CoroutineScope(Dispatchers.IO).launch {
            provider.initiateChat(showKey, jwt, isGuest, callback)
        }
    }

    companion object {
        private val provider = ChatProvider()

        /**
         * Publishes a message to the chat.
         *
         * @param message The message to be published.
         * @param callback An optional callback to be invoked with the result of the publish operation.
         */
        fun publish(message: String, callback: ((String?, String?) -> Unit)? = null) {
            provider.publish(message, callback)
        }

        /**
         * Subscribes to the chat channels to receive messages and other events.
         */
        fun subscribe(callback: ChatCallback) {
            provider.setCallback(object : ChatProviderCallback {
                override fun onMessageReceived(message: MessageModel) {
                    callback.onMessageReceived(message)
                }
            })
            provider.subscribe()
        }

        /**
         * Gets chat messages with pagination support.
         *
         * @param count The number of messages to fetch. Defaults to 25.
         * @param start The starting time token for fetching messages. Used for pagination.
         * @param callback Callback to return messages, the next start token, or an error.
         */
        fun getChatMessages(count: Int = 25, start: Long? = null, callback: (List<MessageModel>?, Long?, String?) -> Unit) {
            provider.fetchPastMessages(count = count, start = start, callback = callback)
        }
    }
}