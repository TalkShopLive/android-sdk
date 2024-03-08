package live.talkshop.sdk.core.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.user.models.UserTokenModel

/**
 * Represents a chat session, encapsulating the logic to initiate and manage chat functionalities.
 *
 * @property showId The unique identifier of the show or chat session.
 * @property jwt The JSON Web Token used for authentication.
 * @property isGuest A boolean indicating whether the user is a guest.
 */
class Chat(private val showId: String, private val jwt: String, private val isGuest: Boolean) {
    /**
     * Secondary constructor that initiates a chat session using the provided parameters.
     *
     * @param showId The unique identifier of the show or chat session.
     * @param jwt The JSON Web Token used for authentication.
     * @param isGuest A boolean indicating whether the user is a guest.
     * @param callback A callback function to be invoked with the result of the chat initiation.
     */
    constructor(
        showId: String,
        jwt: String,
        isGuest: Boolean,
        callback: ((String?, UserTokenModel?) -> Unit)?
    ) : this(showId, jwt, isGuest) {
        CoroutineScope(Dispatchers.IO).launch {
            provider.initiateChat(showId, jwt, isGuest, callback)
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
        fun subscribe() {
            provider.subscribe()
        }
    }
}