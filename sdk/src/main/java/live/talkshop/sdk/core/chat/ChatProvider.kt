package live.talkshop.sdk.core.chat

import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.PubNubException
import com.pubnub.api.UserId
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import com.pubnub.api.models.consumer.pubsub.objects.PNDeleteChannelMetadataEventMessage
import com.pubnub.api.models.consumer.pubsub.objects.PNDeleteMembershipEventMessage
import com.pubnub.api.models.consumer.pubsub.objects.PNDeleteUUIDMetadataEventMessage
import com.pubnub.api.models.consumer.pubsub.objects.PNObjectEventResult
import com.pubnub.api.models.consumer.pubsub.objects.PNSetChannelMetadataEventMessage
import com.pubnub.api.models.consumer.pubsub.objects.PNSetMembershipEventMessage
import com.pubnub.api.models.consumer.pubsub.objects.PNSetUUIDMetadataEventMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.talkshop.sdk.core.authentication.globalShowKey
import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.authentication.storedClientKey
import live.talkshop.sdk.core.chat.models.MessageModel
import live.talkshop.sdk.core.chat.models.SenderModel
import live.talkshop.sdk.core.chat.models.UserTokenModel
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Constants.CHANNEL_CHAT_PREFIX
import live.talkshop.sdk.resources.Constants.CHANNEL_EVENTS_PREFIX
import live.talkshop.sdk.resources.Constants.PLATFORM_TYPE
import live.talkshop.sdk.resources.APIClientError
import live.talkshop.sdk.resources.APIClientError.AUTHENTICATION_FAILED
import live.talkshop.sdk.resources.APIClientError.CHANNEL_SUBSCRIPTION_FAILED
import live.talkshop.sdk.resources.APIClientError.CHAT_CONNECTION_ERROR
import live.talkshop.sdk.resources.APIClientError.INVALID_USER_TOKEN
import live.talkshop.sdk.resources.APIClientError.MESSAGE_LIST_FAILED
import live.talkshop.sdk.resources.APIClientError.MESSAGE_SENDING_FAILED
import live.talkshop.sdk.resources.APIClientError.PERMISSION_DENIED
import live.talkshop.sdk.resources.APIClientError.UNKNOWN_EXCEPTION
import live.talkshop.sdk.resources.APIClientError.USER_ALREADY_AUTHENTICATED
import live.talkshop.sdk.resources.APIClientError.USER_TOKEN_EXCEPTION
import live.talkshop.sdk.resources.Keys.KEY_ID
import live.talkshop.sdk.resources.Keys.KEY_NAME
import live.talkshop.sdk.resources.Keys.KEY_PROFILE_URL
import live.talkshop.sdk.resources.Keys.KEY_SENDER
import live.talkshop.sdk.utils.Collector
import live.talkshop.sdk.utils.Logging
import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.ApiResponse
import live.talkshop.sdk.utils.networking.HTTPMethod
import live.talkshop.sdk.utils.networking.URLs.getCurrentStreamUrl
import live.talkshop.sdk.utils.networking.URLs.getMessagesUrl
import live.talkshop.sdk.utils.networking.URLs.getUserMetaUrl
import live.talkshop.sdk.utils.networking.URLs.getUserTokenUrl
import live.talkshop.sdk.utils.parsers.MessageParser
import live.talkshop.sdk.utils.parsers.ShowStatusParser
import live.talkshop.sdk.utils.parsers.UserTokenParser
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
    private var triedToReconnectBefore = false
    private var pubnub: PubNub? = null
    private lateinit var publishChannel: String
    private var eventsChannel: String? = null
    private var callback: ChatProviderCallback? = null
    private lateinit var currentShowKey: String
    private lateinit var eventId: String
    private lateinit var userId: String
    private lateinit var currentJwt: String
    private var fromUpdateUser: Boolean = false
    private val userMetadataCache = mutableMapOf<String, SenderModel>()
    private var isSubscribed = false

    /**
     * Sets the callback for handling chat events and messages.
     *
     * @param callback The callback to be invoked on chat events.
     */
    internal suspend fun setCallback(callback: ChatProviderCallback) {
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
            var response: ApiResponse? = null
            try {
                currentShowKey = showKey
                globalShowKey = showKey
                val url = getUserTokenUrl(isGuest)
                val headers = mutableMapOf(
                    Constants.SDK_KEY to storedClientKey,
                    Constants.AUTH_KEY to "${Constants.BEARER_KEY} $jwt"
                )
                response = APIHandler.makeRequest(url, HTTPMethod.POST, headers = headers)
                userTokenModel = UserTokenParser.fromJsonString(response.body)!!
                initializePubNub()
                callback?.invoke(null, userTokenModel)
                currentJwt = jwt
            } catch (e: Exception) {
                if (response != null) {
                    when (response.statusCode) {
                        403 -> {
                            Logging.print(PERMISSION_DENIED)
                            callback?.invoke(PERMISSION_DENIED, null)
                        }

                        !in 200..299 -> {
                            Logging.print(INVALID_USER_TOKEN)
                            callback?.invoke(INVALID_USER_TOKEN, null)
                        }

                        else -> {
                            Logging.print(USER_TOKEN_EXCEPTION)
                            callback?.invoke(USER_TOKEN_EXCEPTION, null)
                        }
                    }
                } else {
                    Logging.print(CHAT_CONNECTION_ERROR)
                    callback?.invoke(CHAT_CONNECTION_ERROR, null)
                }
            }
        } else {
            callback?.invoke(AUTHENTICATION_FAILED, null)
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
        val response = APIHandler.makeRequest(
            getCurrentStreamUrl(currentShowKey),
            HTTPMethod.GET
        )

        if (response.statusCode !in 200..299) {
            Logging.print(CHANNEL_SUBSCRIPTION_FAILED)
        } else {
            Logging.print("Channels subscribe success")
        }

        val showStatusModel = ShowStatusParser.parseFromJson(JSONObject(response.body))
        publishChannel = CHANNEL_CHAT_PREFIX + showStatusModel.eventId
        eventsChannel = CHANNEL_EVENTS_PREFIX + showStatusModel.eventId
        channels = listOfNotNull(publishChannel, eventsChannel)
        eventId = publishChannel
        subscribe()

        val action: String
        if (fromUpdateUser) {
            action = Constants.COLLECTOR_ACTION_UPDATE_USER
        } else {
            action = Constants.COLLECTOR_ACTION_SELECT_VIEW_CHAT
            fromUpdateUser = true
        }
        Collector.collect(
            action = action,
            category = Constants.COLLECTOR_CAT_INTERACTION,
            eventID = showStatusModel.eventId,
            showKey = currentShowKey,
            showStatus = showStatusModel.status,
            userId = userId
        )
    }

    /**
     * Subscribes to the channels and sets up listeners for handling chat events.
     */
    internal suspend fun subscribe() {
        if (!isAuthenticated) {
            println(AUTHENTICATION_FAILED)
            return
        }
        handleShowKeyChange()
        if (!isSubscribed) {
            val listener = object : SubscribeCallback() {
                override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                    when (pnMessageResult.channel) {
                        publishChannel -> {
                            val messageData: MessageModel? =
                                MessageParser.parse(JSONObject(pnMessageResult.message.asJsonObject.toString()))
                            if (messageData != null) {
                                val uuid = messageData.sender?.id
                                if (uuid != null && userMetadataCache.containsKey(uuid)) {
                                    // Update the sender information with cached metadata
                                    messageData.sender = userMetadataCache[uuid]
                                    callback?.onMessageReceived(messageData)
                                } else if (uuid != null) { // Metadata not in cache, fetch asynchronously and update
                                    CoroutineScope(Dispatchers.IO).launch {
                                        fetchUserMetaData(uuid)
                                        messageData.sender = userMetadataCache[uuid]
                                        // Post back on the main thread or the appropriate thread for UI updates
                                        withContext(Dispatchers.Main) {
                                            callback?.onMessageReceived(messageData)
                                        }
                                    }
                                } else {
                                    // UUID is null or invalid, process the message normally
                                    callback?.onMessageReceived(messageData)
                                }
                            } else {
                                println("messageData is null")
                            }
                        }

                        eventsChannel -> {
                            println("Received message on events channel: ${pnMessageResult.message}")
                            if (pnMessageResult.message.asJsonObject.get("key").asString == "message_deleted") {
                                val messageId =
                                    pnMessageResult.message.asJsonObject.get("payload").asLong
                                callback?.onMessageDeleted(messageId)
                            }
                        }

                        else -> {
                            println("Received message on other channel: ${pnMessageResult.message}")
                        }
                    }
                }

                override fun status(pubnub: PubNub, pnStatus: PNStatus) {
                    println("PubNub status: ${pnStatus.category}, error code: ${pnStatus.statusCode}")
                    if (pnStatus.category == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                        if (!triedToReconnectBefore) {
                            triedToReconnectBefore = true
                            pubnub.reconnect()
                        }
                    } else if (pnStatus.category == PNStatusCategory.PNConnectionError && triedToReconnectBefore) {
                        callback?.onStatusChange(CHAT_CONNECTION_ERROR)
                    } else if (pnStatus.category == PNStatusCategory.PNConnectedCategory || pnStatus.category == PNStatusCategory.PNReconnectedCategory) {
                        triedToReconnectBefore = false
                    }

                    if (pnStatus.error) {
                        if (pnStatus.statusCode == 403) {
                            isSubscribed = false
                        }
                    }
                }

                override fun presence(
                    pubnub: PubNub,
                    pnPresenceEventResult: PNPresenceEventResult
                ) {
                    println("Presence event: ${pnPresenceEventResult.event} on channel: ${pnPresenceEventResult.channel}")
                }

                override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {
                    println("Signal received on channel: ${pnSignalResult.channel} with content: ${pnSignalResult.message}")
                }

                override fun messageAction(
                    pubnub: PubNub,
                    pnMessageActionResult: PNMessageActionResult
                ) {
                    println("Message action type: ${pnMessageActionResult.messageAction.type} on message: ${pnMessageActionResult.messageAction.value}")
                }

                override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {
                    println("File event received on channel: ${pnFileEventResult.channel}, file name: ${pnFileEventResult.file.name}")
                }

                override fun objects(pubnub: PubNub, objectEvent: PNObjectEventResult) {
                    when (objectEvent.extractedMessage) {
                        is PNSetChannelMetadataEventMessage -> {
                            val message =
                                objectEvent.extractedMessage as PNSetChannelMetadataEventMessage
                            println("Channel metadata set event received, channel: ${message.data.name}")
                        }

                        is PNSetUUIDMetadataEventMessage -> {
                            val message =
                                objectEvent.extractedMessage as PNSetUUIDMetadataEventMessage
                            println("UUID metadata set event received, UUID: ${message.data.id}")
                        }

                        is PNSetMembershipEventMessage -> {
                            val message =
                                objectEvent.extractedMessage as PNSetMembershipEventMessage
                            println("Membership set event received, channel: ${message.data.channel}, UUID: ${message.data.uuid}")
                        }

                        is PNDeleteChannelMetadataEventMessage -> {
                            val message =
                                objectEvent.extractedMessage as PNDeleteChannelMetadataEventMessage
                            println("Channel metadata delete event received, channel: ${message.channel}")
                        }

                        is PNDeleteUUIDMetadataEventMessage -> {
                            val message =
                                objectEvent.extractedMessage as PNDeleteUUIDMetadataEventMessage
                            println("UUID metadata delete event received, UUID: ${message.uuid}")
                        }

                        is PNDeleteMembershipEventMessage -> {
                            val message =
                                objectEvent.extractedMessage as PNDeleteMembershipEventMessage
                            println("Membership delete event received, channel: ${message.data.channelId}, UUID: ${message.data.uuid}")
                        }

                        else -> println("Other object event received")
                    }
                }
            }
            pubnub?.addListener(listener)
            pubnub!!.subscribe(channels, withPresence = true)
            isSubscribed = true
        }
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
            callback?.invoke(AUTHENTICATION_FAILED, null)
            return
        }
        try {
            handleShowKeyChange()
            if (message.length > 200) {
                callback?.invoke(
                    APIClientError.MESSAGE_ERROR_MESSAGE_MAX_LENGTH,
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
                platform = PLATFORM_TYPE
            )

            pubnub?.publish(publishChannel, messageObject)?.async { result, status ->
                if (!status.error) {
                    callback?.invoke(null, result!!.timetoken.toString())
                } else {
                    Logging.print(MESSAGE_SENDING_FAILED)
                    callback?.invoke(MESSAGE_SENDING_FAILED, null)
                }
            }
        } catch (error: Exception) {
            if ((error as? PubNubException)?.statusCode == 403) {
                Logging.print(PERMISSION_DENIED)
                callback?.invoke(PERMISSION_DENIED, null)
            } else {
                Logging.print(UNKNOWN_EXCEPTION, error)
                callback?.invoke(UNKNOWN_EXCEPTION, null)
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
            callback(null, null, AUTHENTICATION_FAILED)
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
                    val messages = result.messages.mapNotNull { messageDetail ->
                        if (messageDetail.entry.isJsonObject) {
                            MessageParser.parse(
                                JSONObject(messageDetail.entry.asJsonObject.toString()),
                                messageDetail.timetoken
                            )?.apply { // Update sender metadata if available in the cache
                                this.sender?.id?.let { uuid ->
                                    if (userMetadataCache.containsKey(uuid)) {
                                        this.sender = userMetadataCache[uuid]
                                    } else { // Asynchronously fetch metadata if not in cache
                                        CoroutineScope(Dispatchers.IO).launch {
                                            fetchUserMetaData(uuid)
                                        }
                                    }
                                }
                            }
                        } else {
                            null
                        }
                    }

                    // Callback should be executed after all the metadata updates are initiated
                    val nextStart = result.messages.lastOrNull()?.timetoken
                    callback(messages, nextStart, null)
                } else {
                    status.exception?.message?.let { Logging.print(MESSAGE_LIST_FAILED) }
                    callback(null, null, MESSAGE_LIST_FAILED)
                }
            }
        } catch (error: Exception) {
            if ((error as? PubNubException)?.statusCode == 403) {
                Logging.print(PERMISSION_DENIED)
                callback.invoke(null, null, PERMISSION_DENIED)
            } else {
                Logging.print(UNKNOWN_EXCEPTION)
                callback(null, null, UNKNOWN_EXCEPTION)
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
            Logging.print(USER_ALREADY_AUTHENTICATED)
            callback?.invoke(USER_ALREADY_AUTHENTICATED, null)
        }
    }

    /**
     * Cleans up the current PubNub instance and prepares for re-initialization or shutdown.
     */
    internal fun clearConnection() {
        pubnub?.unsubscribeAll()
        pubnub?.destroy()
        pubnub = null
        isSubscribed = false
    }

    /**
     * Checks if the current show key has changed and re-initializes the PubNub instance if necessary.
     */
    private suspend fun handleShowKeyChange() {
        if (currentShowKey != globalShowKey) {
            currentShowKey = globalShowKey
            clearConnection()
            initializePubNub()
            isSubscribed = false
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
                        Logging.print(PERMISSION_DENIED)
                    } else {
                        Logging.print(UNKNOWN_EXCEPTION, error)
                    }
                }
                callback(null)
            }
        }
    }

    internal suspend fun unPublishMessage(
        timeToken: String,
        callback: ((Boolean, String?) -> Unit)?
    ) {

        val headers = mutableMapOf(
            Constants.SDK_KEY to storedClientKey,
            Constants.AUTH_KEY to "${Constants.BEARER_KEY} $currentJwt"
        )

        try {
            val response = APIHandler.makeRequest(
                requestUrl = getMessagesUrl(eventId, timeToken),
                requestMethod = HTTPMethod.DELETE,
                headers = headers
            )

            when (response.statusCode) {
                403 -> {
                    Logging.print(PERMISSION_DENIED)
                    callback?.invoke(false, PERMISSION_DENIED.toString())
                }

                in 200..299 -> {
                    callback?.let { it(true, null) }
                }

                else -> {
                    callback?.let { it(false, response.body) }
                    println(response.body)
                    Logging.print(response.body)
                }
            }
        } catch (e: Exception) {
            Logging.print(UNKNOWN_EXCEPTION, e)
            callback?.let { it(false, UNKNOWN_EXCEPTION.toString()) }
        }
    }

    private suspend fun fetchUserMetaData(uuid: String) {
        try {
            val response = APIHandler.makeRequest(getUserMetaUrl(uuid), HTTPMethod.GET)
            if (response.statusCode in 200..299) {
                val jsonObject = JSONObject(response.body)
                val sender = jsonObject.getJSONObject(KEY_SENDER)
                val senderModel = SenderModel(
                    id = sender.getString(KEY_ID),
                    name = sender.getString(KEY_NAME),
                    profileUrl = sender.getString(KEY_PROFILE_URL)
                )
                userMetadataCache[uuid] = senderModel
            } else {
                Logging.print("$UNKNOWN_EXCEPTION: ${response.body}")
            }
        } catch (e: Exception) {
            Logging.print(UNKNOWN_EXCEPTION, e)
        }
    }
}