package live.talkshop.sdk.utils.networking

import live.talkshop.sdk.core.authentication.currentShow
import live.talkshop.sdk.core.authentication.isAuthenticated
import live.talkshop.sdk.core.authentication.storedClientKey
import live.talkshop.sdk.core.chat.models.SenderModel
import live.talkshop.sdk.core.chat.models.UserTokenModel
import live.talkshop.sdk.core.show.models.ProductModel
import live.talkshop.sdk.core.show.models.ShowModel
import live.talkshop.sdk.core.show.models.EventModel
import live.talkshop.sdk.resources.APIClientError
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Keys
import live.talkshop.sdk.resources.Keys.KEY_SENDER
import live.talkshop.sdk.resources.URLs
import live.talkshop.sdk.resources.URLs.getMessagesUrl
import live.talkshop.sdk.resources.URLs.getMultipleProducts
import live.talkshop.sdk.resources.URLs.getShowDetailsUrl
import live.talkshop.sdk.resources.URLs.getShowStatusUrl
import live.talkshop.sdk.resources.URLs.getUserMetaUrl
import live.talkshop.sdk.utils.helpers.Either
import live.talkshop.sdk.utils.Logging
import live.talkshop.sdk.utils.parsers.ProductParser
import live.talkshop.sdk.utils.parsers.ShowParser
import live.talkshop.sdk.utils.parsers.ShowStatusParser
import live.talkshop.sdk.utils.parsers.UserTokenParser
import org.json.JSONObject

/**
 * Provides a collection of API calls related to shows, user tokens, events, and messages.
 * This object contains internal functions that handle authenticated requests and return
 * results wrapped in the `Either` type, representing success or error states.
 */
internal object APICalls {

    /**
     * Retrieves detailed information about a show using the provided show key.
     *
     * @param showKey The key associated with the desired show.
     * @return An `Either` object containing the `ShowModel` if successful, or an `APIClientError`.
     */
    suspend fun getShowDetails(showKey: String): Either<APIClientError, ShowModel> {
        return executeWithAuthCheck {
            try {
                val response =
                    APIHandler.makeRequest(getShowDetailsUrl(showKey), HTTPMethod.GET)
                if (response.statusCode !in 200..299) {
                    Either.Error(getError(APIClientError.SHOW_NOT_FOUND))
                } else {
                    val showModel = ShowParser.parseFromJson(JSONObject(response.body))
                    currentShow = showModel
                    Either.Result(showModel)
                }
            } catch (e: Exception) {
                Logging.print(APICalls::class.java, e)
                Either.Error(getError(APIClientError.SHOW_UNKNOWN_EXCEPTION))
            }
        }
    }

    /**
     * Retrieves the current event status for a show.
     *
     * @param showKey The key associated with the desired show.
     * @return An `Either` object containing the `ShowStatusModel` if successful, or an `APIClientError`.
     */
    suspend fun getCurrentEvent(showKey: String): Either<APIClientError, EventModel> {
        return executeWithAuthCheck {
            try {
                val response = APIHandler.makeRequest(
                    getShowStatusUrl(showKey),
                    HTTPMethod.GET
                )
                if (response.statusCode !in 200..299) {
                    Either.Error(getError(APIClientError.EVENT_NOT_FOUND))
                } else {
                    Either.Result(
                        try {
                            ShowStatusParser.parseFromJson(JSONObject(response.body), showKey)
                        } catch (_: Exception) {
                            EventModel()
                        }
                    )
                }
            } catch (e: Exception) {
                Logging.print(APICalls::class.java, e)
                Either.Error(getError(APIClientError.EVENT_UNKNOWN_EXCEPTION))
            }
        }
    }

    /**
     * Retrieves the products and their info for a show.
     *
     * @param showKey The key associated with the desired show.
     * @property preLive A flag indicating whether the request is related to pre products.
     * @return An `Either` object containing the `ProductModel` if successful, or an `APIClientError`.
     */
    suspend fun getShowProducts(
        showKey: String,
        preLive: Boolean,
    ): Either<APIClientError, List<ProductModel>> {
        var finalResult: Either<APIClientError, List<ProductModel>> =
            Either.Error(APIClientError.UNKNOWN_EXCEPTION)

        getShowDetails(showKey).onError {
            finalResult = Either.Error(it)
        }.onResult { show ->
            if (show.productIds.isNullOrEmpty()) {
                finalResult = Either.Error(APIClientError.NO_PRODUCTS_FOUND)
            } else {
                val productIds = if (preLive) {
                    show.entranceProductsIds
                } else {
                    show.productIds
                }
                if (productIds.isNullOrEmpty()) {
                    finalResult = Either.Error(APIClientError.NO_PRODUCTS_FOUND)
                } else {
                    try {
                        val response = APIHandler.makeRequest(
                            getMultipleProducts(show.productIds),
                            HTTPMethod.GET
                        )
                        finalResult = if (response.statusCode !in 200..299) {
                            Either.Error(getError(APIClientError.NO_PRODUCTS_FOUND))
                        } else {
                            val productModel =
                                ProductParser.parseFromJson(JSONObject(response.body))
                            Either.Result(productModel)
                        }
                    } catch (e: Exception) {
                        Logging.print(APICalls::class.java, e)
                        finalResult = Either.Error(getError(APIClientError.SHOW_UNKNOWN_EXCEPTION))
                    }
                }
            }
        }

        return finalResult
    }

    /**
     * Retrieves a user token using a JWT (JSON Web Token) and guest status.
     *
     * @param jwt The JWT for authentication.
     * @param isGuest Indicates whether the user is a guest.
     * @return An `Either` object containing the `UserTokenModel` if successful, or an `APIClientError`.
     */
    suspend fun getUserToken(
        jwt: String,
        isGuest: Boolean,
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
                    403 -> Either.Error(getError(APIClientError.PERMISSION_DENIED))
                    else -> Either.Error(getError(APIClientError.INVALID_USER_TOKEN))
                }
            }

            return Either.Result(UserTokenParser.fromJsonString(response.body)!!)
        } catch (e: Exception) {
            Logging.print(APICalls::class.java, e)
            return Either.Error(getError(APIClientError.USER_TOKEN_EXCEPTION))
        }
    }

    /**
     * Retrieves the current stream status for a show.
     *
     * @param currentShowKey The key associated with the current show.
     * @return An `Either` object containing the `ShowStatusModel` if successful, or an `APIClientError`.
     */
    suspend fun getCurrentStream(currentShowKey: String): Either<APIClientError, EventModel> {
        return executeWithAuthCheck {
            try {
                val response = APIHandler.makeRequest(
                    getShowStatusUrl(currentShowKey),
                    HTTPMethod.GET
                )

                if (response.statusCode !in 200..299) {
                    Either.Error(getError(APIClientError.CHANNEL_SUBSCRIPTION_FAILED))
                } else {
                    Logging.print(APICalls::class.java, "Channels subscribe success")
                    Either.Result(
                        ShowStatusParser.parseFromJson(
                            JSONObject(response.body),
                            currentShowKey
                        )
                    )
                }
            } catch (e: Exception) {
                Logging.print(APICalls::class.java, e)
                Either.Error(getError(APIClientError.CHANNEL_SUBSCRIPTION_FAILED))
            }
        }
    }

    /**
     * Deletes a message for a specific event based on the event ID and message timestamp.
     *
     * @param eventId The unique identifier for the event.
     * @param timeToken The timestamp of the message to be deleted.
     * @param currentJwt The current JWT for authentication.
     * @return An `Either` object containing `true` if successful, or an `APIClientError`.
     */
    suspend fun deleteMessage(
        eventId: String,
        timeToken: String,
        currentJwt: String,
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
                        Either.Error(getError(APIClientError.PERMISSION_DENIED))
                    }

                    else -> {
                        Logging.print(APICalls::class.java, response.body)
                        Either.Error(getError(APIClientError.UNKNOWN_EXCEPTION))
                    }
                }
            } catch (e: Exception) {
                Logging.print(APICalls::class.java, e)
                Either.Error(getError(APIClientError.UNKNOWN_EXCEPTION))
            }
        }
    }

    /**
     * Retrieves user metadata using a unique identifier (UUID).
     *
     * @param uuid The unique identifier for the user.
     * @return An `Either` object containing the `SenderModel` if successful, or an `APIClientError`.
     */
    suspend fun getUserMeta(uuid: String): Either<APIClientError, SenderModel> {
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
                        Either.Error(getError(APIClientError.PERMISSION_DENIED))
                    } else {
                        Either.Error(getError(APIClientError.UNKNOWN_EXCEPTION))
                    }
                }
            } catch (e: Exception) {
                Logging.print(APICalls::class.java, e)
                Either.Error(getError(APIClientError.UNKNOWN_EXCEPTION))
            }
        }
    }

    /**
     * Deletes an action for a specific message based on the event ID and message timestamp.
     *
     * @param eventId The unique identifier for the event.
     * @param timeToken The timestamp of the message.
     * @param actionTimeToken The timestamp of the action to be deleted.
     * @param currentJwt The current JWT for authentication.
     * @return An `Either` object containing `true` if successful, or an `APIClientError`.
     */
    suspend fun deleteAction(
        eventId: String,
        timeToken: String,
        actionTimeToken: String,
        currentJwt: String,
    ): Either<APIClientError, Boolean> {
        return executeWithAuthCheck {
            try {
                val response = APIHandler.makeRequest(
                    requestUrl = getMessagesUrl(eventId, timeToken, actionTimeToken),
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
                        Either.Error(getError(APIClientError.PERMISSION_DENIED))
                    }

                    else -> {
                        Logging.print(APICalls::class.java, response.body)
                        Either.Error(getError(APIClientError.UNKNOWN_EXCEPTION))
                    }
                }
            } catch (e: Exception) {
                Logging.print(APICalls::class.java, e)
                Either.Error(getError(APIClientError.UNKNOWN_EXCEPTION))
            }
        }
    }

    /**
     * Increments the view count for a specific event.
     *
     * @param eventId The unique identifier for the event.
     */
    suspend fun incrementView(eventId: String) {
        try {
            APIHandler.makeRequest(URLs.getIncrementViewUrl(eventId), HTTPMethod.POST)
        } catch (e: Exception) {
            Logging.print(APICalls::class.java, e)
        }
    }

    /**
     * Executes the provided authenticated API call, checking for authentication first.
     *
     * @param call The suspend function representing the API call.
     * @return An `Either` object containing the result if successful, or an `APIClientError`.
     */
    private suspend fun <T> executeWithAuthCheck(call: suspend () -> Either<APIClientError, T>): Either<APIClientError, T> {
        if (!isAuthenticated) {
            return Either.Error(getError(APIClientError.AUTHENTICATION_FAILED))
        }
        return call()
    }

    /**
     * Retrieves a specific `APIClientError` instance, optionally associating it with the class name.
     *
     * @param error The error to be returned.
     * @return The `APIClientError` instance.
     */
    private fun getError(error: APIClientError): APIClientError {
        return error.from(APICalls::class.java.name)
    }
}