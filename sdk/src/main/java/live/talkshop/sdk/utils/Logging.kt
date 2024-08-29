package live.talkshop.sdk.utils

import live.talkshop.sdk.core.authentication.isDebugMode
import live.talkshop.sdk.resources.APIClientError

internal class Logging {
    companion object {
        fun print(className: Class<*>, message: String) {
            if (isDebugMode) {
                println("${className.simpleName}: $message")
            }
        }

        fun print(className: Class<*>, exception: Exception) {
            if (isDebugMode) {
                println("${className.simpleName}: ${exception.message}")
                exception.printStackTrace()
            }
        }

        fun print(className: Class<*>, message: APIClientError) {
            if (isDebugMode) {
                println("${className.simpleName}: $message")
            }
        }

        fun print(className: Class<*>, message: APIClientError, exception: Exception) {
            if (isDebugMode) {
                println("${className.simpleName}: $message: " + exception.message)
            }
        }
    }
}