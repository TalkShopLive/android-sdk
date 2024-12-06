package live.talkshop.sdk.core.show

import live.talkshop.sdk.core.show.models.ProductModel
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.core.show.models.ShowStatusModel
import live.talkshop.sdk.resources.APIClientError

/**
 * This class provides static methods to fetch details and status of a show.
 */
class Show {
    companion object {
        private val showProvider = ShowProvider()

        /**
         * Fetches details of a show corresponding to the provided product key.
         *
         * @param showKey The product key of the show for which details are to be fetched.
         * @param callback An optional callback that is invoked upon completion of the request.
         * It provides an error message if something goes wrong, or the ShowModel if successful.
         */
        suspend fun getDetails(
            showKey: String,
            callback: ((APIClientError?, ShowModel?) -> Unit)? = null
        ) {
            showProvider.fetchShow(showKey, callback)
        }

        /**
         * Fetches the status of the current event for a given show.
         *
         * @param showKey The product key of the show for which the current event status is to be fetched.
         * @param callback An optional callback that is invoked upon completion of the request.
         * It provides an error message if something goes wrong, or the ShowStatusModel if successful.
         */
        suspend fun getStatus(
            showKey: String,
            callback: ((APIClientError?, ShowStatusModel?) -> Unit)? = null
        ) {
            showProvider.fetchCurrentEvent(showKey, callback)
        }

        /**
         * Fetches the products for a given show.
         *
         * @param showKey The product key of the show.
         * @property preLive A flag indicating whether the request is related to pre products. Default is false.
         * @param callback An callback that is invoked upon completion of the request.
         * It provides an error message if something goes wrong, or a list of Products if successful.
         */
        suspend fun getProducts(
            showKey: String,
            preLive: Boolean = false,
            callback: ((APIClientError?, List<ProductModel>?) -> Unit)
        ) {
            showProvider.fetchProducts(showKey, preLive, callback)
        }
    }
}