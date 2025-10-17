package live.talkshop.sdk.utils.parsers

import live.talkshop.sdk.core.authentication.globalShowId
import live.talkshop.sdk.core.show.models.EventModel
import live.talkshop.sdk.resources.Keys.KEY_DATA
import live.talkshop.sdk.resources.Keys.KEY_DURATION
import live.talkshop.sdk.resources.Keys.KEY_ID
import live.talkshop.sdk.resources.Keys.KEY_STATE
import live.talkshop.sdk.resources.Keys.KEY_URL
import org.json.JSONObject

internal object ShowStatusParser {
    /**
     * Parses a JSON object into a ShowStatusModel instance.
     *
     * This method extracts show status-related information from a JSON object.
     *
     * @param statusJson The JSONObject containing show status information.
     * @return A ShowStatusModel instance populated with data from the provided JSON object.
     */
    fun parseFromJson(statusJson: JSONObject, showKey: String): EventModel {
        globalShowId = showKey
        val json = statusJson.getJSONObject(KEY_DATA)

        val status = json.optString(KEY_STATE, "")
        val url: String? = json.optString(KEY_URL, "").takeIf { it.isNotBlank() }

        val (hlsPlaybackUrl, hlsUrl) = when (status.lowercase()) {
            "live" -> url to null
            "vod" -> null to url
            else -> null to null
        }

        return EventModel(
            showKey,
            status = status,
            hlsPlaybackUrl = hlsPlaybackUrl,
            hlsUrl = hlsUrl,
            trailerUrl = null,
            eventId = json.optInt(KEY_ID, 0),
            duration = json.optInt(KEY_DURATION, 0),
        )
    }
}