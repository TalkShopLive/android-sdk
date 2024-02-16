package live.talkshop.sdk.utils.networking

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Test suite for the APIHandler object.
 *
 * This class contains unit tests for the APIHandler, focusing on its ability to
 * make HTTP requests using mocked network connections.
 */
class APIHandlerTest {
    private lateinit var mockHttpURLConnection: HttpURLConnection
    private lateinit var mockUrlFactory: HttpURLConnectionFactory

    @Before
    fun setUp() {
        // Initialize mocks
        mockHttpURLConnection = mock(HttpURLConnection::class.java)
        mockUrlFactory = mock(HttpURLConnectionFactory::class.java)

        // Setup the default response for the connection
        `when`(mockUrlFactory.create(any() ?: URL("http://example.com/"))).thenReturn(
            mockHttpURLConnection
        )

        // Ensure APIHandler uses the mocked factory
        APIHandler.connectionFactory = mockUrlFactory
    }

    @Test
    fun `request GET with success response`() = runTest {
        val expectedResponseBody = "Success"
        val inputStream =
            ByteArrayInputStream(expectedResponseBody.toByteArray(StandardCharsets.UTF_8))
        `when`(mockHttpURLConnection.responseCode).thenReturn(HttpURLConnection.HTTP_OK)
        `when`(mockHttpURLConnection.inputStream).thenReturn(inputStream)

        val response = APIHandler.makeRequest("https://example.com/api", "GET")

        assertEquals(expectedResponseBody, response, "Success")
    }

    /**
     * Tests the handling of a POST request with a JSON payload.
     * Verifies that the APIHandler correctly sends the payload and handles the response.
     */
    @Test
    fun `request POST with payload`() = runTest {
        val payload = JSONObject().apply {
            put("key", "value")
        }
        val expectedResponse = "Posted"
        val inputStream = ByteArrayInputStream(expectedResponse.toByteArray())
        val outputStream = ByteArrayOutputStream()

        `when`(mockHttpURLConnection.responseCode).thenReturn(HttpURLConnection.HTTP_OK)
        `when`(mockHttpURLConnection.inputStream).thenReturn(inputStream)
        `when`(mockHttpURLConnection.outputStream).thenReturn(outputStream)

        val response = APIHandler.makeRequest("https://example.com/api/post", "POST", payload)

        assertEquals(expectedResponse, response, "Posted")
        assertEquals(payload.toString(),
            withContext(Dispatchers.IO) {
                outputStream.toString(StandardCharsets.UTF_8.name())
            }, "{\"key\":\"value\"}"
        )
    }
}