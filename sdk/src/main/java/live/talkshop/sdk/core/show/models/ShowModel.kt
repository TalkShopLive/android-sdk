package live.talkshop.sdk.core.show.models

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Represents the model for a show, encapsulating various details and metadata about it.
 *
 * @property id The unique identifier of the show.
 * @property showKey The key associated with the show.
 * @property name The name of the show.
 * @property description A description of the show.
 * @property status The current status of the show.
 * @property hlsPlaybackUrl The HLS playback URL for the show.
 * @property hlsUrl The HLS URL for the show.
 * @property trailerUrl The URL for the show's trailer.
 * @property airDate The air date of the show.
 * @property endedAt The end date and time of the show.
 * @property eventId The event ID associated with the show.
 * @property cc Closed captioning information or URL.
 * @property duration The duration of the show in minutes.
 * @property trailerDuration The duration of the trailer in minutes.
 * @property videoThumbnailUrl The url of the show's thumbnail.
 * @property channelLogo The logo of the channel.
 * @property channelName The name of the channel.
 */
data class ShowModel(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("showKey")
    val showKey: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("hlsPlaybackUrl")
    val hlsPlaybackUrl: String?,

    @SerializedName("hlsUrl")
    val hlsUrl: String?,

    @SerializedName("trailerUrl")
    val trailerUrl: String?,

    @SerializedName("airDate")
    val airDate: String?,

    @SerializedName("endedAt")
    val endedAt: Date?,

    @SerializedName("eventId")
    val eventId: String?,

    @SerializedName("cc")
    val cc: String?,

    @SerializedName("duration")
    val duration: Int?,

    @SerializedName("trailerDuration")
    val trailerDuration: Int?,

    @SerializedName("videoThumbnailUrl")
    val videoThumbnailUrl: String,

    @SerializedName("channelLogo")
    val channelLogo: String,

    @SerializedName("channelName")
    val channelName: String,

    @SerializedName("productIds")
    val productIds: List<Int>? = null
)