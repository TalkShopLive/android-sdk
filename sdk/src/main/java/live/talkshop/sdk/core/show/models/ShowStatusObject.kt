package live.talkshop.sdk.core.show.models

import android.util.Log
import com.google.gson.annotations.SerializedName
import live.talkshop.sdk.utils.helpers.HelperFunctions.parseInt
import live.talkshop.sdk.utils.networking.URLs.createHSLUrl
import org.json.JSONObject

data class ShowStatusObject(
    @SerializedName("show_key")
    val show_key: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("hls_playback_url")
    val hls_playback_url: String,

    @SerializedName("hls_url")
    val hls_url: String?,

    @SerializedName("trailer_url")
    val trailer_url: String,

    @SerializedName("event_id")
    val event_id: Int?,

    @SerializedName("duration")
    val duration: Int?
) {
    companion object {
        fun parseFromJson(statusJson: JSONObject): ShowStatusObject {
            val show = ShowStatusObject(
                statusJson.optString("stream_key", ""),
                statusJson.optString("status", ""),
                statusJson.optString("hls_playback_url", ""),
                createHSLUrl(statusJson.optString("filename", "")),
                statusJson.optString("trailer_url", ""),
                statusJson.optInt("id", 0),
                parseInt(statusJson.optString("duration", ""))
            )
            return show
        }
    }
}