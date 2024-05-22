package live.talkshop.sdk.utils.networking

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.talkshop.sdk.utils.Logging
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Represents HTTP request methods.
 */
enum class HTTPMethod(val value: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE")
}

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
        requestMethod: HTTPMethod,
        payload: JSONObject? = null,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): APIResponse = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(requestUrl)
            connection = connectionFactory.create(url)
            connection.apply {
                this.requestMethod = requestMethod.value
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                doInput = true
                doOutput =
                    (requestMethod == HTTPMethod.POST || requestMethod == HTTPMethod.PUT || (requestMethod == HTTPMethod.DELETE && body != null))
                connectTimeout = 15000
                readTimeout = 15000

                headers.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }
            }

            val requestBody = body ?: payload?.toString()

            if (requestBody != null && (requestMethod == HTTPMethod.POST || requestMethod == HTTPMethod.PUT || requestMethod == HTTPMethod.DELETE)) {
                DataOutputStream(connection.outputStream).use { os ->
                    os.write(requestBody.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
            }

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
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
                    reader.readText()
                }
            }

            if (responseCode !in 200..299) {
                Logging.print(
                    APIHandler::class.java,
                    "HTTP request failed with status code $responseCode: $responseText"
                )
            }

            return@withContext APIResponse(responseText, responseCode)
        } catch (e: Exception) {
            Logging.print(APIHandler::class.java, e)
            throw IOException("Failed to make HTTP request: ${e.message}", e)
        } finally {
            connection?.disconnect()
        }
    }
}