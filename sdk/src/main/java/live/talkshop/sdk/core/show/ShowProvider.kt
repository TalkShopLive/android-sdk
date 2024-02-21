package live.talkshop.sdk.core.show

import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.HTTPMethod
import live.talkshop.sdk.utils.networking.URLs
import live.talkshop.sdk.utils.networking.URLs.PATH_CURRENT_EVENT
import org.json.JSONObject

/**
 * This class provides methods to fetch show details and current events for a given product key.
 */
class ShowProvider {
    /**
     * Fetches show details for the specified product key.
     *
     * @param showId The product key for which show details are to be fetched.
     * @return A JSON object containing the show details.
     * @throws Exception if an error occurs during the request.
     */
    internal suspend fun fetchShow(showId: String): JSONObject {
        // Delegate exception handling to the caller (Show class)
        val jsonResponse = APIHandler.makeRequest(
            "${URLs.SHOW_DETAILS_ENDPOINT_URL}$showId",
            HTTPMethod.GET
        )
        return JSONObject(jsonResponse)
    }

    /**
     * Fetches current event details for the specified product key.
     *
     * @param showId The product key for which current event details are to be fetched.
     * @return A JSON object containing the current event details.
     * @throws Exception if an error occurs during the request.
     */
    internal suspend fun fetchCurrentEvent(showId: String): JSONObject {
        // Delegate exception handling to the caller (Show class)
        val jsonResponse = APIHandler.makeRequest(
            "${URLs.CURRENT_EVENT_ENDPOINT_URL}$showId/${URLs.PATH_CURRENT_EVENT}",
            HTTPMethod.GET
        )
        return JSONObject(jsonResponse)
    }
}