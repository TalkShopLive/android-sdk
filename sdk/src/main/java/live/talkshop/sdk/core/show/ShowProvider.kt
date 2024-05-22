package live.talkshop.sdk.core.show

import live.talkshop.sdk.core.show.models.ProductModel
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.core.show.models.ShowStatusModel
import live.talkshop.sdk.resources.APIClientError
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Constants.STATUS_LIVE
import live.talkshop.sdk.utils.Collector
import live.talkshop.sdk.utils.networking.APICalls.getCurrentEvent
import live.talkshop.sdk.utils.networking.APICalls.getShowDetails
import live.talkshop.sdk.utils.networking.APICalls.getShowProducts
import live.talkshop.sdk.utils.networking.APICalls.incrementView

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
        callback: ((APIClientError?, ShowModel?) -> Unit)? = null
    ) {
        getShowDetails(showKey).onError {
            callback?.invoke(it, null)
        }.onResult {
            callback?.invoke(null, it)
            Collector.collect(
                action = Constants.COLLECTOR_ACTION_SELECT_SHOW_METADATA,
                category = Constants.COLLECTOR_CAT_INTERACTION,
                eventID = it.eventId,
                showKey = showKey,
                showStatus = it.status,
            )
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
        callback: ((APIClientError?, ShowStatusModel?) -> Unit)? = null
    ) {
        getCurrentEvent(showKey).onError {
            callback?.invoke(it, null)
        }.onResult {
            if (it.streamInCloud == true && it.status == STATUS_LIVE) {
                if (!incrementViewCalledMap.containsKey(showKey) || !incrementViewCalledMap[showKey]!!) {
                    incrementView(it.eventId!!)
                    incrementViewCalledMap[showKey] = true
                    Collector.collect(
                        action = Constants.COLLECTOR_ACTION_VIEW_COUNT,
                        category = Constants.COLLECTOR_CAT_INTERACTION,
                        eventID = it.eventId,
                        showKey = showKey,
                        showStatus = it.status,
                    )
                }
            }
            callback?.invoke(null, it)
        }
    }

    /**
     * Fetches products for a given show key.
     *
     * @param showKey The key of the show to fetch products for.
     * @param callback The callback to return the result: an error or a list of products.
     */
    internal suspend fun fetchProducts(
        showKey: String,
        callback: ((APIClientError?, List<ProductModel>?) -> Unit)
    ) {
        getShowProducts(showKey).onError {
            callback.invoke(it, null)
        }.onResult {
            callback.invoke(null, it)
        }
    }
}