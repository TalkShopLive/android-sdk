package live.talkshop.sdk.utils.networking

import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.authentication.storedClientKey
import live.talkshop.sdk.core.chat.models.SenderModel
import live.talkshop.sdk.core.chat.models.UserTokenModel
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.core.show.models.ShowStatusModel
import live.talkshop.sdk.resources.APIClientError
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Keys
import live.talkshop.sdk.resources.Keys.KEY_SENDER
import live.talkshop.sdk.resources.URLs
import live.talkshop.sdk.resources.URLs.getCurrentStreamUrl
import live.talkshop.sdk.resources.URLs.getMessagesUrl
import live.talkshop.sdk.resources.URLs.getUserMetaUrl
import live.talkshop.sdk.utils.helpers.Either
import live.talkshop.sdk.utils.Logging
import live.talkshop.sdk.utils.parsers.ShowParser
import live.talkshop.sdk.utils.parsers.ShowStatusParser
import live.talkshop.sdk.utils.parsers.UserTokenParser
import org.json.JSONObject

internal object APICalls {
    internal suspend fun getShowDetails(showKey: String): Either<APIClientError, ShowModel> {
        return executeWithAuthCheck {
            try {
                val response =
                    APIHandler.makeRequest(URLs.getShowDetailsUrl(showKey), HTTPMethod.GET)
                if (response.statusCode !in 200..299) {
                    Either.Error(APIClientError.SHOW_NOT_FOUND)
                } else {
                    val showModel = ShowParser.parseFromJson(JSONObject(response.body))
                    Either.Result(showModel)
                }
            } catch (e: Exception) {
                Logging.print(e)
                Either.Error(APIClientError.SHOW_UNKNOWN_EXCEPTION)
            }
        }
    }

    internal suspend fun getCurrentEvent(showKey: String): Either<APIClientError, ShowStatusModel> {
        return executeWithAuthCheck {
            try {
                val response = APIHandler.makeRequest(
                    getCurrentStreamUrl(showKey),
                    HTTPMethod.GET
                )
                if (response.statusCode !in 200..299) {
                    Either.Error(APIClientError.EVENT_NOT_FOUND)
                } else {
                    Either.Result(
                        try {
                            ShowStatusParser.parseFromJson(JSONObject(response.body))
                        } catch (e: Exception) {
                            ShowStatusModel()
                        }
                    )
                }
            } catch (e: Exception) {
                Logging.print(e)
                Either.Error(APIClientError.EVENT_UNKNOWN_EXCEPTION)
            }
        }
    }

    internal suspend fun getUserToken(
        jwt: String,
        isGuest: Boolean
    ): Either<APIClientError, UserTokenModel> {
        try {
            val response = APIHandler.makeRequest(
                URLs.getUserTokenUrl(isGuest),
                HTTPMethod.POST,
                headers = mutableMapOf(
                    Constants.SDK_KEY to storedClientKey,
                    Constants.AUTH_KEY to "${Constants.BEARER_KEY} $jwt"
                )
            )

            if (response.statusCode !in 200..299) {
                return when (response.statusCode) {
                    403 -> Either.Error(APIClientError.PERMISSION_DENIED)
                    else -> Either.Error(APIClientError.INVALID_USER_TOKEN)
                }
            }

            return Either.Result(UserTokenParser.fromJsonString(response.body)!!)
        } catch (e: Exception) {
            Logging.print(e)
            return Either.Error(APIClientError.USER_TOKEN_EXCEPTION)
        }
    }

    internal suspend fun getCurrentStream(currentShowKey: String): Either<APIClientError, ShowStatusModel> {
        return executeWithAuthCheck {
            try {
                val response = APIHandler.makeRequest(
                    getCurrentStreamUrl(currentShowKey),
                    HTTPMethod.GET
                )

                if (response.statusCode !in 200..299) {
                    Either.Error(APIClientError.CHANNEL_SUBSCRIPTION_FAILED)
                } else {
                    Logging.print("Channels subscribe success")
                    Either.Result(ShowStatusParser.parseFromJson(JSONObject(response.body)))
                }
            } catch (e: Exception) {
                Logging.print(e)
                Either.Error(APIClientError.CHANNEL_SUBSCRIPTION_FAILED)
            }
        }
    }

    internal suspend fun deleteMessage(
        eventId: String,
        timeToken: String,
        currentJwt: String
    ): Either<APIClientError, Boolean> {
        return executeWithAuthCheck {
            try {
                val response = APIHandler.makeRequest(
                    requestUrl = getMessagesUrl(eventId, timeToken),
                    requestMethod = HTTPMethod.DELETE,
                    headers = mutableMapOf(
                        Constants.SDK_KEY to storedClientKey,
                        Constants.AUTH_KEY to "${Constants.BEARER_KEY} $currentJwt"
                    )
                )

                when (response.statusCode) {
                    in 200..299 -> {
                        Either.Result(true)
                    }

                    403 -> {
                        Either.Error(APIClientError.PERMISSION_DENIED)
                    }

                    else -> {
                        Logging.print(response.body)
                        Either.Error(APIClientError.UNKNOWN_EXCEPTION)
                    }
                }
            } catch (e: Exception) {
                Logging.print(e)
                Either.Error(APIClientError.UNKNOWN_EXCEPTION)
            }
        }
    }

    internal suspend fun getUserMeta(uuid: String): Either<APIClientError, SenderModel> {
        return executeWithAuthCheck {
            try {
                val response = APIHandler.makeRequest(getUserMetaUrl(uuid), HTTPMethod.GET)
                if (response.statusCode in 200..299) {
                    val jsonObject = JSONObject(response.body)
                    val sender = jsonObject.getJSONObject(KEY_SENDER)
                    Either.Result(
                        SenderModel(
                            id = sender.getString(Keys.KEY_ID),
                            name = sender.getString(Keys.KEY_NAME),
                            profileUrl = sender.getString(Keys.KEY_PROFILE_URL)
                        )
                    )
                } else {
                    if (response.statusCode == 403) {
                        Either.Error(APIClientError.PERMISSION_DENIED)
                    } else {
                        Either.Error(APIClientError.UNKNOWN_EXCEPTION)
                    }
                }
            } catch (e: Exception) {
                Logging.print(e)
                Either.Error(APIClientError.UNKNOWN_EXCEPTION)
            }
        }
    }

    internal suspend fun incrementView(eventId: String) {
        try {
            APIHandler.makeRequest(URLs.getIncrementViewUrl(eventId), HTTPMethod.POST)
        } catch (e: Exception) {
            Logging.print(e)
        }
    }

    private suspend fun <T> executeWithAuthCheck(call: suspend () -> Either<APIClientError, T>): Either<APIClientError, T> {
        if (!isAuthenticated) {
            return Either.Error(APIClientError.AUTHENTICATION_FAILED)
        }
        return call()
    }
}