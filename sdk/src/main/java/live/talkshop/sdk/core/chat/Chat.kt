package live.talkshop.sdk.core.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.user.models.UserTokenModel

class Chat(private val jwt: String, private val isGuest: Boolean) {
    constructor(
        jwt: String,
        isGuest: Boolean,
        callback: ((String?, UserTokenModel?) -> Unit)?
    ) : this(jwt, isGuest) {
        CoroutineScope(Dispatchers.IO).launch {
            provider.initiateChat(jwt, isGuest, callback)
        }
    }

    companion object {
        private val provider = ChatProvider()
    }
}