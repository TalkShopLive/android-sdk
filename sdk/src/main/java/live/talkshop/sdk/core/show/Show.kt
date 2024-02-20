package live.talkshop.sdk.core.show

import live.talkshop.sdk.core.show.models.ShowObject
import live.talkshop.sdk.resources.Constants

/**
 * Represents a class responsible for fetching details and status of a show.
 */
class Show {
    /**
     * Callback interface for handling successful or failed show details retrieval.
     */
    interface GetDetailsCallback {
        /**
         * Invoked when show details are successfully retrieved.
         *
         * @param showObject The object containing the details of the show.
         */
        fun onSuccess(showObject: ShowObject)

        /**
         * Invoked when an error occurs during show details retrieval.
         *
         * @param error A string describing the error that occurred.
         */
        fun onError(error: String)
    }

    /**
     * Callback interface for handling successful or failed current event status retrieval.
     */
    interface GetStatusShowCallback {
        /**
         * Invoked when current event status is successfully retrieved.
         *
         * @param status A string representing the status of the current event.
         */
        fun onSuccess(status: String)

        /**
         * Invoked when an error occurs during current event status retrieval.
         *
         * @param error A string describing the error that occurred.
         */
        fun onError(error: String)
    }


    companion object {
        // ShowProvider instance used for fetching show details and current event status
        private val showProvider = ShowProvider()

        /**
         * Fetches details of a show corresponding to the provided product key.
         *
         * @param productKey The product key of the show for which details are to be fetched.
         * @param callback The callback object to notify the user about the success or failure of the API call.
         */
        suspend fun getDetails(productKey: String, callback: GetDetailsCallback) {
            try {
                // Fetch show details JSON using the ShowProvider
                val showJson = showProvider.fetchShow(productKey)

                // Parse the JSON response to a ShowObject
                val showObject = ShowObject.parseFromJson(showJson)

                // Notify the user about the success
                callback.onSuccess(showObject)
            } catch (e: Exception) {
                // Notify the user about the error
                callback.onError(e.message ?: "Unknown error occurred")
            }
        }

        /**
         * Fetches the status of the current event for the provided product key.
         *
         * @param productKey The product key of the show for which status is to be fetched.
         * @param callback The callback object to notify the user about the success or failure of the API call.
         */
        suspend fun getStatus(productKey: String, callback: GetStatusShowCallback) {
            try {
                // Fetch current event details JSON using the ShowProvider
                val currentEventJson = showProvider.fetchCurrentEvent(productKey)

                // Extract and return the status from the JSON response
                val status = currentEventJson.getString(Constants.STATUS_KEY)

                // Notify the user about the success
                callback.onSuccess(status)
            } catch (e: Exception) {
                // Notify the user about the error
                callback.onError(e.message ?: "Unknown error occurred")
            }
        }
    }
}