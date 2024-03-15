package live.talkshop.sdk.core.chat

import live.talkshop.sdk.core.authentication.isDebugMode

class Logging {
    companion object {
        fun print(message: String) {
            if (isDebugMode) {
                println("TSL DEBUG: $message")
            }
        }

        fun print(tag: String, message: String) {
            if (isDebugMode) {
                println("TSL DEBUG: $tag: $message")
            }
        }

        fun print(tag: String, message: String, exception: Exception) {
            if (isDebugMode) {
                println("TSL DEBUG: $tag: $message\nException: ${exception.message}")
                exception.printStackTrace()
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