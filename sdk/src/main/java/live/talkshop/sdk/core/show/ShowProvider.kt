package live.talkshop.sdk.core.show

import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.chat.Logging
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.core.show.models.ShowStatusModel
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Constants.MESSAGE_ERROR_AUTH
import live.talkshop.sdk.resources.Constants.STATUS_LIVE
import live.talkshop.sdk.utils.Collector
import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.HTTPMethod
import live.talkshop.sdk.utils.networking.URLs
import live.talkshop.sdk.utils.networking.URLs.getIncrementViewUrl
import live.talkshop.sdk.utils.networking.URLs.getShowDetailsUrl
import live.talkshop.sdk.utils.parsers.ShowParser
import live.talkshop.sdk.utils.parsers.ShowStatusParser
import org.json.JSONObject

/**
 * This class is responsible for fetching show details and current event status from the network.
 */
internal class ShowProvider {
    private val incrementViewCalledMap: MutableMap<String, Boolean> = mutableMapOf()

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
                APIHandler.makeRequest(getShowDetailsUrl(showKey), HTTPMethod.GET)
            val showModel = ShowParser.parseFromJson(JSONObject(jsonResponse.body))
            callback?.invoke(null, showModel)
            Collector.collect(
                action = Constants.COLLECTOR_ACTION_SELECT_VIEW_SHOW_DETAILS,
                category = Constants.COLLECTOR_CAT_INTERACTION,
                eventID = showModel.eventId,
                showKey = showKey,
                showStatus = showModel.status,
                duration = showModel.duration
            )
        } catch (e: Exception) {
            Logging.print(e)
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
                URLs.getCurrentStreamUrl(showKey),
                HTTPMethod.GET
            )

            val showStatusModel = try {
                ShowStatusParser.parseFromJson(JSONObject(jsonResponse.body))
            } catch (e: Exception) {
                ShowStatusModel()
            }
            if (showStatusModel.streamInCloud == true && showStatusModel.status == STATUS_LIVE) {
                if (!incrementViewCalledMap.containsKey(showKey) || !incrementViewCalledMap[showKey]!!) {
                    incrementView(showStatusModel.eventId!!)
                    incrementViewCalledMap[showKey] = true
                }
            }

            callback?.invoke(null, showStatusModel)
        } catch (e: Exception) {
            Logging.print(e)
            callback?.invoke(e.message, null)
        }
    }

    /**
     * Private method to increment view count for a show.
     *
     * @param eventId The event id for which view count is to be incremented.
     */
    private suspend fun incrementView(eventId: String) {
        try {
            APIHandler.makeRequest(getIncrementViewUrl(eventId), HTTPMethod.POST)
        } catch (e: Exception) {
            Logging.print(e)
        }
    }
}