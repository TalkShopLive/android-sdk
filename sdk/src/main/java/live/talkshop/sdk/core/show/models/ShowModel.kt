package live.talkshop.sdk.core.show.models

import android.annotation.SuppressLint
import com.google.gson.annotations.SerializedName
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.utils.helpers.HelperFunctions.parseInt
import live.talkshop.sdk.utils.networking.URLs.createCCUrl
import live.talkshop.sdk.utils.networking.URLs.createHSLUrl
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents the model for a show, encapsulating various details and metadata about it.
 *
 * This data class holds information about a show including identifiers, content details, streaming URLs,
 * dates, and other related information that are extracted from a JSON object.
 *
 * @property id The unique identifier of the show.
 * @property showKey The key associated with the show.
 * @property name The name of the show.
 * @property description A description of the show.
 * @property status The current status of the show.
 * @property hlsPlaybackUrl The HLS playback URL for the show.
 * @property hlsUrl The HLS URL for the show.
 * @property trailerUrl The URL for the show's trailer.
 * @property airDate The air date of the show.
 * @property endedAt The end date and time of the show.
 * @property eventId The event ID associated with the show.
 * @property cc Closed captioning information or URL.
 * @property duration The duration of the show in minutes.
 */
data class ShowModel(
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

        /**
         * Parses a JSON object to create an instance of ShowModel.
         *
         * This method extracts detailed information about a show from a JSON object
         * and constructs a ShowModel object with the extracted data.
         *
         * @param json The JSON object containing show details.
         * @return A ShowModel instance populated with data from the JSON object.
         */
        fun parseFromJson(json: JSONObject): ShowModel {
            val productJson = json.getJSONObject("product")
            val streamContentJson = productJson.getJSONObject("streaming_content")
            return ShowModel(
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

        /**
         * Parses a date string into a Date object.
         *
         * This method converts a date string into a Date object using the predefined date formatter.
         * If the date string is empty, it returns null.
         *
         * @param dateString The date string to parse.
         * @return A Date object or null if the dateString is empty or parsing fails.
         */
        private fun parseDate(dateString: String): Date? {
            return if (dateString.isNotEmpty()) dateFormatter.parse(dateString) else null
        }

        /**
         * Extracts the trailer URL from a streaming content JSON object.
         *
         * This method navigates through a streaming content JSON object to find and return
         * the first trailer URL. If no trailer is found, it returns an empty string.
         *
         * @param streamContentJson The JSON object containing streaming content details.
         * @return The trailer URL or an empty string if not found.
         */
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

        /**
         * Retrieves a specific value from an events array within a product JSON object.
         *
         * This method looks for a specified field within the first object of an events array
         * in the given product JSON object. If the array is empty or the field is not found,
         * it returns an empty string.
         *
         * @param productJson The JSON object containing the product details.
         * @param name The name of the field to retrieve from the events array.
         * @return The value of the specified field or an empty string if not found.
         */
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

        /**
         * Extracts a specific value from an air dates array within a streaming content JSON object.
         *
         * This method parses an air dates array to find a specific field's value in the first object.
         * If the array is empty or the field is not present, it returns an empty string.
         *
         * @param productJson The JSON object containing the product and streaming content details.
         * @param name The name of the field to extract from the air dates array.
         * @return The value of the field or an empty string if not found.
         */
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