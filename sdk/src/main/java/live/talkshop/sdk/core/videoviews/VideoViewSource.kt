package live.talkshop.sdk.core.videoviews

import live.talkshop.sdk.core.show.models.EventModel
import live.talkshop.sdk.core.show.models.ShowModel
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/**
 * The show and stream currently loaded in the host app's player.
 *
 * Pass it to [VideoViewTracker.onSourceChanged] when the player loads a stream,
 * and again every time the show status is refreshed.
 *
 * @property showId Sent as `meta.show_id`.
 * @property showKey Sent as `meta.streaming_content_key`.
 * @property eventId The live asset id, sent as `meta.event_id`. Null when the show has no live asset.
 * @property storeId The channel id, sent as `meta.store_id`.
 * @property videoStatus Sent as `meta.video_status`.
 * @property playbackUrl Identifies the stream; never sent.
 * @property title Sent as `page_metrics.page_title`.
 */
class VideoViewSource internal constructor(
    val showId: Int,
    val showKey: String,
    val eventId: Int?,
    val storeId: Int?,
    val videoStatus: VideoStatus,
    val playbackUrl: String?,
    val title: String?,
) {

    /**
     * The show phase reported as `meta.video_status`.
     */
    enum class VideoStatus(val rawValue: String) {
        PRELIVE("prelive"),
        LIVE("live"),
        VOD("vod");

        companion object {
            /**
             * Maps a raw show state to a reportable status:
             * `prelive` / `draft` -> [PRELIVE], `live` / `transcoding` -> [LIVE], `vod` -> [VOD].
             * Returns null for any other value.
             */
            fun normalize(raw: String?): VideoStatus? = when (raw?.lowercase(Locale.ROOT)) {
                "prelive", "draft" -> PRELIVE
                "live", "transcoding" -> LIVE
                "vod" -> VOD
                else -> null
            }
        }
    }

    /**
     * Whether [other] belongs to the same play: same show, same phase and same stream.
     *
     * Streams are compared by scheme, host and path, so query tokens do not matter.
     * A missing URL on either side matches any URL.
     */
    internal fun isSamePlay(other: VideoViewSource): Boolean {
        if (showId != other.showId || videoStatus != other.videoStatus) {
            return false
        }
        val lhs = playbackUrl ?: return true
        val rhs = other.playbackUrl ?: return true
        return streamIdentity(lhs) == streamIdentity(rhs)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VideoViewSource) return false
        return showId == other.showId &&
                showKey == other.showKey &&
                eventId == other.eventId &&
                storeId == other.storeId &&
                videoStatus == other.videoStatus &&
                playbackUrl == other.playbackUrl &&
                title == other.title
    }

    override fun hashCode(): Int {
        var result = showId
        result = 31 * result + showKey.hashCode()
        result = 31 * result + (eventId ?: 0)
        result = 31 * result + (storeId ?: 0)
        result = 31 * result + videoStatus.hashCode()
        result = 31 * result + (playbackUrl?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "VideoViewSource(showId=$showId, showKey=$showKey, eventId=$eventId, storeId=$storeId, " +
                "videoStatus=${videoStatus.rawValue}, playbackUrl=$playbackUrl, title=$title)"

    companion object {

        /**
         * Creates a source from the show details and, optionally, the latest show status.
         *
         * Returns null when the show has no id or key, or its status is not prelive / live / vod.
         * The stream is chosen the same way as on web: prelive -> [ShowModel.trailerUrl],
         * live -> [ShowModel.hlsPlaybackUrl], vod -> [ShowModel.hlsUrl].
         *
         * @param show The show details from `Show.getDetails`.
         * @param event The latest status from `Show.getStatus`. Its state wins over `show.status`.
         */
        fun from(show: ShowModel, event: EventModel? = null): VideoViewSource? {
            val showId = show.id?.takeIf { it > 0 } ?: return null
            val showKey = show.showKey.presentOrNull() ?: return null
            // A status that failed to parse comes back without a show key: treat it as no status.
            val eventStatus = event?.takeIf { it.showKey != null }?.status.presentOrNull()
            val videoStatus = VideoStatus.normalize(eventStatus ?: show.status.presentOrNull())
                ?: return null

            val playbackUrl = when (videoStatus) {
                VideoStatus.PRELIVE -> show.trailerUrl
                VideoStatus.LIVE -> show.hlsPlaybackUrl
                VideoStatus.VOD -> show.hlsUrl
            }.presentOrNull()

            return VideoViewSource(
                showId = showId,
                showKey = showKey,
                eventId = show.liveAssetId,
                storeId = show.storeId.toIntOrNull(),
                videoStatus = videoStatus,
                playbackUrl = playbackUrl,
                title = show.name.presentOrNull()
            )
        }

        /** Empty strings and the literal "null" (from `optString`) mean "not set". */
        private fun String?.presentOrNull(): String? = this?.takeIf { it.isNotEmpty() && it != "null" }

        private fun streamIdentity(url: String): String = try {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase(Locale.ROOT).orEmpty()
            val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
            "$scheme://$host${uri.path.orEmpty()}"
        } catch (e: URISyntaxException) {
            url.substringBefore('#').substringBefore('?')
        }
    }
}
