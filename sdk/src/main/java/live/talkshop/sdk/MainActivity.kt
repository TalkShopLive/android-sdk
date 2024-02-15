package live.talkshop.sdk

import com.google.gson.JsonObject
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.UserId
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import java.util.Arrays


class MainActivity {

    companion object {
        fun sendMessage(): String {
            val pnConfiguration = PNConfiguration(UserId("myUserId"))
            pnConfiguration.subscribeKey = "demo"
            pnConfiguration.publishKey = "demo"

            val pubnub = PubNub(pnConfiguration)


            val channelName = "awesomeChannel"

            // create message payload using Gson

            // create message payload using Gson
            val messageJsonObject = JsonObject()
            messageJsonObject.addProperty("msg", "hello")

            pubnub.addListener(object : SubscribeCallback() {
                override fun status(pubnub: PubNub, status: PNStatus) {
                    if (status.category == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                        // This event happens when radio / connectivity is lost
                    } else if (status.category == PNStatusCategory.PNConnectedCategory) {

                        // Connect event. You can do stuff like publish, and know you'll get it.
                        // Or just use the connected event to confirm you are subscribed for
                        // UI / internal notifications, etc
                        if (status.category == PNStatusCategory.PNConnectedCategory) {
                            pubnub.publish().channel(channelName).message(messageJsonObject)
                                .async { result, status ->
                                    // Check whether request successfully completed or not.
                                    if (!status.isError) {

                                        // Message successfully published to specified channel.
                                    } else {

                                        // Handle message publish error. Check 'category' property to find out possible issue
                                        // because of which request did fail.
                                        //
                                        // Request can be resent using: [status retry];
                                    }
                                }
                        }
                    } else if (status.category == PNStatusCategory.PNReconnectedCategory) {

                        // Happens as part of our regular operation. This event happens when
                        // radio / connectivity is lost, then regained.
                    } else if (status.category == PNStatusCategory.PNDecryptionErrorCategory) {

                        // Handle messsage decryption error. Probably client configured to
                        // encrypt messages and on live data feed it received plain text.
                    }
                }

                override fun message(pubnub: PubNub, message: PNMessageResult) {
                    // Handle new message stored in message.message
                    if (message.channel != null) {
                        // Message has been received on channel group stored in
                        // message.getChannel()
                    } else {
                        // Message has been received on channel stored in
                        // message.getSubscription()
                    }
                    val receivedMessageObject = message.message
                    println("Received message content: $receivedMessageObject")
                    // extract desired parts of the payload, using Gson
                    val msg = message.message.asJsonObject["msg"].asString
                    println("msg content: $msg")

                    /*
                log the following items with your favorite logger
                    - message.getMessage()
                    - message.getSubscription()
                    - message.getTimetoken()
            */
                }

                override fun presence(pubnub: PubNub, presence: PNPresenceEventResult) {}
                override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {
                    TODO("Not yet implemented")
                }

                override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {
                    TODO("Not yet implemented")
                }

                override fun channel(
                    pubnub: PubNub,
                    pnChannelMetadataResult: PNChannelMetadataResult
                ) {
                    TODO("Not yet implemented")
                }

                override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {
                    TODO("Not yet implemented")
                }

                override fun messageAction(
                    pubnub: PubNub,
                    pnMessageActionResult: PNMessageActionResult
                ) {
                    TODO("Not yet implemented")
                }

                override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {
                    TODO("Not yet implemented")
                }
            })

            pubnub.subscribe().channels(Arrays.asList(channelName)).execute()


            return "Message to send: $messageJsonObject"
        }
    }
}