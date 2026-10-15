package live.talkshop.sdk.utils

import android.content.res.Resources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.authentication.currentShow
import live.talkshop.sdk.core.authentication.isDNT
import live.talkshop.sdk.core.show.models.EventModel
import live.talkshop.sdk.resources.CollectorActions
import live.talkshop.sdk.resources.Constants.COLLECTOR_CAT_INTERACTION
import live.talkshop.sdk.resources.Constants.COLLECTOR_CAT_PAGE_VIEW
import live.talkshop.sdk.resources.Constants.COLLECTOR_CAT_PROCESS
import live.talkshop.sdk.resources.Constants.SDK_VERSION
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
        showId: Int? = null,
        storeId: String? = "NOT_SET",
        showStatus: String? = "NOT_SET",
        videoTime: Int? = null,
        showTitle: String? = "NOT_SET",
        productKey: String? = "NOT_SET",
        variantId: Int? = null,
        productId: Int? = null,
        productOwningChannelId: String? = "NOT_SET",
    ) {
        val timestamp = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("timestamp_utc", timestamp)
            put("user_id", userId)
            put("category", category)
            put("version", SDK_VERSION)
            put("action", action)
            put("application", "android")
            put("meta", JSONObject().apply {
                put("external", true)
                put("streaming_content_key", showKey)
                put("store_id", storeId)
                put("video_status", showStatus)
                put("video_time", videoTime)
                put("show_id", showId)
                put("variant_id", variantId)
                put("product_key", productKey)
                put("product_id", productId)
                put("product_owning_channel_id", productOwningChannelId)
            })
            put("page_metrics", pageMetrics(showKey, showTitle))
            put("aspect", aspect())
        }

        try {
            APIHandler.makeRequest(
                requestUrl = getCollectorUrl(), requestMethod = HTTPMethod.POST, payload = payload
            )
            Logging.print(Collector::class.java, "Collector-$action: Analytics Succeeded")
        } catch (e: Exception) {
            Logging.print(
                Collector::class.java,
                "Collector-$action: Analytics Failed with error: $e"
            )
        }
    }

    companion object {
        @Volatile
        private var instance: Collector? = null

        private fun pageMetrics(showKey: String?, showTitle: String?) = JSONObject().apply {
            put("origin", URL_PUBLISH_PROD)
            put("host", URL_PUBLISH_PROD.removePrefix("https://"))
            put("referrer", URL_COLLECTOR_WALMART)
            put("page_url", getCollectorWatchUrl(showKey))
            put("page_url_raw", getCollectorWatchUrl(showKey))
            put("page_title", showTitle)
        }

        private fun aspect() = JSONObject().apply {
            val display = Resources.getSystem().displayMetrics
            put("screen_resolution", "${display.heightPixels} x ${display.widthPixels}")
        }

        /**
         * Builds a VIDEO_VIEW / VIDEO_ENGAGED_VIEW payload.
         * Anonymous (DNT): whitelisted meta only; no user_id, page_metrics or aspect.
         * Null values are dropped by [JSONObject.put], so a missing event_id is not sent.
         */
        private fun videoViewPayload(
            action: CollectorActions,
            viewRule: String,
            showId: Int,
            showKey: String,
            eventId: Int?,
            storeId: Int?,
            videoStatus: String,
            showTitle: String?,
            userId: String,
            anonymous: Boolean,
        ) = JSONObject().apply {
            put("timestamp_utc", System.currentTimeMillis())
            if (!anonymous) {
                put("user_id", userId)
            }
            put("category", COLLECTOR_CAT_PROCESS)
            put("version", SDK_VERSION)
            put("action", action)
            put("application", "android")
            put("meta", JSONObject().apply {
                put("external", true)
                put("streaming_content_key", showKey)
                put("store_id", storeId)
                put("video_status", videoStatus)
                put("show_id", showId)
                put("event_id", eventId)
                put("view_rule", viewRule)
                if (anonymous) {
                    put("anonymous", true)
                }
            })
            if (!anonymous) {
                put("page_metrics", pageMetrics(showKey, showTitle))
                put("aspect", aspect())
            }
        }

        /**
         * Sends a video view event for [live.talkshop.sdk.core.videoviews.VideoViewTracker].
         * Unlike [collect], it is not blocked by DNT: with [anonymous] the event is sent anonymously.
         */
        fun collectVideoView(
            action: CollectorActions,
            viewRule: String,
            showId: Int,
            showKey: String,
            eventId: Int?,
            storeId: Int?,
            videoStatus: String,
            showTitle: String?,
            userId: String,
            anonymous: Boolean,
        ) {
            if (instance == null) return
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    APIHandler.makeRequest(
                        requestUrl = getCollectorUrl(),
                        requestMethod = HTTPMethod.POST,
                        payload = videoViewPayload(
                            action, viewRule, showId, showKey, eventId, storeId,
                            videoStatus, showTitle, userId, anonymous
                        )
                    )
                } catch (e: Exception) {
                    // A failed view event is dropped, like any other collector event.
                }
            }
        }

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
            videoTime: Int? = null,
            productKey: String? = null,
            variantId: Int? = null,
            productId: Int? = null,
        ) {
            if (isDNT) {
                return
            }
            CoroutineScope(Dispatchers.IO).launch {
                getInstance().collect(
                    action = action,
                    category = when (action) {
                        CollectorActions.VIEW_CONTENT -> {
                            COLLECTOR_CAT_PAGE_VIEW
                        }

                        CollectorActions.VIDEO_PLAY,
                        CollectorActions.CUSTOMIZE_PRODUCT_QUANTITY_DECREASE,
                        CollectorActions.ADD_TO_CART,
                        CollectorActions.CUSTOMIZE_PRODUCT_QUANTITY_INCREASE,
                        CollectorActions.VIDEO_PAUSE,
                        CollectorActions.SELECT_PRODUCT,
                            -> {
                            COLLECTOR_CAT_INTERACTION
                        }

                        else -> {
                            COLLECTOR_CAT_PROCESS
                        }
                    },
                    userId = userId,
                    showKey = currentShow?.showKey,
                    showId = currentShow?.id,
                    storeId = currentShow?.storeId,
                    showStatus = event?.status,
                    videoTime = videoTime,
                    showTitle = currentShow?.name,
                    productKey = productKey,
                    variantId = variantId,
                    productId = productId,
                    productOwningChannelId = currentShow?.storeId
                )
            }
        }
    }
}