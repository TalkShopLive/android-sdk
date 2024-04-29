package live.talkshop.sdk.utils.networking

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import live.talkshop.sdk.resources.Constants.RESPONSE_PAYLOAD
import live.talkshop.sdk.resources.Constants.RESPONSE_POSTED
import live.talkshop.sdk.resources.Constants.RESPONSE_SUCCESS
import live.talkshop.sdk.resources.Constants.TERM_KEY
import live.talkshop.sdk.resources.Constants.TERM_VALUE
import live.talkshop.sdk.resources.URLs.URL_SAMPLE_API
import live.talkshop.sdk.resources.URLs.URL_SAMPLE_API_POST
import live.talkshop.sdk.resources.URLs.URL_SAMPLE_BASE
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
        mockHttpURLConnection = mock(HttpURLConnection::class.java)
        mockUrlFactory = mock(HttpURLConnectionFactory::class.java)

        `when`(mockUrlFactory.create(any() ?: URL(URL_SAMPLE_BASE))).thenReturn(
            mockHttpURLConnection
        )

        APIHandler.connectionFactory = mockUrlFactory
    }

    /**
     * Tests the handling of a GET request.
     * Verifies that the APIHandler correctly receives and handles the response.
     */
    @Test
    fun `request GET with success response`() = runTest {
        val inputStream = ByteArrayInputStream(RESPONSE_SUCCESS.toByteArray(StandardCharsets.UTF_8))
        `when`(mockHttpURLConnection.responseCode).thenReturn(HttpURLConnection.HTTP_OK)
        `when`(mockHttpURLConnection.inputStream).thenReturn(inputStream)

        val response = APIHandler.makeRequest(URL_SAMPLE_API, HTTPMethod.GET)

        assertEquals(RESPONSE_SUCCESS, response, RESPONSE_SUCCESS)
    }

    /**
     * Tests the handling of a POST request with a JSON payload.
     * Verifies that the APIHandler correctly sends the payload and handles the response.
     */
    @Test
    fun `request POST with payload`() = runTest {
        val payload = JSONObject().apply { put(TERM_KEY, TERM_VALUE) }
        val inputStream = ByteArrayInputStream(RESPONSE_POSTED.toByteArray())
        val outputStream = ByteArrayOutputStream()

        `when`(mockHttpURLConnection.responseCode).thenReturn(HttpURLConnection.HTTP_OK)
        `when`(mockHttpURLConnection.inputStream).thenReturn(inputStream)
        `when`(mockHttpURLConnection.outputStream).thenReturn(outputStream)

        val response = APIHandler.makeRequest(URL_SAMPLE_API_POST, HTTPMethod.POST, payload)

        assertEquals(RESPONSE_POSTED, response, RESPONSE_POSTED)
        assertEquals(
            payload.toString(),
            withContext(Dispatchers.IO) {
                outputStream.toString(StandardCharsets.UTF_8.name())
            },
            RESPONSE_PAYLOAD
        )
    }
}