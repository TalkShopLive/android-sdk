package live.talkshop.sdk.core.show.models

import com.google.gson.annotations.SerializedName
import live.talkshop.sdk.utils.helpers.HelperFunctions.parseInt
import live.talkshop.sdk.utils.networking.URLs.createHSLUrl
import org.json.JSONObject

/**
 * Represents the status model of a show, encapsulating various details related to the stream.
 *
 * This data class holds information about a show's streaming status.
 *
 * @property showKey The unique identifier for the show.
 * @property status The current status of the show.
 * @property hlsPlaybackUrl The URL for HLS playback of the show.
 * @property hlsUrl The HLS URL for the show stream. It can be null if not available.
 * @property trailerUrl The URL for the show's trailer.
 * @property eventId The unique identifier for the event. It can be null if the event ID is not available.
 * @property duration The duration of the show in seconds. It can be null if the duration is not specified.
 */
data class ShowStatusModel(
    @SerializedName("show_key")
    val showKey: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("hls_playback_url")
    val hlsPlaybackUrl: String,

    @SerializedName("hls_url")
    val hlsUrl: String?,

    @SerializedName("trailer_url")
    val trailerUrl: String,

    @SerializedName("event_id")
    val eventId: Int?,

    @SerializedName("duration")
    val duration: Int?
) {
    companion object {
        /**
         * Parses a JSON object into a ShowStatusModel instance.
         *
         * This method extracts show status-related information from a JSON object.
         *
         * @param statusJson The JSONObject containing show status information.
         * @return A ShowStatusModel instance populated with data from the provided JSON object.
         */
        fun parseFromJson(statusJson: JSONObject): ShowStatusModel {
            return ShowStatusModel(
                statusJson.optString("stream_key", ""),
                statusJson.optString("status", ""),
                statusJson.optString("hls_playback_url", ""),
                createHSLUrl(statusJson.optString("filename", "")),
                statusJson.optString("trailer_url", ""),
                statusJson.optInt("id", 0),
                parseInt(statusJson.optString("duration", ""))
            )
        }
    }
}