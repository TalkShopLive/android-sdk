package live.talkshop.sdk.utils.parsers

import live.talkshop.sdk.core.authentication.globalShowId
import live.talkshop.sdk.core.show.models.EventModel
import live.talkshop.sdk.resources.Keys.KEY_DATA
import live.talkshop.sdk.resources.Keys.KEY_DURATION
import live.talkshop.sdk.resources.Keys.KEY_HLS_PLAYBACK_URL
import live.talkshop.sdk.resources.Keys.KEY_ID
import live.talkshop.sdk.resources.Keys.KEY_STATE
import live.talkshop.sdk.resources.Keys.KEY_STREAM_KEY
import live.talkshop.sdk.resources.Keys.KEY_TOTAL_VIEWS
import live.talkshop.sdk.resources.Keys.KEY_TRAILERS_URL
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
    fun parseFromJson(statusJson: JSONObject, showKey: String? = null): EventModel {
        globalShowId = showKey
        val json = statusJson.getJSONObject(KEY_DATA)
        return EventModel(
            json.optString(KEY_STREAM_KEY, ""),
            json.optString(KEY_STATE, ""),
            json.optString(KEY_HLS_PLAYBACK_URL, ""),
            json.optString(KEY_URL, ""),
            json.optString(KEY_TRAILERS_URL, ""),
            json.optString(KEY_ID, ""),
            json.optInt(KEY_DURATION, 0),
            json.optInt(KEY_TOTAL_VIEWS, 0)
        )
    }
}