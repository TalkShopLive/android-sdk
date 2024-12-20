package live.talkshop.sdk.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.authentication.isDNT
import live.talkshop.sdk.resources.URLs.getCollectorUrl
import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.HTTPMethod
import org.json.JSONObject

internal class Collector private constructor() {
    private suspend fun collect(
        action: String,
        category: String,
        userId: String? = "NOT_SET",
        eventID: String? = "NOT_SET",
        showKey: String? = "NOT_SET",
        storeId: String? = "NOT_SET",
        videoKey: String? = "NOT_SET",
        showStatus: String? = "NOT_SET",
    ) {
        val timestamp = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("timestamp_utc", timestamp)
            put("user_id", userId)
            put("category", category)
            put("version", "1.1.3")
            put("action", action)
            put("application", "android")
            put("meta", JSONObject().apply {
                put("external", true)
                put("event_id", eventID)
                put("streaming_content_key", showKey)
                put("store_id", storeId)
                put("video_key", videoKey)
                put("video_status", showStatus)
            })
            put("utm", JSONObject().apply {
                put("source", "NOT_SET")
                put("campaign", "NOT_SET")
                put("medium", "NOT_SET")
                put("term", "NOT_SET")
                put("content", "NOT_SET")
            })
        }

        try {
            APIHandler.makeRequest(
                requestUrl = getCollectorUrl(),
                requestMethod = HTTPMethod.POST,
                payload = payload
            )
        } catch (e: Exception) {
            Logging.print(Collector::class.java, e)
        }
    }

    companion object {
        @Volatile
        private var instance: Collector? = null

        fun initialize() {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = Collector()
                    }
                }
            }
        }

        private fun getInstance(): Collector {
            return instance
                ?: throw IllegalStateException("Collector must be initialized in Application.onCreate()")
        }

        fun collect(
            action: String,
            category: String,
            userId: String? = null,
            eventID: String? = null,
            showKey: String? = null,
            storeId: String? = null,
            videoKey: String? = null,
            showStatus: String? = null,
        ) {
            if (isDNT) {
                return
            }
            CoroutineScope(Dispatchers.IO).launch {
                getInstance().collect(
                    action,
                    category,
                    userId,
                    eventID,
                    showKey,
                    storeId,
                    videoKey,
                    showStatus,
                )
            }
        }
    }
}