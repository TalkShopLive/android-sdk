package live.talkshop.sdk.core.show.models

import com.google.gson.annotations.SerializedName

/**
 * Represents the status model of a show, encapsulating various details related to the stream.
 *
 * @property showKey The unique identifier for the show.
 * @property status The current status of the show.
 * @property hlsPlaybackUrl The URL for HLS playback of the show.
 * @property hlsUrl The HLS URL for the show stream. It can be null if not available.
 * @property trailerUrl The URL for the show's trailer.
 * @property eventId The unique identifier for the event. It can be null if the event ID is not available.
 * @property duration The duration of the show in seconds. It can be null if the duration is not specified.
 */
data class EventModel(
    @SerializedName("showKey")
    val showKey: String? = null,

    @SerializedName("status")
    val status: String? = "created",

    @SerializedName("hlsPlaybackUrl")
    val hlsPlaybackUrl: String? = null,

    @SerializedName("hlsUrl")
    val hlsUrl: String? = null,

    @SerializedName("trailerUrl")
    val trailerUrl: String? = null,

    @SerializedName("eventId")
    val eventId: Int? = 0,

    @SerializedName("duration")
    val duration: Int? = 0,
)