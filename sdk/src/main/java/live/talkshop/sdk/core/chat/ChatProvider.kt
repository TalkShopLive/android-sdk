package live.talkshop.sdk.core.chat

import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.PubNubException
import com.pubnub.api.UserId
import com.pubnub.api.models.consumer.message_actions.PNMessageAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.talkshop.sdk.core.authentication.globalShowId
import live.talkshop.sdk.core.authentication.globalShowKey
import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.chat.models.MessageModel
import live.talkshop.sdk.core.chat.models.SenderModel
import live.talkshop.sdk.core.chat.models.UserTokenModel
import live.talkshop.sdk.resources.APIClientError
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.utils.Collector
import live.talkshop.sdk.utils.Logging
import live.talkshop.sdk.utils.helpers.HelperFunctions.isNotEmptyOrNull
import live.talkshop.sdk.utils.networking.APICalls
import live.talkshop.sdk.utils.parsers.MessageParser
import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * The ChatProvider class is responsible for handling chat functionalities,
 * including initiating the chat by obtaining a user token, subscribing to
 * channels, and publishing messages.
 *
 * @property userTokenModel Contains user-specific information such as the
 * user ID and authentication token required for interacting with the chat service.
 * @property channels A list of channels that the user is subscribed to.
 * @property pubnub The PubNub instance used for real-time communication.
 * @property publishChannel The primary channel for publishing messages.
 * @property eventsChannel The channel used for subscribing to events.
 * @property callback An optional callback for handling received messages.
 * @property currentShowKey The current show's key.
 * @property eventId The id of the current event..
 */
class ChatProvider {
    private lateinit var userTokenModel: UserTokenModel
    private lateinit var channels: List<String>
    private var pubnub: PubNub? = null
    private lateinit var publishChannel: String
    private var eventsChannel: String? = null
    private var callback: ChatCallback? = null
    private lateinit var currentShowKey: String
    private lateinit var eventId: String
    private lateinit var userId: String
    private lateinit var currentJwt: String
    private var fromUpdateUser: Boolean = false
    private val userMetadataCache = mutableMapOf<String, SenderModel>()

    /**
     * Sets the callback for handling chat events and messages.
     *
     * @param callback The callback to be invoked on chat events.
     */
    internal suspend fun setCallback(callback: ChatCallback) {
        handleShowKeyChange()
        this.callback = callback
    }

    /**
     * Initiates the chat by fetching a user token and setting up PubNub.
     *
     * @param showKey The unique identifier for the chat session or show.
     * @param jwt The JWT token used for authentication.
     * @param isGuest Indicates whether the user is a guest.
     * @param callback A callback invoked upon the completion of the chat initiation process.
     */
    internal suspend fun initiateChat(
        showKey: String,
        jwt: String,
        isGuest: Boolean,
        callback: ((APIClientError?, UserTokenModel?) -> Unit)?
    ) {
        if (isAuthenticated) {
            if (!isNotEmptyOrNull(globalShowId)) {
                callback?.invoke(getError(APIClientError.SHOW_NOT_LIVE), null)
                return
            }

            currentShowKey = showKey
            globalShowKey = showKey

            APICalls.getUserToken(jwt, isGuest).onError {
                callback?.invoke(it, null)
            }.onResult {
                userTokenModel = it
                initializePubNub()
                callback?.invoke(null, userTokenModel)
                currentJwt = jwt
            }
        } else {
            callback?.invoke(getError(APIClientError.AUTHENTICATION_FAILED), null)
        }
    }

    /**
     * Initializes the PubNub configuration and subscribes to the necessary channels.
     */
    private suspend fun initializePubNub() {
        val pnConfig = PNConfiguration(UserId(userTokenModel.userId)).apply {
            subscribeKey = userTokenModel.subscribeKey
            publishKey = userTokenModel.publishKey
            authKey = userTokenModel.token
            secure = true
        }
        userId = userTokenModel.userId
        pubnub = PubNub(pnConfig)

        subscribeChannels()
    }

    /**
     * Subscribes to the chat and events channels.
     */
    private suspend fun subscribeChannels() {
        APICalls.getCurrentStream(currentShowKey).onResult {
            publishChannel = Constants.CHANNEL_CHAT_PREFIX + it.eventId
            eventsChannel = Constants.CHANNEL_EVENTS_PREFIX + it.eventId
            channels = listOfNotNull(publishChannel, eventsChannel)
            eventId = publishChannel

            val action: String
            val category: String
            if (fromUpdateUser) {
                action = Constants.COLLECTOR_ACTION_UPDATE_USER
                category = Constants.COLLECTOR_CAT_PROCESS
            } else {
                action = Constants.COLLECTOR_ACTION_SELECT_VIEW_CHAT
                category = Constants.COLLECTOR_CAT_INTERACTION
                fromUpdateUser = true
            }
            Collector.collect(
                action = action,
                category = category,
                eventID = it.eventId,
                showKey = currentShowKey,
                showStatus = it.status,
                userId = userId
            )
        }
    }

    /**
     * Subscribes to the channels and sets up listeners for handling chat events.
     */
    internal suspend fun subscribe() {
        if (!isAuthenticated) {
            println(APIClientError.AUTHENTICATION_FAILED)
            return
        }
        handleShowKeyChange()
        val pubNubListeners = PubNubListeners(
            callback,
            userMetadataCache,
            publishChannel,
            eventsChannel
        )
        pubnub?.addListener(pubNubListeners.TSLSubscribeCallback())
        pubnub!!.subscribe(channels, withPresence = true)
    }

    /**
     * Publishes a message to the chat channel.
     *
     * @param message The message to be published.
     * @param callback An optional callback invoked with the result of the publish operation.
     */
    internal suspend fun publish(
        message: String,
        callback: ((APIClientError?, String?) -> Unit)? = null
    ) {
        if (!isAuthenticated) {
            callback?.invoke(getError(APIClientError.AUTHENTICATION_FAILED), null)
            return
        }
        try {
            handleShowKeyChange()
            if (message.length > 200) {
                callback?.invoke(
                    getError(APIClientError.MESSAGE_ERROR_MESSAGE_MAX_LENGTH),
                    null
                )
                return
            }

            val messageType = when {
                message.trim().contains("?") -> Constants.MESSAGE_TYPE_QUESTION
                else -> Constants.MESSAGE_TYPE_COMMENT
            }


            val messageObject = MessageModel(
                id = System.currentTimeMillis(),
                createdAt = Date().toString(),
                sender = SenderModel(id = userTokenModel.userId, name = userTokenModel.name),
                text = message,
                type = messageType,
                platform = Constants.PLATFORM_TYPE
            )

            pubnub?.publish(publishChannel, messageObject)?.async { result, status ->
                if (!status.error) {
                    callback?.invoke(null, result!!.timetoken.toString())
                } else {
                    callback?.invoke(getError(APIClientError.MESSAGE_SENDING_FAILED), null)
                }
            }
        } catch (error: Exception) {
            if ((error as? PubNubException)?.statusCode == 403) {
                if (error.message?.contains("expired") == true) {
                    callback?.invoke(
                        getError(APIClientError.CHAT_TOKEN_EXPIRED),
                        null
                    )
                } else {
                    callback?.invoke(getError(APIClientError.PERMISSION_DENIED), null)
                }
            } else {
                log(APIClientError.UNKNOWN_EXCEPTION, error)
                callback?.invoke(getError(APIClientError.UNKNOWN_EXCEPTION), null)
            }
        }
    }

    /**
     * Fetches past chat messages.
     *
     * @param count The number of messages to fetch. Defaults to 25.
     * @param start The starting point in time for fetching messages. If null, fetches the latest messages.
     * @param includeMeta Whether to include message metadata.
     * @param callback Callback to return the result or error.
     */
    internal suspend fun fetchPastMessages(
        count: Int = 25,
        start: Long? = System.currentTimeMillis(),
        includeMeta: Boolean = true,
        callback: (List<MessageModel>?, Long?, APIClientError?) -> Unit
    ) {
        if (!isAuthenticated) {
            callback(null, null, getError(APIClientError.AUTHENTICATION_FAILED))
            return
        }

        try {
            handleShowKeyChange()
            pubnub?.history(
                channel = publishChannel,
                start = start,
                count = count,
                includeTimetoken = true,
                includeMeta = includeMeta
            )?.async { result, status ->
                if (!status.error && result != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val deferredMetadataUpdates = mutableListOf<Deferred<Unit>>()
                        val messages = result.messages.mapNotNull { messageDetail ->
                            if (messageDetail.entry.isJsonObject) {
                                MessageParser.parse(
                                    JSONObject(messageDetail.entry.asJsonObject.toString()),
                                    messageDetail.timetoken
                                )?.apply {
                                    this.sender?.id?.let { uuid ->
                                        if (userMetadataCache.containsKey(uuid)) {
                                            this.sender = userMetadataCache[uuid]
                                        } else {
                                            deferredMetadataUpdates.add(async {
                                                APICalls.getUserMeta(uuid).onResult {
                                                    userMetadataCache[uuid] = it
                                                }
                                                sender = userMetadataCache[uuid]
                                            })
                                        }
                                    }
                                }
                            } else {
                                null
                            }
                        }

                        deferredMetadataUpdates.awaitAll()
                        withContext(Dispatchers.Main) {
                            val nextStart = result.messages.firstOrNull()
                            if (nextStart != null) {
                                callback(messages, nextStart.timetoken, null)
                            } else {
                                callback(messages, null, null)
                            }
                        }
                    }
                } else {
                    callback(null, null, getError(APIClientError.MESSAGE_LIST_FAILED))
                }
            }
        } catch (error: Exception) {
            when (error) {
                is PubNubException -> {
                    if (error.statusCode == 403) {
                        callback.invoke(null, null, getError(APIClientError.PERMISSION_DENIED))
                    } else {
                        callback(null, null, getError(APIClientError.UNKNOWN_EXCEPTION))
                    }
                }

                else -> {
                    callback(null, null, getError(APIClientError.UNKNOWN_EXCEPTION))
                }
            }
        }
    }

    /**
     * Attempts to update the user token model and re-initiate chat if the JWT has changed.
     *
     * @param newJwt The new JWT to be set for the user.
     * @param isGuest A flag indicating if the current user is a guest.
     * @param callback A callback to be invoked after the update attempt with any resulting message
     * and the updated UserTokenModel.
     */
    internal suspend fun editUser(
        newJwt: String,
        isGuest: Boolean,
        callback: ((APIClientError?, UserTokenModel?) -> Unit)?
    ) {
        if (userTokenModel.token != newJwt) {
            clearConnection()
            initiateChat(currentShowKey, newJwt, isGuest, callback)
        } else {
            callback?.invoke(getError(APIClientError.USER_ALREADY_AUTHENTICATED), null)
        }
    }

    /**
     * Cleans up the current PubNub instance and prepares for re-initialization or shutdown.
     */
    internal fun clearConnection() {
        pubnub?.unsubscribeAll()
        pubnub?.destroy()
        pubnub = null
        callback = null
    }

    /**
     * Checks if the current show key has changed and re-initializes the PubNub instance if necessary.
     */
    private suspend fun handleShowKeyChange() {
        if (currentShowKey != globalShowKey) {
            currentShowKey = globalShowKey
            clearConnection()
            initializePubNub()
            callback = null
        }
    }

    /**
     * Counts the unread messages in the provided channels.
     *
     * @param callback Callback to return the count of unread messages based on the
     * name of the channel
     */
    internal fun countUnreadMessages(callback: (Map<String, Long>?) -> Unit) {
        val lastHourTimeToken =
            (Calendar.getInstance().timeInMillis - TimeUnit.HOURS.toMillis(1)) * 10000L
        pubnub?.messageCounts(channels, listOf(lastHourTimeToken))?.async { result, status ->
            if (!status.error && result != null) {
                callback(result.channels)
            } else {
                val error = status.exception
                error?.statusCode?.let {
                    if (it == 403) {
                        log(APIClientError.PERMISSION_DENIED)
                    } else {
                        log(APIClientError.UNKNOWN_EXCEPTION, error)
                    }
                }
                callback(null)
            }
        }
    }

    /**
     * Unpublishes a message with the given time token.
     *
     * @param timeToken The time token of the message to unpublish.
     * @param callback  Callback with the result: true if successful, false with an error message otherwise.
     */
    internal suspend fun unPublishMessage(
        timeToken: String,
        callback: ((Boolean, String?) -> Unit)?
    ) {
        APICalls.deleteMessage(eventId, timeToken, currentJwt).onError {
            callback?.let { it(false, it.toString()) }
        }.onResult {
            callback?.let { it(true, null) }
        }
    }

    internal fun likeComment(
        timeToken: Long,
        callback: ((Boolean, APIClientError?) -> Unit)? = null
    ) {
        pubnub?.let {
            it.addMessageAction(
                publishChannel, PNMessageAction("reaction", "like", timeToken)
            ).async { _, status ->
                if (!status.error) {
                    callback?.let { it(false, getError(APIClientError.LIKE_COMMENT_FAILED)) }
                } else {
                    callback?.let { it(true, null) }
                }
            }
        }
    }

    internal suspend fun unlikeComment(
        timeToken: Long,
        actionTimeToken: Long,
        callback: ((Boolean, APIClientError?) -> Unit)? = null
    ) {
        APICalls.deleteAction(eventId, timeToken.toString(), actionTimeToken.toString(), currentJwt)
            .onError { error ->
                callback?.let { it(false, getError(error)) }
            }.onResult {
                callback?.let { it(true, null) }
            }
    }

    private fun getError(error: APIClientError): APIClientError {
        return error.from(ChatProvider::class.java.name)
    }

    private fun log(code: APIClientError? = null, error: Exception? = null) {
        if (code != null && error != null)
            Logging.print(ChatProvider::class.java, code, error)
        else if (code != null)
            Logging.print(ChatProvider::class.java, code)
    }
}