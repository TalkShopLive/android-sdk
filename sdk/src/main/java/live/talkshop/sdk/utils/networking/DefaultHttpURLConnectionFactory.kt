package live.talkshop.sdk.utils.networking

import java.net.HttpURLConnection
import java.net.URL

/**
 * Default implementation of [HttpURLConnectionFactory] that simply
 * opens a connection to a given URL. This class is used as the default
 * connection factory in production code to ensure standard behavior
 * for HTTP requests.
 */
internal class DefaultHttpURLConnectionFactory : HttpURLConnectionFactory {
    override fun create(url: URL): HttpURLConnection = url.openConnection() as HttpURLConnection
}