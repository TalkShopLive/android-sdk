package live.talkshop.sdk.core.show.models

import android.annotation.SuppressLint
import com.google.gson.annotations.SerializedName
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.utils.networking.URLs.createCCUrl
import org.json.JSONException
import org.json.JSONObject
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
    val cc: String?
) {
    companion object {
        @SuppressLint("ConstantLocale")
        private val dateFormatter = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())

        fun parseFromJson(json: JSONObject): ShowObject {
            val productJson = json.getJSONObject("product")
            return ShowObject(
                productJson.optInt("id", 0),
                productJson.optString("product_key", ""),
                productJson.optString("name", ""),
                productJson.optString("description", ""),
                productJson.optString("status", ""),
                productJson.optString("hls_playback_url", ""),
                productJson.optString("hls_url", ""),
                productJson.optString("trailer_url", ""),
                parseDate(productJson.optString("air_date", "")),
                parseDate(productJson.optString("ended_at", "")),
                productJson.optString("event_id", ""),
                createCCUrl(productJson.optString("cc", ""))
            )
        }

        private fun parseDate(dateString: String): Date? {
            return if (dateString.isNotEmpty()) dateFormatter.parse(dateString) else null
        }
    }
}