package live.talkshop.sdk.utils.parsers

import android.annotation.SuppressLint
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Keys.KEY_ASSETS
import live.talkshop.sdk.resources.Keys.KEY_CHANNEL
import live.talkshop.sdk.resources.Keys.KEY_CHANNEL_ID
import live.talkshop.sdk.resources.Keys.KEY_DATA
import live.talkshop.sdk.resources.Keys.KEY_DESCRIPTION
import live.talkshop.sdk.resources.Keys.KEY_DURATION
import live.talkshop.sdk.resources.Keys.KEY_ENDED_AT
import live.talkshop.sdk.resources.Keys.KEY_ENTRANCE
import live.talkshop.sdk.resources.Keys.KEY_ID
import live.talkshop.sdk.resources.Keys.KEY_KEY
import live.talkshop.sdk.resources.Keys.KEY_KIND
import live.talkshop.sdk.resources.Keys.KEY_NAME
import live.talkshop.sdk.resources.Keys.KEY_PRODUCT_ID
import live.talkshop.sdk.resources.Keys.KEY_PRODUCT_IDS
import live.talkshop.sdk.resources.Keys.KEY_SCHEDULED_LIVE_AT
import live.talkshop.sdk.resources.Keys.KEY_SHOW_PRODUCTS
import live.talkshop.sdk.resources.Keys.KEY_STATE
import live.talkshop.sdk.resources.Keys.KEY_THUMBNAIL_IMAGE
import live.talkshop.sdk.resources.Keys.KEY_THUMBNAIL_IMAGE_URL
import live.talkshop.sdk.resources.Keys.KEY_TITLE
import live.talkshop.sdk.resources.Keys.KEY_TRAILER
import live.talkshop.sdk.resources.Keys.KEY_TYPE
import live.talkshop.sdk.resources.Keys.KEY_URL
import live.talkshop.sdk.utils.Logging
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
        val showDataJson = json.getJSONObject(KEY_DATA)
        val assets = showDataJson.getJSONArray(KEY_ASSETS)
        val channelDataJson = showDataJson.getJSONObject(KEY_CHANNEL)
        return ShowModel(
            id = showDataJson.optInt(KEY_ID, 0),
            showKey = showDataJson.optString(KEY_KEY, ""),
            name = showDataJson.optString(KEY_TITLE, ""),
            showDescription = showDataJson.optString(KEY_DESCRIPTION, ""),
            status = showDataJson.optString(KEY_STATE, "created"),
            trailerUrl = parseAssetUrlByType(showDataJson, KEY_TRAILER),
            hlsPlaybackUrl = parseAssetUrlByType(showDataJson, "live"),
            hlsUrl = parseAssetUrlByType(showDataJson, "vod"),
            airDate = showDataJson.optString(KEY_SCHEDULED_LIVE_AT, ""),
            eventId = assets.getJSONObject(0).optInt(KEY_ID, 0),
            storeId = showDataJson.optString(KEY_CHANNEL_ID, ""),
            cc = parseAssetUrlByType(showDataJson, "vod").replace("mp4", "transcript.vtt"),
            endedAt = parseDate(showDataJson.optString(KEY_ENDED_AT, "")),
            duration = assets.getJSONObject(0).optInt(KEY_DURATION, 0),
            videoThumbnailUrl = showDataJson.optString(KEY_THUMBNAIL_IMAGE_URL,""),
            channelLogo = channelDataJson.optString(KEY_THUMBNAIL_IMAGE, ""),
            channelName = channelDataJson.optString(KEY_NAME, ""),
            trailerDuration = parseAssetDurationByType(showDataJson),
            productIds = showDataJson.optJSONArray(KEY_PRODUCT_IDS)?.let {
                val intList = mutableListOf<Int>()
                for (i in 0 until it.length()) {
                    val value = it.optInt(i)
                    intList.add(value)
                }
                intList
            },
            entranceProductsIds = parseEntranceProductIds(showDataJson)
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
    private fun parseDate(dateString: String?): Date? {
        val raw = dateString?.trim()
        if (raw.isNullOrEmpty() || raw.equals("null", ignoreCase = true)) return null

        val patterns = listOf(
            Constants.DATE_FORMAT,
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        )
        for (p in patterns) {
            try {
                val fmt = SimpleDateFormat(p, Locale.getDefault())
                fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
                return fmt.parse(raw)
            } catch (_: Exception) { }
        }
        Logging.print(
            ShowParser::class.java,
            java.lang.IllegalArgumentException("Unparseable date: $raw")
        )
        return null
    }

    /**
     * Parses the first asset URL from the `assets` array that matches the given type.
     *
     * @param showDataJson The JSON object containing the `assets` array.
     * @param type The asset type to match (e.g., "trailer", "live", "vod").
     * @return The URL of the matching asset, or an empty string if not found.
     */
    private fun parseAssetUrlByType(showDataJson: JSONObject, type: String): String {
        val assetsArray = showDataJson.optJSONArray(KEY_ASSETS) ?: return ""
        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.optJSONObject(i) ?: continue
            if (asset.optString(KEY_TYPE) == type) {
                return asset.optString(KEY_URL, "")
            }
        }
        return ""
    }

    /**
     * Parses the duration of the trailer asset from the `assets` array.
     *
     * @param showDataJson The JSON object containing the `assets` array.
     * @return The duration of the trailer in seconds, or 0 if not found.
     */
    private fun parseAssetDurationByType(showDataJson: JSONObject): Int {
        val assetsArray = showDataJson.optJSONArray(KEY_ASSETS) ?: return 0
        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.optJSONObject(i) ?: continue
            if (asset.optString(KEY_TYPE) == KEY_TRAILER) {
                return asset.optInt(KEY_DURATION, 0)
            }
        }
        return 0
    }

    /**
     * Parses all `product_id` values from the `show_products` array where kind is "entrance".
     *
     * @param showDataJson The JSON object containing the `show_products` array.
     * @return A list of product IDs for entrance products. Empty if none are found.
     */
    private fun parseEntranceProductIds(showDataJson: JSONObject): List<Int> {
        val entranceIds = mutableListOf<Int>()
        val showProductsArray = showDataJson.optJSONArray(KEY_SHOW_PRODUCTS) ?: return entranceIds

        for (i in 0 until showProductsArray.length()) {
            val productObj = showProductsArray.optJSONObject(i) ?: continue
            if (productObj.optString(KEY_KIND) == KEY_ENTRANCE) {
                entranceIds.add(productObj.optInt(KEY_PRODUCT_ID))
            }
        }
        return entranceIds
    }
}