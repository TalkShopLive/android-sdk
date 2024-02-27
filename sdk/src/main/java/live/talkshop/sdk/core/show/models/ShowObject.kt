package live.talkshop.sdk.core.show.models

import android.annotation.SuppressLint
import com.google.gson.annotations.SerializedName
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.utils.networking.URLs.createCCUrl
import live.talkshop.sdk.utils.networking.URLs.createHSLUrl
import org.json.JSONObject
import java.lang.Integer.parseInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ShowObject(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("product_key")
    val showKey: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("hls_playback_url")
    val hlsPlaybackUrl: String?,

    @SerializedName("hls_url")
    val hlsUrl: String?,

    @SerializedName("trailer_url")
    val trailerUrl: String?,

    @SerializedName("air_date")
    val airDate: Date?,

    @SerializedName("ended_at")
    val endedAt: Date?,

    @SerializedName("event_id")
    val eventId: String?,

    @SerializedName("cc")
    val cc: String?,

    @SerializedName("duration")
    val duration: Int?
) {
    companion object {
        @SuppressLint("ConstantLocale")
        private val dateFormatter = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())

        fun parseFromJson(json: JSONObject): ShowObject {
            val productJson = json.getJSONObject("product")
            val streamContentJson = productJson.getJSONObject("streaming_content")
            return ShowObject(
                productJson.optInt("id", 0),
                productJson.optString("product_key", ""),
                productJson.optString("name", ""),
                productJson.optString("description", ""),
                parseEventsArray(productJson, "status"),
                parseEventsArray(productJson, "hls_playback_url"),
                createHSLUrl(parseEventsArray(productJson, "filename")),
                parseTrailerUrl(streamContentJson),
                parseDate(parseAirDates(productJson, "date")),
                parseDate(productJson.optString("ended_at", "")),
                parseAirDates(productJson, "event_id"),
                createCCUrl(parseEventsArray(productJson, "filename")),
                parseInt(parseEventsArray(productJson, "duration"))
            )
        }

        private fun parseDate(dateString: String): Date? {
            return if (dateString.isNotEmpty()) dateFormatter.parse(dateString) else null
        }

        private fun parseTrailerUrl(streamContentJson: JSONObject): String {
            try {
                val trailersArray = streamContentJson.optJSONArray("trailers")
                if (trailersArray != null && trailersArray.length() > 0) {
                    val firstTrailer = trailersArray.getJSONObject(0)
                    return firstTrailer.optString("video", "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }

        private fun parseEventsArray(productJson: JSONObject, name: String): String {
            try {
                val eventsArray = productJson.getJSONArray("events")
                if (eventsArray.length() > 0) {
                    val firstStatus = eventsArray.getJSONObject(0)
                    return firstStatus.optString(name, "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }

        private fun parseAirDates(productJson: JSONObject, name: String): String {
            try {
                val airDatesArray =
                    productJson.getJSONObject("streaming_content").getJSONArray("air_dates")
                if (airDatesArray.length() > 0) {
                    val firstAirDate = airDatesArray.getJSONObject(0)
                    return firstAirDate.optString(name, "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }
    }
}