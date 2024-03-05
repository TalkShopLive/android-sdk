package live.talkshop.sdk.core.show

import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.core.show.models.ShowStatusModel
import live.talkshop.sdk.resources.Constants.ERROR_AUTH_MESSAGE
import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.HTTPMethod
import live.talkshop.sdk.utils.networking.URLs
import live.talkshop.sdk.utils.parsers.ShowParser
import live.talkshop.sdk.utils.parsers.ShowStatusParser
import org.json.JSONObject

/**
 * This class is responsible for fetching show details and current event status from the network.
 */
internal class ShowProvider {
    /**
     * Fetches show details for the specified product key, handling the network request and authentication check.
     *
     * @param showId The product key for which show details are to be fetched.
     * @param callback An optional callback that is invoked upon completion of the request.
     * It provides an error message if something goes wrong, or the ShowModel if successful.
     */
    internal suspend fun fetchShow(
        showId: String,
        callback: ((String?, ShowModel?) -> Unit)? = null
    ) {
        if (!isAuthenticated) {
            callback?.invoke(ERROR_AUTH_MESSAGE, null)
            return
        }

        try {
            val jsonResponse =
                APIHandler.makeRequest("${URLs.URL_SHOW_DETAILS_ENDPOINT}$showId", HTTPMethod.GET)
            val showModel = ShowParser.parseFromJson(JSONObject(jsonResponse))
            callback?.invoke(null, showModel)
        } catch (e: Exception) {
            callback?.invoke(e.message, null)
        }
    }

    /**
     * Fetches current event details for the specified product key, handling the network request and authentication check.
     *
     * @param showId The product key for which current event status is to be fetched.
     * @param callback An optional callback that is invoked upon completion of the request.
     * It provides an error message if something goes wrong, or the ShowStatusModel if successful.
     */
    internal suspend fun fetchCurrentEvent(
        showId: String,
        callback: ((String?, ShowStatusModel?) -> Unit)? = null
    ) {
        if (!isAuthenticated) {
            callback?.invoke(ERROR_AUTH_MESSAGE, null)
            return
        }

        try {
            val jsonResponse = APIHandler.makeRequest(
                "${URLs.URL_CURRENT_EVENT_ENDPOINT}$showId/${URLs.PATH_CURRENT_EVENT}",
                HTTPMethod.GET
            )
            val showStatusModel = ShowStatusParser.parseFromJson(JSONObject(jsonResponse))
            callback?.invoke(null, showStatusModel)
        } catch (e: Exception) {
            callback?.invoke(e.message, null)
        }
    }
}