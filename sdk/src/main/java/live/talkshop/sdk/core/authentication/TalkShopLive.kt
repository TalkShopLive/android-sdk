package live.talkshop.sdk.core.authentication

import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import live.talkshop.sdk.utils.Logging
import live.talkshop.sdk.resources.Constants.COLLECTOR_ACTION_SDK_INITIALIZED
import live.talkshop.sdk.resources.Constants.COLLECTOR_CAT_INTERACTION
import live.talkshop.sdk.resources.Constants.SDK_KEY
import live.talkshop.sdk.resources.Constants.KEY_AUTHENTICATED
import live.talkshop.sdk.resources.Constants.SHARED_PREFS_NAME
import live.talkshop.sdk.resources.APIClientError.AUTHENTICATION_EXCEPTION
import live.talkshop.sdk.resources.APIClientError.AUTHENTICATION_FAILED
import live.talkshop.sdk.resources.Keys.KEY_VALID_KEY
import live.talkshop.sdk.utils.Collector
import live.talkshop.sdk.utils.networking.APIHandler
import live.talkshop.sdk.utils.networking.HTTPMethod
import live.talkshop.sdk.resources.URLs.getAuthUrl
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
            callback: ((Boolean) -> Unit)? = null
        ) {
            Collector.initialize(context)
            getInstance(context).initializeInternal(clientKey, debugMode, testMode, dnt, callback)
            Collector.collect(
                action = COLLECTOR_ACTION_SDK_INITIALIZED,
                category = COLLECTOR_CAT_INTERACTION
            )
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun initializeInternal(
        clientKey: String,
        debugMode: Boolean,
        testMode: Boolean,
        dnt: Boolean,
        callback: ((Boolean) -> Unit)?
    ) {
        isDebugMode = debugMode
        isTestMode = testMode
        isDNT = dnt

        GlobalScope.launch(Dispatchers.IO) {
            val success = authenticateWithAPI(clientKey)
            isAuthenticated = success
            storedClientKey = clientKey
            callback?.invoke(success)
        }
    }

    private suspend fun authenticateWithAPI(clientKey: String): Boolean {

        val headers = mutableMapOf(SDK_KEY to clientKey)

        return try {
            val response =
                APIHandler.makeRequest(
                    requestUrl = getAuthUrl(),
                    requestMethod = HTTPMethod.GET,
                    headers = headers
                )

            if (response.statusCode !in 200..299) {
                Logging.print(AUTHENTICATION_FAILED)
            }

            val jsonResponse = JSONObject(response.body)
            val validKey = jsonResponse.optBoolean(KEY_VALID_KEY, false)

            setAuthenticated(validKey)
            validKey
        } catch (e: Exception) {
            Logging.print(AUTHENTICATION_EXCEPTION, e)
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
}