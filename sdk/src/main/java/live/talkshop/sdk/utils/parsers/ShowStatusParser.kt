package live.talkshop.sdk.utils.parsers

import live.talkshop.sdk.core.authentication.globalShowId
import live.talkshop.sdk.core.show.models.ShowStatusModel
import live.talkshop.sdk.resources.Keys.KEY_DURATION
import live.talkshop.sdk.resources.Keys.KEY_FILENAME
import live.talkshop.sdk.resources.Keys.KEY_HLS_PLAYBACK_URL
import live.talkshop.sdk.resources.Keys.KEY_ID
import live.talkshop.sdk.resources.Keys.KEY_STATUS
import live.talkshop.sdk.resources.Keys.KEY_STREAM_IN_CLOUD
import live.talkshop.sdk.resources.Keys.KEY_STREAM_KEY
import live.talkshop.sdk.resources.Keys.KEY_TOTAL_VIEWS
import live.talkshop.sdk.resources.Keys.KEY_TRAILERS_URL
import live.talkshop.sdk.utils.helpers.HelperFunctions
import live.talkshop.sdk.resources.URLs
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
    fun parseFromJson(statusJson: JSONObject): ShowStatusModel {
        globalShowId = statusJson.getString(KEY_ID)
        return ShowStatusModel(
            statusJson.optString(KEY_STREAM_KEY, ""),
            statusJson.optString(KEY_STATUS, ""),
            statusJson.optString(KEY_HLS_PLAYBACK_URL, ""),
            URLs.createHSLUrl(statusJson.optString(KEY_FILENAME, "")),
            statusJson.optString(KEY_TRAILERS_URL, ""),
            statusJson.optString(KEY_ID, ""),
            HelperFunctions.parseInt(statusJson.optString(KEY_DURATION, "")),
            statusJson.getBoolean(KEY_STREAM_IN_CLOUD),
            statusJson.optInt(KEY_TOTAL_VIEWS, 0)
        )
    }
}