package live.talkshop.sdk.core.show

import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.core.show.models.ShowStatusModel
import live.talkshop.sdk.resources.Constants.MESSAGE_ERROR_AUTH
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
     * @param showKey The product key for which show details are to be fetched.
     * @param callback An optional callback that is invoked upon completion of the request.
     * It provides an error message if something goes wrong, or the ShowModel if successful.
     */
    internal suspend fun fetchShow(
        showKey: String,
        callback: ((String?, ShowModel?) -> Unit)? = null
    ) {
        if (!isAuthenticated) {
            callback?.invoke(MESSAGE_ERROR_AUTH, null)
            return
        }

        try {
            val jsonResponse =
                APIHandler.makeRequest("${URLs.URL_SHOW_DETAILS_ENDPOINT}$showKey", HTTPMethod.GET)
            val showModel = ShowParser.parseFromJson(JSONObject(jsonResponse))
            callback?.invoke(null, showModel)
        } catch (e: Exception) {
            callback?.invoke(e.message, null)
        }
    }

    /**
     * Fetches current event details for the specified product key, handling the network request and authentication check.
     *
     * @param showKey The product key for which current event status is to be fetched.
     * @param callback An optional callback that is invoked upon completion of the request.
     * It provides an error message if something goes wrong, or the ShowStatusModel if successful.
     */
    internal suspend fun fetchCurrentEvent(
        showKey: String,
        callback: ((String?, ShowStatusModel?) -> Unit)? = null
    ) {
        if (!isAuthenticated) {
            callback?.invoke(MESSAGE_ERROR_AUTH, null)
            return
        }

        try {
            val jsonResponse = APIHandler.makeRequest(
                "${URLs.URL_CURRENT_EVENT_ENDPOINT}$showKey/${URLs.PATH_STREAMS_CURRENT}",
                HTTPMethod.GET
            )
            val showStatusModel = ShowStatusParser.parseFromJson(JSONObject(jsonResponse))
            callback?.invoke(null, showStatusModel)
        } catch (e: Exception) {
            callback?.invoke(e.message, null)
        }
    }
}