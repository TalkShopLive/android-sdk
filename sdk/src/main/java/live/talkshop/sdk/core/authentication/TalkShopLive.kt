package live.talkshop.sdk.core.authentication

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import live.talkshop.sdk.core.user.models.UserTokenModel
import live.talkshop.sdk.resources.Constants
import live.talkshop.sdk.resources.Constants.SDK_KEY
import live.talkshop.sdk.resources.Constants.KEY_AUTHENTICATED
import live.talkshop.sdk.resources.Constants.KEY_VALID
import live.talkshop.sdk.resources.Constants.SHARED_PREFS_NAME
import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.HTTPMethod
import live.talkshop.sdk.utils.networking.URLs
import live.talkshop.sdk.utils.networking.URLs.URL_AUTH_ENDPOINT
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Class responsible for managing TalkShopLive authentication and initialization.
 *
 * This class provides methods to initialize TalkShopLive, authenticate the user,
 * and manage authentication status.
 *
 * @property context The application context.
 * @constructor Creates an instance of TalkShopLive with the given application context.
 */
class TalkShopLive private constructor(private val context: Context) {

    companion object {
        private var instance: WeakReference<TalkShopLive>? = null

        /**
         * Initializes TalkShopLive with the provided parameters. The callback is optional.
         *
         * @param context The application context.
         * @param clientKey The client key for authentication.
         * @param debugMode Indicates whether debug mode is enabled.
         * @param testMode Indicates whether test mode is enabled.
         * @param dnt Indicates whether Do Not Track (DNT) is enabled.
         * @param callback An optional callback function to be invoked upon initialization completion.
         */
        fun initialize(
            context: Context,
            clientKey: String,
            debugMode: Boolean = false,
            testMode: Boolean = false,
            dnt: Boolean = false,
            callback: ((Boolean) -> Unit)? = null // Make callback optional
        ) {
            getInstance(context).initializeInternal(clientKey, debugMode, testMode, dnt, callback)
        }

        /**
         * Initiates chat functionality with an optional callback that returns an error message if the operation fails.
         *
         * @param context The application context.
         * @param jwt The JWT token for authentication.
         * @param isGuest Indicates whether the user is a guest.
         * @param callback An optional callback function to be invoked upon completion.
         */
        fun Chat(
            context: Context,
            jwt: String,
            isGuest: Boolean,
            callback: ((String?, UserTokenModel?) -> Unit)? = null
        ) {
            val talkShopLive = getInstance(context)
            if (isAuthenticated) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = talkShopLive.fetchUserToken(jwt, isGuest)
                        val userTokenModel = UserTokenModel.fromJsonString(response)
                        callback?.invoke(null, userTokenModel)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback?.invoke(
                            e.message ?: "An error occurred during the chat initialization.", null
                        )
                    }
                }
            } else {
                callback?.invoke(
                    "Authentication invalid", null
                )
            }
        }

        private fun getInstance(context: Context): TalkShopLive {
            return instance?.get() ?: synchronized(this) {
                val newInstance = TalkShopLive(context.applicationContext)
                instance = WeakReference(newInstance)
                newInstance
            }
        }
    }

    private val sharedPreferences =
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    private var debugMode: Boolean = false
    private var testMode: Boolean = false
    private var dnt: Boolean = false

    @OptIn(DelicateCoroutinesApi::class)
    private fun initializeInternal(
        clientKey: String,
        debugMode: Boolean,
        testMode: Boolean,
        dnt: Boolean,
        callback: ((Boolean) -> Unit)?
    ) {
        this.debugMode = debugMode
        this.testMode = testMode
        this.dnt = dnt

        // Launch a coroutine to perform the initialization
        GlobalScope.launch(Dispatchers.IO) {
            val success = authenticateWithAPI(clientKey)
            isAuthenticated = success
            storedClientKey = clientKey
            callback?.invoke(success)
        }
    }

    private suspend fun authenticateWithAPI(clientKey: String): Boolean {
        val headers = mapOf(SDK_KEY to clientKey)

        return try {
            val response =
                APIHandler.makeRequest(URL_AUTH_ENDPOINT, HTTPMethod.GET, headers = headers)
            val jsonResponse = JSONObject(response)
            val validKey = jsonResponse.optBoolean(KEY_VALID, false)

            setAuthenticated(validKey)
            validKey
        } catch (e: Exception) {
            e.printStackTrace()
            setAuthenticated(false)
            false
        }
    }

    /**
     * Sets the authentication status in shared preferences.
     *
     * @param authenticated The authentication status to be set.
     */
    private fun setAuthenticated(authenticated: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTHENTICATED, authenticated).apply()
    }

    /**
     * Fetches a user token from the server.
     *
     * This method asynchronously retrieves a token for either a federated user or a guest, based on the provided parameters. It constructs the appropriate API request, handles the response, and returns the raw JSON string containing token information.
     *
     * @param jwt The JWT token for authentication (required for federated users).
     * @param isGuest A boolean flag indicating whether the token request is for a guest (true) or a federated user (false).
     * @return A JSON string containing the token response from the server. The response structure varies depending on the user type (federated or guest).
     * @throws Exception if there's an error during the API request or response handling.
     */
    private suspend fun fetchUserToken(jwt: String, isGuest: Boolean): String {
        val url = if (isGuest) URLs.URL_GUEST_TOKEN else URLs.URL_FED_TOKEN
        val headers = mutableMapOf(
            SDK_KEY to storedClientKey,
            Constants.AUTH_KEY to "${Constants.BEARER_KEY} $jwt"
        )
        return APIHandler.makeRequest(url, HTTPMethod.POST, headers = headers)
    }
}