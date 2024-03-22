package live.talkshop.sdk.utils

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.authentication.isDNT
import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.HTTPMethod
import live.talkshop.sdk.utils.networking.URLs.getCollectorUrl
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
        userId: String? = null,
        eventID: String? = null,
        showKey: String? = null,
        storeId: Int? = null,
        videoKey: String? = null,
        showStatus: String? = null,
        duration: Int? = null
    ) {
        val timestamp = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("timestamp_utc", timestamp)
            put("user_id", userId)
            put("category", category)
            put("version", "1.0.0-beta")
            put("action", action)
            put("application", "android")
            put("meta", JSONObject().apply {
                put("external", true)
                if (eventID != null)
                    put("event_id", eventID)
                if (showKey != null)
                    put("streaming_content_key", showKey)
                if (storeId != null)
                    put("store_id", storeId)
                if (videoKey != null)
                    put("video_key", videoKey)
                if (showStatus != null)
                    put("video_status", showStatus)
                if (duration != null)
                    put("video_time", duration)
            })
            put("aspect", JSONObject().apply {
                put("screen_resolution", deviceScreenResolution)
            })
        }

        APIHandler.makeRequest(
            requestUrl = getCollectorUrl(),
            requestMethod = HTTPMethod.POST,
            payload = payload
        )
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
            storeId: Int? = null,
            videoKey: String? = null,
            showStatus: String? = null,
            duration: Int? = null
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
                    duration
                )
            }
        }
    }
}