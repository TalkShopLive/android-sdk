package live.talkshop.sdk.utils.networking

import java.net.HttpURLConnection
import java.net.URL

/**
 * Defines a factory interface for creating instances of [HttpURLConnection].
 *
 * This interface allows for abstraction of the process of creating HTTP connections,
 * making the network communication components of an application more flexible and testable.
 * By using this factory interface, the application can easily switch between different
 * implementations of HTTP connection creation, which is particularly useful for testing,
 * where mock or fake connections might be needed.
 */
internal interface HttpURLConnectionFactory {
    /**
     * Creates and returns a new instance of [HttpURLConnection].
     *
     * This method allows for the dynamic creation of [HttpURLConnection] instances
     * based on the provided [URL], enabling the encapsulation of connection initialization
     * logic within implementing classes.
     *
     * @param url The [URL] for which the HTTP connection needs to be established.
     * @return An instance of [HttpURLConnection] ready to be used for initiating HTTP requests.
     * @throws java.io.IOException If an I/O exception occurs while opening the connection.
     */
    fun create(url: URL): HttpURLConnection
}