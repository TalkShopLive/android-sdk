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
import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.authentication.storedClientKey
import live.talkshop.sdk.core.chat.models.MessageModel
import live.talkshop.sdk.core.user.models.UserTokenModel
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Constants.CHANNEL_CHAT_PREFIX
import live.talkshop.sdk.resources.Constants.CHANNEL_EVENTS_PREFIX
import live.talkshop.sdk.resources.Constants.MESSAGE_ERROR_AUTH
import live.talkshop.sdk.resources.Constants.MESSAGE_ERROR_MESSAGE_MAX_LENGTH
import live.talkshop.sdk.resources.Constants.PLATFORM_TYPE_SDK
import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.HTTPMethod
import live.talkshop.sdk.utils.networking.URLs
import live.talkshop.sdk.utils.parsers.MessageParser
import live.talkshop.sdk.utils.parsers.ShowStatusParser
import live.talkshop.sdk.utils.parsers.UserTokenParser
import org.json.JSONObject
import java.util.Date

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
 */
class ChatProvider {
    private lateinit var userTokenModel: UserTokenModel
    private lateinit var channels: List<String>
    private var pubnub: PubNub? = null
    private lateinit var publishChannel: String
    private var eventsChannel: String? = null
    private var callback: ChatProviderCallback? = null

    /**
     * Sets the callback for handling chat events and messages.
     *
     * @param callback The callback to be invoked on chat events.
     */
    internal fun setCallback(callback: ChatProviderCallback) {
        this.callback = callback
    }

    /**
     * Initiates the chat by fetching a user token and setting up PubNub.
     *
     * @param showId The unique identifier for the chat session or show.
     * @param jwt The JWT token used for authentication.
     * @param isGuest Indicates whether the user is a guest.
     * @param callback A callback invoked upon the completion of the chat initiation process.
     */
    internal suspend fun initiateChat(
        showId: String,
        jwt: String,
        isGuest: Boolean,
        callback: ((String?, UserTokenModel?) -> Unit)?
    ) {
        if (isAuthenticated) {
            try {
                val url = if (isGuest) URLs.URL_GUEST_TOKEN else URLs.URL_FED_TOKEN
                val headers = mutableMapOf(
                    Constants.SDK_KEY to storedClientKey,
                    Constants.AUTH_KEY to "${Constants.BEARER_KEY} $jwt"
                )

                val response = APIHandler.makeRequest(url, HTTPMethod.POST, headers = headers)
                userTokenModel = UserTokenParser.fromJsonString(response)!!
                initializePubNub(showId)
                callback?.invoke(null, userTokenModel)
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
     *
     * @param showId The unique identifier for the chat session or show.
     */
    private suspend fun initializePubNub(showId: String) {
        val pnConfig = PNConfiguration(UserId(userTokenModel.userId)).apply {
            subscribeKey = userTokenModel.subscribeKey
            publishKey = userTokenModel.publishKey
            authKey = userTokenModel.token
            secure = true
        }

        pubnub = PubNub(pnConfig)
        subscribeChannels(showId)

    }

    /**
     * Subscribes to the chat and events channels.
     *
     * @param showId The unique identifier for the chat session or show.
     */
    private suspend fun subscribeChannels(showId: String) {
        val jsonResponse = APIHandler.makeRequest(
            "${URLs.URL_CURRENT_EVENT_ENDPOINT}$showId/${URLs.PATH_STREAMS_CURRENT}",
            HTTPMethod.GET
        )
        val showStatusModel = ShowStatusParser.parseFromJson(JSONObject(jsonResponse))
        publishChannel = CHANNEL_CHAT_PREFIX + showStatusModel.eventId
        eventsChannel = CHANNEL_EVENTS_PREFIX + showStatusModel.eventId
        channels = listOfNotNull(publishChannel, eventsChannel)
        subscribe()
    }

    /**
     * Subscribes to the channels and sets up listeners for handling chat events.
     */
    internal fun subscribe() {
        if (!isAuthenticated) {
            println(MESSAGE_ERROR_AUTH)
            return
        }
        val listener = object : SubscribeCallback() {
            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                when (pnMessageResult.channel) {
                    publishChannel -> {
                        val messageData: MessageModel? = MessageParser.parse(pnMessageResult)
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
    internal fun publish(message: String, callback: ((String?, String?) -> Unit)? = null) {
        if (!isAuthenticated) {
            callback?.invoke(MESSAGE_ERROR_AUTH, null)
            return
        }
        try {
            if (message.length > 200) {
                callback?.invoke(
                    MESSAGE_ERROR_MESSAGE_MAX_LENGTH,
                    null
                )
                return
            }

            val messageType = when {
                message.trim().contains("?") -> MessageModel.MessageType.QUESTION
                else -> MessageModel.MessageType.COMMENT
            }

            val messageObject = MessageModel(
                id = System.currentTimeMillis().toInt(),
                createdAt = Date().toString(),
                sender = userTokenModel.userId,
                text = message,
                type = messageType,
                platform = PLATFORM_TYPE_SDK
            )

            pubnub?.publish(publishChannel, messageObject)?.async { result, status ->
                if (!status.error) {
                    callback?.invoke(null, result!!.timetoken.toString())
                } else {
                    println(status.exception)
                    status.exception?.printStackTrace()
                    callback?.invoke(status.exception?.message, null)
                }
            }
        } catch (error: Exception) {
            error.printStackTrace()
            callback?.invoke(error.message, null)
        }
    }
}