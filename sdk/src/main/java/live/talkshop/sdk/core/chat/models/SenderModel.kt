package live.talkshop.sdk.core.chat.models

data class SenderModel(
    val profileUrl: String? = null,
    val name: String? = null,
    val channelCode: String? = null,
    val isVerified: Boolean = false,
    val id: String? = null
)