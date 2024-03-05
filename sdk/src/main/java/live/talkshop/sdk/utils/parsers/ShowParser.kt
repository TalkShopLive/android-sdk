package live.talkshop.sdk.utils.parsers

import android.annotation.SuppressLint
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Keys.AIR_DATES
import live.talkshop.sdk.resources.Keys.DATE
import live.talkshop.sdk.resources.Keys.DESCRIPTION
import live.talkshop.sdk.resources.Keys.DURATION
import live.talkshop.sdk.resources.Keys.ENDED_AT
import live.talkshop.sdk.resources.Keys.EVENTS
import live.talkshop.sdk.resources.Keys.EVENT_ID
import live.talkshop.sdk.resources.Keys.FILENAME
import live.talkshop.sdk.resources.Keys.HLS_PLAYBACK_URL
import live.talkshop.sdk.resources.Keys.ID
import live.talkshop.sdk.resources.Keys.NAME
import live.talkshop.sdk.resources.Keys.PRODUCT
import live.talkshop.sdk.resources.Keys.PRODUCT_KEY
import live.talkshop.sdk.resources.Keys.STATUS
import live.talkshop.sdk.resources.Keys.STREAMING_CONTENT
import live.talkshop.sdk.resources.Keys.TRAILERS
import live.talkshop.sdk.resources.Keys.VIDEO
import live.talkshop.sdk.utils.helpers.HelperFunctions
import live.talkshop.sdk.utils.networking.URLs
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object ShowParser {

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
        val productJson = json.getJSONObject(PRODUCT)
        val streamContentJson = productJson.getJSONObject(STREAMING_CONTENT)
        return ShowModel(
            productJson.optInt(ID, 0),
            productJson.optString(PRODUCT_KEY, ""),
            productJson.optString(NAME, ""),
            productJson.optString(DESCRIPTION, ""),
            parseEventsArray(productJson, STATUS),
            parseEventsArray(productJson, HLS_PLAYBACK_URL),
            URLs.createHSLUrl(parseEventsArray(productJson, FILENAME)),
            parseTrailerUrl(streamContentJson),
            parseDate(parseAirDates(productJson, DATE)),
            parseDate(productJson.optString(ENDED_AT, "")),
            parseAirDates(productJson, EVENT_ID),
            URLs.createCCUrl(parseEventsArray(productJson, FILENAME)),
            HelperFunctions.parseInt(parseEventsArray(productJson, DURATION))
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
            val trailersArray = streamContentJson.optJSONArray(TRAILERS)
            if (trailersArray != null && trailersArray.length() > 0) {
                val firstTrailer = trailersArray.getJSONObject(0)
                return firstTrailer.optString(VIDEO, "")
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
            val eventsArray = productJson.getJSONArray(EVENTS)
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
                productJson.getJSONObject(STREAMING_CONTENT).getJSONArray(AIR_DATES)
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