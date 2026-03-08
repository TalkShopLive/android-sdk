package live.talkshop.sdk.core.chat

import live.talkshop.sdk.core.show.models.ShowType

internal enum class ChatVersion {
    V1,
    V2
}

internal object ChatVersionProvider {
    fun getVersion(showType: ShowType, isGuest: Boolean): ChatVersion {
        if (isGuest) return ChatVersion.V1
        return if (showType == ShowType.LEGACY) ChatVersion.V1 else ChatVersion.V2
    }
}