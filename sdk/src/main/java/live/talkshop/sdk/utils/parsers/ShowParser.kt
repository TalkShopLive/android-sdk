package live.talkshop.sdk.utils.parsers

import android.annotation.SuppressLint
import live.talkshop.sdk.core.chat.Logging
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Keys.KEY_AIR_DATES
import live.talkshop.sdk.resources.Keys.KEY_ATTACHMENT
import live.talkshop.sdk.resources.Keys.KEY_BRAND_NAME
import live.talkshop.sdk.resources.Keys.KEY_DATE
import live.talkshop.sdk.resources.Keys.KEY_DESCRIPTION
import live.talkshop.sdk.resources.Keys.KEY_DURATION
import live.talkshop.sdk.resources.Keys.KEY_ENDED_AT
import live.talkshop.sdk.resources.Keys.KEY_EVENTS
import live.talkshop.sdk.resources.Keys.KEY_EVENT_ID
import live.talkshop.sdk.resources.Keys.KEY_FILENAME
import live.talkshop.sdk.resources.Keys.KEY_HLS_PLAYBACK_URL
import live.talkshop.sdk.resources.Keys.KEY_ID
import live.talkshop.sdk.resources.Keys.KEY_IMAGE
import live.talkshop.sdk.resources.Keys.KEY_IMAGES
import live.talkshop.sdk.resources.Keys.KEY_LARGE
import live.talkshop.sdk.resources.Keys.KEY_MASTER
import live.talkshop.sdk.resources.Keys.KEY_NAME
import live.talkshop.sdk.resources.Keys.KEY_OWNING_STORE
import live.talkshop.sdk.resources.Keys.KEY_PRODUCT
import live.talkshop.sdk.resources.Keys.KEY_PRODUCT_KEY
import live.talkshop.sdk.resources.Keys.KEY_STATUS
import live.talkshop.sdk.resources.Keys.KEY_STREAMING_CONTENT
import live.talkshop.sdk.resources.Keys.KEY_TRAILERS
import live.talkshop.sdk.resources.Keys.KEY_VIDEO
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
        val productJson = json.getJSONObject(KEY_PRODUCT)
        val streamContentJson = productJson.getJSONObject(KEY_STREAMING_CONTENT)
        return ShowModel(
            productJson.optInt(KEY_ID, 0),
            productJson.optString(KEY_PRODUCT_KEY, ""),
            productJson.optString(KEY_NAME, ""),
            productJson.optString(KEY_DESCRIPTION, ""),
            parseEventsArray(productJson, KEY_STATUS),
            parseEventsArray(productJson, KEY_HLS_PLAYBACK_URL),
            URLs.createHSLUrl(parseEventsArray(productJson, KEY_FILENAME)),
            parseTrailerUrl(streamContentJson, KEY_VIDEO),
            parseDate(parseAirDates(productJson, KEY_DATE)),
            parseDate(productJson.optString(KEY_ENDED_AT, "")),
            parseAirDates(productJson, KEY_EVENT_ID),
            URLs.createCCUrl(parseEventsArray(productJson, KEY_FILENAME)),
            HelperFunctions.parseInt(parseEventsArray(productJson, KEY_DURATION)),
            HelperFunctions.parseInt(parseTrailerUrl(streamContentJson, KEY_DURATION)),
            parseVideoThumbnailUrl(productJson),
            parseChannelLogo(productJson),
            productJson.optString(KEY_BRAND_NAME, "")
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
     * @param name The name of the field to retrieve from the trailers array.
     * @return The trailer URL or an empty string if not found.
     */
    private fun parseTrailerUrl(streamContentJson: JSONObject, name: String): String {
        try {
            val trailersArray = streamContentJson.optJSONArray(KEY_TRAILERS)
            if (trailersArray != null && trailersArray.length() > 0) {
                val firstTrailer = trailersArray.getJSONObject(0)
                return if (name == KEY_DURATION) {
                    firstTrailer.optInt(name).toString()
                } else {
                    firstTrailer.optString(name, "")
                }
            }
        } catch (e: Exception) {
            Logging.print(e)
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
            val eventsArray = productJson.getJSONArray(KEY_EVENTS)
            if (eventsArray.length() > 0) {
                val firstStatus = eventsArray.getJSONObject(0)
                return firstStatus.optString(name, "")
            }
        } catch (e: Exception) {
            Logging.print(e)
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
                productJson.getJSONObject(KEY_STREAMING_CONTENT).getJSONArray(KEY_AIR_DATES)
            if (airDatesArray.length() > 0) {
                val firstAirDate = airDatesArray.getJSONObject(0)
                return firstAirDate.optString(name, "")
            }
        } catch (e: Exception) {
            Logging.print(e)
        }
        return ""
    }

    /**
     * Parses the product JSON object to extract the video thumbnail URL.
     * It navigates through the nested JSON structure to find the thumbnail URL.
     * If any part of the structure is missing or there is an exception, it returns an empty string.
     *
     * @param productJson The JSON object containing the product details.
     * @return The extracted video thumbnail URL or an empty string if not found or in case of an error.
     */
    private fun parseVideoThumbnailUrl(productJson: JSONObject): String {
        return try {
            productJson.getJSONObject(KEY_MASTER).getJSONArray(KEY_IMAGES).getJSONObject(0)
                .getJSONObject(KEY_ATTACHMENT).optString(KEY_LARGE, "")
        } catch (e: Exception) {
            Logging.print(e)
            ""
        }
    }

    /**
     * Parses the product JSON object to extract the channel logo URL.
     * It navigates through the nested JSON structure to find the channel logo URL.
     * If any part of the structure is missing or there is an exception, it returns an empty string.
     *
     * @param productJson The JSON object containing the product details.
     * @return The extracted channel logo URL or an empty string if not found or in case of an error.
     */
    private fun parseChannelLogo(productJson: JSONObject): String {
        return try {
            productJson.getJSONObject(KEY_OWNING_STORE).getJSONObject(KEY_IMAGE)
                .getJSONObject(KEY_ATTACHMENT).optString(KEY_LARGE, "")
        } catch (e: Exception) {
            Logging.print(e)
            ""
        }
    }
}