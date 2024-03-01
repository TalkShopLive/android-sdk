package live.talkshop.sdk.core.show

import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.core.show.models.ShowStatusModel

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
         * @param showModel The object containing the details of the show.
         */
        fun onSuccess(showModel: ShowModel)

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
         * @param showStatusModel A string representing the status of the current event.
         */
        fun onSuccess(showStatusModel: ShowStatusModel)

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
         * @param showId The product key of the show for which details are to be fetched.
         * @param callback The callback object to notify the user about the success or failure of the API call.
         */
        suspend fun getDetails(showId: String, callback: GetDetailsCallback) {
            if (isAuthenticated) {
                try {
                    // Fetch show details JSON using the ShowProvider
                    val showJson = showProvider.fetchShow(showId)

                    // Parse the JSON response to a ShowObject
                    val showModel = ShowModel.parseFromJson(showJson)

                    // Notify the user about the success
                    callback.onSuccess(showModel)
                } catch (e: Exception) {
                    // Notify the user about the error
                    callback.onError(e.message ?: "Unknown error occurred")
                }
            } else {
                callback.onError("Authentication invalid")
            }
        }

        /**
         * Fetches the status of the current event for the provided product key.
         *
         * @param showId The product key of the show for which status is to be fetched.
         * @param callback The callback object to notify the user about the success or failure of the API call.
         */
        suspend fun getStatus(showId: String, callback: GetStatusShowCallback) {
            if (isAuthenticated) {
                try {
                    // Fetch current event details JSON using the ShowProvider
                    val currentEventJson = showProvider.fetchCurrentEvent(showId)

                    /// Parse the JSON response to a ShowStatusObject
                    val showStatusModel = ShowStatusModel.parseFromJson(currentEventJson)

                    // Notify the user about the success
                    callback.onSuccess(showStatusModel)
                } catch (e: Exception) {
                    // Notify the user about the error
                    callback.onError(e.message ?: "Unknown error occurred")
                }
            } else {
                callback.onError("Authentication invalid")
            }
        }
    }
}