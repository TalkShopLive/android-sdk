package live.talkshop.sdk.core.show

import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.core.show.models.ShowStatusModel

/**
 * This class provides static methods to fetch details and status of a show.
 */
class Show {
    companion object {
        private val showProvider = ShowProvider()

        /**
         * Fetches details of a show corresponding to the provided product key.
         *
         * @param showId The product key of the show for which details are to be fetched.
         * @param callback An optional callback that is invoked upon completion of the request.
         * It provides an error message if something goes wrong, or the ShowModel if successful.
         */
        suspend fun getDetails(
            showId: String,
            callback: ((String?, ShowModel?) -> Unit)? = null
        ) {
            showProvider.fetchShow(showId, callback)
        }

        /**
         * Fetches the status of the current event for a given show.
         *
         * @param showId The product key of the show for which the current event status is to be fetched.
         * @param callback An optional callback that is invoked upon completion of the request.
         * It provides an error message if something goes wrong, or the ShowStatusModel if successful.
         */
        suspend fun getStatus(
            showId: String,
            callback: ((String?, ShowStatusModel?) -> Unit)? = null
        ) {
            showProvider.fetchCurrentEvent(showId, callback)
        }
    }
}