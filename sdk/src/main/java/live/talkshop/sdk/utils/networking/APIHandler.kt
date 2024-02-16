package live.talkshop.sdk.utils.networking

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * A singleton object that handles HTTP requests (GET, POST, PUT, DELETE) using `HttpURLConnection`.
 * It supports adding custom headers and sending JSON payloads.
 */
object APIHandler {
    /**
     * The factory used for creating [HttpURLConnection] instances.
     * Defaults to [DefaultHttpURLConnectionFactory] but can be replaced,
     * particularly useful in testing environments to use mock connections.
     */
    var connectionFactory: HttpURLConnectionFactory = DefaultHttpURLConnectionFactory()

    /**
     * Makes an HTTP request and returns the response as a String.
     *
     * @param requestUrl The URL to which the request is made.
     * @param requestMethod The HTTP method to use (e.g., "GET", "POST", "PUT", "DELETE").
     * @param payload An optional JSON object containing data to send with the request. Relevant for POST and PUT requests.
     * @param headers A map of headers to include in the request.
     * @return A String containing the response from the server. In case of an error, it returns a descriptive error message.
     * @throws Exception if an error occurs during the request.
     */
    suspend fun makeRequest(
        requestUrl: String,
        requestMethod: String,
        payload: JSONObject? = null,
        headers: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        // Ensures the network call is made in a background thread.
        var connection: HttpURLConnection? = null
        try {
            val url = URL(requestUrl)
            connection = connectionFactory.create(url)
            connection.apply {
                this.requestMethod = requestMethod
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                doInput = true
                doOutput = requestMethod == "POST" || requestMethod == "PUT"
                connectTimeout = 15000 // Sets the timeout for connecting to the URL (15 seconds).
                readTimeout = 15000 // Sets the timeout for reading the response (15 seconds).

                // Applies each custom header from the map to the connection.
                headers.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }
            }

            // Sends the JSON payload if present (for POST and PUT requests).
            payload?.let {
                DataOutputStream(connection.outputStream).use { os ->
                    os.write(it.toString().toByteArray(StandardCharsets.UTF_8))
                    os.flush()
                }
            }

            // Reads the response from the server and returns it as a String.
            return@withContext if (connection.responseCode in 200..299) {
                BufferedReader(
                    InputStreamReader(
                        connection.inputStream,
                        StandardCharsets.UTF_8
                    )
                ).use { reader ->
                    reader.readText()
                }
            } else {
                BufferedReader(
                    InputStreamReader(
                        connection.errorStream,
                        StandardCharsets.UTF_8
                    )
                ).use { reader ->
                    "Error: ${reader.readText()}"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        } finally {
            connection?.disconnect()
        }
    }
}