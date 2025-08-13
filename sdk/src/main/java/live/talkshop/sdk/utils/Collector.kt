package live.talkshop.sdk.utils

import android.content.res.Resources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.authentication.currentShow
import live.talkshop.sdk.core.authentication.isDNT
import live.talkshop.sdk.core.show.models.EventModel
import live.talkshop.sdk.resources.CollectorActions
import live.talkshop.sdk.resources.Constants.COLLECTOR_CAT_PAGE_VIEW
import live.talkshop.sdk.resources.Constants.COLLECTOR_CAT_PROCESS
import live.talkshop.sdk.resources.URLs.URL_COLLECTOR_WALMART
import live.talkshop.sdk.resources.URLs.URL_PUBLISH_PROD
import live.talkshop.sdk.resources.URLs.getCollectorUrl
import live.talkshop.sdk.resources.URLs.getCollectorWatchUrl
import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.HTTPMethod
import org.json.JSONObject

internal class Collector private constructor() {
    private suspend fun collect(
        action: CollectorActions,
        category: String,
        userId: String? = "NOT_SET",
        showKey: String? = "NOT_SET",
        storeId: String? = "NOT_SET",
        videoKey: String? = "NOT_SET",
        showStatus: String? = "NOT_SET",
        videoTime: Int? = 0,
        showTitle: String? = "NOT_SET",
        productKey: String? = "NOT_SET",
        variantId: Int? = 0,
    ) {
        val timestamp = System.currentTimeMillis()
        val display = Resources.getSystem().displayMetrics
        val payload = JSONObject().apply {
            put("timestamp_utc", timestamp)
            put("user_id", userId)
            put("category", category)
            put("version", "1.1.7")
            put("action", action)
            put("application", "android")
            put("meta", JSONObject().apply {
                put("external", true)
                put("streaming_content_key", showKey)
                put("store_id", storeId)
                put("video_key", videoKey)
                put("video_status", showStatus)
                put("video_time", videoTime)
                put("show_id", showKey)
                put("productKey", productKey)
                put("variantId", variantId)

            })
            put("page_metrics", JSONObject().apply {
                put("origin", URL_PUBLISH_PROD)
                put("host", URL_PUBLISH_PROD.removePrefix("https://"))
                put("referrer", URL_COLLECTOR_WALMART)
                put("page_url", getCollectorWatchUrl(showKey))
                put("page_url_raw", getCollectorWatchUrl(showKey))
                put("page_title", showTitle)
            })
            put("aspect", JSONObject().apply {
                put("browser_resolution", "NOT_SET")
                put("screen_resolution", "${display.heightPixels} x ${display.widthPixels}")
            })
        }

        try {
            APIHandler.makeRequest(
                requestUrl = getCollectorUrl(), requestMethod = HTTPMethod.POST, payload = payload
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
            action: CollectorActions,
            event: EventModel? = null,
            userId: String? = null,
            videoTime: Int? = 0,
            productKey: String? = null,
            variantId: Int? = 0,
        ) {
            if (isDNT) {
                return
            }
            CoroutineScope(Dispatchers.IO).launch {
                getInstance().collect(
                    action = action,
                    category = if (action == CollectorActions.VIEW_CONTENT) COLLECTOR_CAT_PAGE_VIEW else COLLECTOR_CAT_PROCESS,
                    userId = userId,
                    showKey = currentShow?.showKey,
                    storeId = currentShow?.id?.toString(),
                    videoKey = currentShow?.eventId,
                    showStatus = event?.status,
                    videoTime = videoTime,
                    showTitle = currentShow?.name,
                    productKey = productKey,
                    variantId = variantId
                )
            }
        }
    }
}