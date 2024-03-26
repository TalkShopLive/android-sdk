package live.talkshop.sdk.core.chat

import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.UserId
import com.pubnub.api.callbacks.SubscribeCallback
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
import live.talkshop.sdk.core.authentication.globalShowKey
import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.authentication.storedClientKey
import live.talkshop.sdk.core.chat.models.MessageModel
import live.talkshop.sdk.core.chat.models.SenderModel
import live.talkshop.sdk.core.chat.models.UserTokenModel
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Constants.CHANNEL_CHAT_PREFIX
import live.talkshop.sdk.resources.Constants.CHANNEL_EVENTS_PREFIX
import live.talkshop.sdk.resources.Constants.MESSAGE_ERROR_AUTH
import live.talkshop.sdk.resources.Constants.MESSAGE_ERROR_MESSAGE_MAX_LENGTH
import live.talkshop.sdk.resources.Constants.PLATFORM_TYPE
import live.talkshop.sdk.utils.Collector
import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.HTTPMethod
import live.talkshop.sdk.utils.networking.URLs.getCurrentStreamUrl
import live.talkshop.sdk.utils.networking.URLs.getMessagesUrl
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
    private var pubnub: PubNub? = null
    private lateinit var publishChannel: String
    private var eventsChannel: String? = null
    private var callback: ChatProviderCallback? = null
    private lateinit var currentShowKey: String
    private lateinit var eventId: String
    private lateinit var userId: String
    private lateinit var currentJwt: String
    private var fromUpdateUser: Boolean = false

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
        callback: ((String?, UserTokenModel?) -> Unit)?
    ) {
        if (isAuthenticated) {
            try {
                currentShowKey = showKey
                globalShowKey = showKey
                val url = getUserTokenUrl(isGuest)
                val headers = mutableMapOf(
                    Constants.SDK_KEY to storedClientKey,
                    Constants.AUTH_KEY to "${Constants.BEARER_KEY} $jwt"
                )
                val response = APIHandler.makeRequest(url, HTTPMethod.POST, headers = headers)
                userTokenModel = UserTokenParser.fromJsonString(response.body)!!
                initializePubNub()
                callback?.invoke(null, userTokenModel)
                currentJwt = jwt
            } catch (e: Exception) {
                e.printStackTrace()
                callback?.invoke(e.message, null)
            }
        } else {
            callback?.invoke(MESSAGE_ERROR_AUTH, null)
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
        val jsonResponse = APIHandler.makeRequest(
            getCurrentStreamUrl(currentShowKey),
            HTTPMethod.GET
        )
        val showStatusModel = ShowStatusParser.parseFromJson(JSONObject(jsonResponse.body))
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
            duration = showStatusModel.duration.toString(),
            userId = userId
        )
    }

    /**
     * Subscribes to the channels and sets up listeners for handling chat events.
     */
    internal suspend fun subscribe() {
        if (!isAuthenticated) {
            println(MESSAGE_ERROR_AUTH)
            return
        }
        handleShowKeyChange()
        val listener = object : SubscribeCallback() {
            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                when (pnMessageResult.channel) {
                    publishChannel -> {
                        val messageData: MessageModel? =
                            MessageParser.parse(pnMessageResult.message.asJsonObject)
                        if (messageData != null) {
                            println("Received message on publish channel: $messageData")
                            callback?.onMessageReceived(messageData)
                        } else {
                            println("messageData is null")
                        }
                    }

                    eventsChannel -> {
                        println("Received message on events channel: ${pnMessageResult.message}")
                    }

                    else -> {
                        println("Received message on other channel: ${pnMessageResult.message}")
                    }
                }
            }

            override fun status(pubnub: PubNub, pnStatus: PNStatus) {
                if (pnStatus.error) {
                    println("Error on PubNub status: ${pnStatus.category}")
                } else {
                    println("Status changed: ${pnStatus.category}")
                }
            }

            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
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
                        val message = objectEvent.extractedMessage as PNSetUUIDMetadataEventMessage
                        println("UUID metadata set event received, UUID: ${message.data.id}")
                    }

                    is PNSetMembershipEventMessage -> {
                        val message = objectEvent.extractedMessage as PNSetMembershipEventMessage
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
                        val message = objectEvent.extractedMessage as PNDeleteMembershipEventMessage
                        println("Membership delete event received, channel: ${message.data.channelId}, UUID: ${message.data.uuid}")
                    }

                    else -> println("Other object event received")
                }
            }
        }

        pubnub?.addListener(listener)
        pubnub!!.subscribe(channels, withPresence = true)
    }

    /**
     * Publishes a message to the chat channel.
     *
     * @param message The message to be published.
     * @param callback An optional callback invoked with the result of the publish operation.
     */
    internal suspend fun publish(message: String, callback: ((String?, String?) -> Unit)? = null) {
        if (!isAuthenticated) {
            callback?.invoke(MESSAGE_ERROR_AUTH, null)
            return
        }
        try {
            handleShowKeyChange()
            if (message.length > 200) {
                callback?.invoke(
                    MESSAGE_ERROR_MESSAGE_MAX_LENGTH,
                    null
                )
                return
            }

            val messageType = when {
                message.trim().contains("?") -> Constants.MESSAGE_TYPE_QUESTION
                else -> Constants.MESSAGE_TYPE_COMMENT
            }


            val messageObject = MessageModel(
                id = System.currentTimeMillis().toInt(),
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
                    Logging.print(status.exception?.message.toString())
                    callback?.invoke(status.exception?.message, null)
                }
            }
        } catch (error: Exception) {
            Logging.print(error)
            callback?.invoke(error.message, null)
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
        start: Long? = null,
        includeMeta: Boolean = true,
        callback: (List<MessageModel>?, Long?, String?) -> Unit
    ) {
        if (!isAuthenticated) {
            callback(null, null, MESSAGE_ERROR_AUTH)
            return
        }

        try {
            handleShowKeyChange()
            pubnub?.history(
                channel = publishChannel,
                start = start,
                count = count,
                includeMeta = includeMeta
            )?.async { result, status ->
                if (!status.error && result != null) {
                    val messages = result.messages.mapNotNull { messageDetail ->
                        if (messageDetail.entry.isJsonObject) {
                            MessageParser.parse(messageDetail.entry.asJsonObject)
                        } else {
                            null
                        }
                    }

                    val nextStart = result.messages.lastOrNull()?.timetoken

                    callback(messages, nextStart, null)
                } else {
                    status.exception?.message?.let { Logging.print(it) }
                    callback(null, null, status.exception?.message)
                }
            }
        } catch (error: Exception) {
            Logging.print(error)
            callback(null, null, error.message)
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
        callback: ((String?, UserTokenModel?) -> Unit)?
    ) {
        if (userTokenModel.token != newJwt) {
            clearConnection()
            initiateChat(currentShowKey, newJwt, isGuest, callback)
        } else {
            callback?.invoke(null, userTokenModel)
        }
    }

    /**
     * Cleans up the current PubNub instance and prepares for re-initialization or shutdown.
     */
    internal fun clearConnection() {
        pubnub?.unsubscribeAll()
        pubnub?.destroy()
        pubnub = null
    }

    /**
     * Checks if the current show key has changed and re-initializes the PubNub instance if necessary.
     */
    private suspend fun handleShowKeyChange() {
        if (currentShowKey != globalShowKey) {
            currentShowKey = globalShowKey
            clearConnection()
            initializePubNub()
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
                status.exception?.errorMessage?.let { Logging.print(it) }
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
            if (response.statusCode in 200..299) {
                callback?.let { it(true, null) }
            } else {
                callback?.let { it(false, response.body) }
                println(response.body)
                Logging.print(response.body)
            }
        } catch (e: Exception) {
            Logging.print(e)
            callback?.let { it(false, e.message) }
        }
    }
}