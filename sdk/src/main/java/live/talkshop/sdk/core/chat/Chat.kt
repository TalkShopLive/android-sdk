package live.talkshop.sdk.core.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.chat.models.MessageModel
import live.talkshop.sdk.core.chat.models.UserTokenModel
import live.talkshop.sdk.resources.APIClientError

/**
 * Represents a chat session, encapsulating the logic to initiate and manage chat functionalities.
 *
 * @property showKey The unique identifier of the show or chat session.
 * @property jwt The JSON Web Token used for authentication.
 * @property isGuest A boolean indicating whether the user is a guest.
 */
class Chat(private val showKey: String, private val jwt: String, private val isGuest: Boolean) {
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
        callback: ((APIClientError?, UserTokenModel?) -> Unit)?
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
        suspend fun publish(
            message: String,
            callback: ((APIClientError?, String?) -> Unit)? = null
        ) {
            provider.publish(message, callback)
        }

        /**
         * Subscribes to the chat channels to receive messages and other events.
         */
        suspend fun subscribe(callback: ChatCallback) {
            provider.setCallback(object : ChatCallback {
                override fun onMessageReceived(message: MessageModel) {
                    callback.onMessageReceived(message)
                }

                override fun onMessageDeleted(messageId: Long) {
                    callback.onMessageDeleted(messageId)
                }

                override fun onStatusChange(error: APIClientError) {
                    callback.onStatusChange(error)
                }

                override fun onLikeComment(messageId: Long) {
                    callback.onLikeComment(messageId)
                }

                override fun onUnlikeComment(messageId: Long) {
                    callback.onUnlikeComment(messageId)
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
        suspend fun getChatMessages(
            count: Int = 25,
            start: Long? = null,
            callback: (List<MessageModel>?, Long?, APIClientError?) -> Unit
        ) {
            provider.fetchPastMessages(count = count, start = start, callback = callback)
        }

        /**
         * Updates the user's authentication state and re-initiates the chat session if necessary.
         *
         * @param newJwt The new JWT for the user authentication.
         * @param isGuest Indicates if the user is in guest mode.
         * @param callback A callback to be invoked with the operation result.
         */
        suspend fun updateUser(
            newJwt: String,
            isGuest: Boolean,
            callback: ((APIClientError?, UserTokenModel?) -> Unit)?
        ) {
            provider.editUser(newJwt, isGuest, callback)
        }

        /**
         * Unsubscribes from all the channels and chat connections
         */
        fun clean() {
            provider.clearConnection()
        }

        /**
         * Public method to count unread messages.
         *
         * @param callback Callback to return the count of unread messages.
         */
        fun countMessages(callback: (Map<String, Long>?) -> Unit) {
            provider.countUnreadMessages(callback)
        }

        /**
         * Public method to count unread messages.
         * @param timeToken The time token of the message.
         * @param callback optional callback to return success or error.
         */
        suspend fun deleteMessage(
            timeToken: String,
            callback: ((Boolean, String?) -> Unit)? = null
        ) {
            provider.unPublishMessage(timeToken, callback)
        }

        /**
         * Public method to like a comment.
         * @param timeToken The time token of the comment.
         * @param callback optional callback to return success or error.
         */
        suspend fun likeComment(
            timeToken: Long,
            callback: ((Boolean, APIClientError?) -> Unit)? = null
        ) {
            provider.likeComment(timeToken, callback)
        }

        /**
         * Public method to unlike a comment.
         * @param timeToken The time token of the comment.
         * @param actionTimeToken The time token of the like on the comment.
         * @param callback optional callback to return success or error.
         */
        suspend fun unlikeComment(
            timeToken: Long,
            actionTimeToken: Long,
            callback: ((Boolean, APIClientError?) -> Unit)? = null
        ) {
            provider.unlikeComment(timeToken, actionTimeToken, callback)
        }
    }
}