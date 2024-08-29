package live.talkshop.sdk.utils.parsers

import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import live.talkshop.sdk.utils.Logging
import live.talkshop.sdk.core.chat.models.MessageModel
import live.talkshop.sdk.core.chat.models.SenderModel
import live.talkshop.sdk.resources.Constants.MESSAGE_TYPE_COMMENT
import live.talkshop.sdk.resources.Keys.KEY_ASPECT_RATIO
import live.talkshop.sdk.resources.Keys.KEY_CHANNEL_CODE
import live.talkshop.sdk.resources.Keys.KEY_ID
import live.talkshop.sdk.resources.Keys.KEY_CREATED_AT
import live.talkshop.sdk.resources.Keys.KEY_IS_VERIFIED
import live.talkshop.sdk.resources.Keys.KEY_NAME
import live.talkshop.sdk.resources.Keys.KEY_PLATFORM
import live.talkshop.sdk.resources.Keys.KEY_PROFILE_URL
import live.talkshop.sdk.resources.Keys.KEY_SENDER
import live.talkshop.sdk.resources.Keys.KEY_TEXT
import live.talkshop.sdk.resources.Keys.KEY_TIME_MESSAGE
import live.talkshop.sdk.resources.Keys.KEY_TIME_ORIGINAL
import live.talkshop.sdk.resources.Keys.KEY_TIME_TOKEN
import live.talkshop.sdk.resources.Keys.KEY_TYPE
import org.json.JSONException
import org.json.JSONObject

internal object MessageParser {
    /**
     * Parses the payload of a [PNMessageResult] to a [MessageModel].
     *
     * @param message The [JSONObject] containing the message payload.
     * @return The parsed [MessageModel] or null if the parsing fails.
     */
    fun parse(message: JSONObject, timeToken: Long?): MessageModel? {
        return try {
            MessageModel(
                id = message.optLong(KEY_ID),
                createdAt = message.optString(KEY_CREATED_AT),
                sender = parseSender(message.get(KEY_SENDER).toString()),
                text = message.optString(KEY_TEXT),
                type = message.optString(KEY_TYPE) ?: MESSAGE_TYPE_COMMENT,
                platform = message.optString(KEY_PLATFORM),
                aspectRatio = message.optLong(KEY_ASPECT_RATIO),
                timeToken = timeToken,
                original = message.optJSONObject(KEY_TIME_ORIGINAL)?.let {
                    it.optJSONObject(KEY_TIME_MESSAGE)
                        ?.let { message -> parse(message, it.optLong(KEY_TIME_TOKEN)) }
                }
            )
        } catch (e: Exception) {
            Logging.print(MessageParser::class.java, e)
            null
        }
    }

    /**
     * Parses the sender field which can be a JSON object or a string.
     *
     * @param senderJson The [String] containing the sender information.
     * @return The parsed sender as a [SenderModel].
     */
    private fun parseSender(senderJson: String?): SenderModel? {
        return if (senderJson == null) {
            null
        } else {
            try {
                val jsonObject = JSONObject(senderJson)
                SenderModel(
                    id = if (!jsonObject.isNull(KEY_ID)) jsonObject.getString(KEY_ID) else null,
                    name = if (!jsonObject.isNull(KEY_NAME)) jsonObject.getString(KEY_NAME) else null,
                    profileUrl = if (!jsonObject.isNull(KEY_PROFILE_URL)) jsonObject.getString(
                        KEY_PROFILE_URL
                    ) else null,
                    channelCode = if (!jsonObject.isNull(KEY_CHANNEL_CODE)) jsonObject.getString(
                        KEY_CHANNEL_CODE
                    ) else null,
                    isVerified = if (!jsonObject.isNull(KEY_IS_VERIFIED)) jsonObject.getBoolean(
                        KEY_IS_VERIFIED
                    ) else false
                )
            } catch (e: JSONException) {
                SenderModel(name = senderJson)
            }
        }
    }
}