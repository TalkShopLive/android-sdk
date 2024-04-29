package live.talkshop.sdk.utils

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.authentication.isDNT
import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.HTTPMethod
import live.talkshop.sdk.resources.URLs.getCollectorUrl
import org.json.JSONObject

class Collector private constructor(context: Context) {
    private val deviceScreenResolution: String

    init {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        deviceScreenResolution = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            "${bounds.width()}x${bounds.height()}"
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
        }
    }

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
            put("version", "1.0.9")
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
            put("aspect", JSONObject().apply {
                put("browser_resolution", "NOT_SET")
                put("screen_resolution", deviceScreenResolution)
            })
        }

        if (isAuthenticated) {
            APIHandler.makeRequest(
                requestUrl = getCollectorUrl(),
                requestMethod = HTTPMethod.POST,
                payload = payload
            )
        }
    }

    companion object {
        @Volatile
        private var instance: Collector? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = Collector(context)
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