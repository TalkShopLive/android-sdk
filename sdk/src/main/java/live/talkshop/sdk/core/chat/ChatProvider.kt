package live.talkshop.sdk.core.chat

import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.authentication.storedClientKey
import live.talkshop.sdk.core.user.models.UserTokenModel
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Constants.ERROR_AUTH_MESSAGE
import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.HTTPMethod
import live.talkshop.sdk.utils.networking.URLs
import live.talkshop.sdk.utils.parsers.UserTokenParser

class ChatProvider {

    internal suspend fun initiateChat(
        jwt: String,
        isGuest: Boolean,
        callback: ((String?, UserTokenModel?) -> Unit)?
    ) {
        if (isAuthenticated) {
            try {
                val url = if (isGuest) URLs.URL_GUEST_TOKEN else URLs.URL_FED_TOKEN
                val headers = mutableMapOf(
                    Constants.SDK_KEY to storedClientKey,
                    Constants.AUTH_KEY to "${Constants.BEARER_KEY} $jwt"
                )

                val response = APIHandler.makeRequest(url, HTTPMethod.POST, headers = headers)
                val userTokenModel = UserTokenParser.fromJsonString(response)

                callback?.invoke(null, userTokenModel)
            } catch (e: Exception) {
                e.printStackTrace()
                callback?.invoke(e.message, null)
            }
        } else {
            callback?.invoke(ERROR_AUTH_MESSAGE, null)
        }
    }
}