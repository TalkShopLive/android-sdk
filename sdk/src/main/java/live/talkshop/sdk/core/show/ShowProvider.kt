package live.talkshop.sdk.core.show

import live.talkshop.sdk.core.show.models.ProductModel
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.core.show.models.EventModel
import live.talkshop.sdk.resources.APIClientError
import live.talkshop.sdk.resources.Constants
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
    suspend fun fetchShow(
        showKey: String,
        callback: ((APIClientError?, ShowModel?) -> Unit)? = null,
    ) {
        getShowDetails(showKey).onError {
            callback?.invoke(it, null)
        }.onResult {
            callback?.invoke(null, it)
        }
    }

    /**
     * Fetches current event details for the specified product key, handling the network request and authentication check.
     *
     * @param showKey The product key for which current event status is to be fetched.
     * @param callback An optional callback that is invoked upon completion of the request.
     * It provides an error message if something goes wrong, or the ShowStatusModel if successful.
     */
    suspend fun fetchCurrentEvent(
        showKey: String,
        callback: ((APIClientError?, EventModel?) -> Unit)? = null,
    ) {
        getCurrentEvent(showKey).onError {
            callback?.invoke(it, null)
        }.onResult {
            if (it.status == Constants.STATUS_LIVE) {
                if (!incrementViewCalledMap.containsKey(showKey) || !incrementViewCalledMap[showKey]!!) {
                    incrementView(it.eventId.toString())
                    incrementViewCalledMap[showKey] = true
                }
            }
            callback?.invoke(null, it)
        }
    }

    /**
     * Fetches products for a given show key.
     *
     * @param showKey The key of the show to fetch products for.
     * @property preLive A flag indicating whether the request is related to pre products.
     * @param callback The callback to return the result: an error or a list of products.
     */
    suspend fun fetchProducts(
        showKey: String,
        preLive: Boolean,
        callback: ((APIClientError?, List<ProductModel>?) -> Unit),
    ) {
        getShowProducts(showKey, preLive).onError {
            callback.invoke(it, null)
        }.onResult {
            callback.invoke(null, it)
        }
    }
}