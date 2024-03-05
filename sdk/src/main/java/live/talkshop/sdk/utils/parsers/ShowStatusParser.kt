package live.talkshop.sdk.utils.parsers

import live.talkshop.sdk.core.show.models.ShowStatusModel
import live.talkshop.sdk.resources.Keys.DURATION
import live.talkshop.sdk.resources.Keys.FILENAME
import live.talkshop.sdk.resources.Keys.HLS_PLAYBACK_URL
import live.talkshop.sdk.resources.Keys.ID
import live.talkshop.sdk.resources.Keys.STATUS
import live.talkshop.sdk.resources.Keys.STREAM_KEY
import live.talkshop.sdk.resources.Keys.TRAILERS_URL
import live.talkshop.sdk.utils.helpers.HelperFunctions
import live.talkshop.sdk.utils.networking.URLs
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
        return ShowStatusModel(
            statusJson.optString(STREAM_KEY, ""),
            statusJson.optString(STATUS, ""),
            statusJson.optString(HLS_PLAYBACK_URL, ""),
            URLs.createHSLUrl(statusJson.optString(FILENAME, "")),
            statusJson.optString(TRAILERS_URL, ""),
            statusJson.optInt(ID, 0),
            HelperFunctions.parseInt(statusJson.optString(DURATION, ""))
        )
    }
}