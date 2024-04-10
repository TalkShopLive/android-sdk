package live.talkshop.sdk.utils

import live.talkshop.sdk.core.authentication.isDebugMode
import live.talkshop.sdk.resources.APIClientError

class Logging {
    companion object {
        fun print(message: String) {
            if (isDebugMode) {
                println("TSL DEBUG: $message")
            }
        }

        fun print(message: APIClientError) {
            if (isDebugMode) {
                println("TSL DEBUG: $message")
            }
        }

        fun print(message: APIClientError, exception: Exception) {
            if (isDebugMode) {
                println("TSL DEBUG: $message: " + exception.message)
            }
        }

        fun print(exception: Exception) {
            if (isDebugMode) {
                println("TSL DEBUG: Exception: ${exception.message}")
                exception.printStackTrace()
            }
        }
    }
}