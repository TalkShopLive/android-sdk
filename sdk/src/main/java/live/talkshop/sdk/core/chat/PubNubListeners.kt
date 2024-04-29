package live.talkshop.sdk.core.chat

import com.pubnub.api.PubNub
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
import live.talkshop.sdk.core.chat.models.MessageModel
import live.talkshop.sdk.core.chat.models.SenderModel
import live.talkshop.sdk.resources.APIClientError
import live.talkshop.sdk.utils.Logging
import live.talkshop.sdk.utils.networking.APICalls
import live.talkshop.sdk.utils.parsers.MessageParser
import org.json.JSONObject

internal class PubNubListeners(
    var callback: ChatCallback?,
    val userMetadataCache: MutableMap<String, SenderModel>,
    var publishChannel: String,
    var eventsChannel: String?,
) {
    inner class TSLSubscribeCallback : SubscribeCallback() {
        private var triedToReconnectBefore: Boolean = false
        override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
            when (pnMessageResult.channel) {
                publishChannel -> {
                    val messageData: MessageModel? =
                        MessageParser.parse(
                            JSONObject(pnMessageResult.message.asJsonObject.toString()),
                            pnMessageResult.timetoken
                        )
                    if (messageData != null) {
                        val uuid = messageData.sender?.id
                        if (uuid != null && userMetadataCache.containsKey(uuid)) {
                            messageData.sender = userMetadataCache[uuid]
                            callback?.onMessageReceived(messageData)
                        } else if (uuid != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                APICalls.getUserMeta(uuid).onResult {
                                    userMetadataCache[uuid] = it
                                }
                                messageData.sender = userMetadataCache[uuid]
                                withContext(Dispatchers.Main) {
                                    callback?.onMessageReceived(messageData)
                                }
                            }
                        } else {
                            callback?.onMessageReceived(messageData)
                        }
                    } else {
                        Logging.print("messageData is null")
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
                callback?.onStatusChange(APIClientError.CHAT_CONNECTION_ERROR)
            } else if (pnStatus.category == PNStatusCategory.PNConnectedCategory || pnStatus.category == PNStatusCategory.PNReconnectedCategory) {
                triedToReconnectBefore = false
            } else if (pnStatus.category == PNStatusCategory.PNTimeoutCategory) {
                callback?.onStatusChange(APIClientError.CHAT_TIMEOUT)
            } else if (pnStatus.category == PNStatusCategory.PNAccessDeniedCategory) {
                callback?.onStatusChange(APIClientError.PERMISSION_DENIED)
            }
        }

        override fun presence(
            pubnub: PubNub,
            pnPresenceEventResult: PNPresenceEventResult
        ) {
            Logging.print("Presence event: ${pnPresenceEventResult.event} on channel: ${pnPresenceEventResult.channel}")
        }

        override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {
            Logging.print("Signal received on channel: ${pnSignalResult.channel} with content: ${pnSignalResult.message}")
        }

        override fun messageAction(
            pubnub: PubNub,
            pnMessageActionResult: PNMessageActionResult
        ) {
            Logging.print("Message action type: ${pnMessageActionResult.messageAction.type} on message: ${pnMessageActionResult.messageAction.value}")
        }

        override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {
            Logging.print("File event received on channel: ${pnFileEventResult.channel}, file name: ${pnFileEventResult.file.name}")
        }

        override fun objects(pubnub: PubNub, objectEvent: PNObjectEventResult) {
            when (objectEvent.extractedMessage) {
                is PNSetChannelMetadataEventMessage -> {
                    val message =
                        objectEvent.extractedMessage as PNSetChannelMetadataEventMessage
                    Logging.print("Channel metadata set event received, channel: ${message.data.name}")
                }

                is PNSetUUIDMetadataEventMessage -> {
                    val message =
                        objectEvent.extractedMessage as PNSetUUIDMetadataEventMessage
                    Logging.print("UUID metadata set event received, UUID: ${message.data.id}")
                }

                is PNSetMembershipEventMessage -> {
                    val message =
                        objectEvent.extractedMessage as PNSetMembershipEventMessage
                    Logging.print("Membership set event received, channel: ${message.data.channel}, UUID: ${message.data.uuid}")
                }

                is PNDeleteChannelMetadataEventMessage -> {
                    val message =
                        objectEvent.extractedMessage as PNDeleteChannelMetadataEventMessage
                    Logging.print("Channel metadata delete event received, channel: ${message.channel}")
                }

                is PNDeleteUUIDMetadataEventMessage -> {
                    val message =
                        objectEvent.extractedMessage as PNDeleteUUIDMetadataEventMessage
                    Logging.print("UUID metadata delete event received, UUID: ${message.uuid}")
                }

                is PNDeleteMembershipEventMessage -> {
                    val message =
                        objectEvent.extractedMessage as PNDeleteMembershipEventMessage
                    Logging.print("Membership delete event received, channel: ${message.data.channelId}, UUID: ${message.data.uuid}")
                }

                else -> Logging.print("Other object event received")
            }
        }
    }
}