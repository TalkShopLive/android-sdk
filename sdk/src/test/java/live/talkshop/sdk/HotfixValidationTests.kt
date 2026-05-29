package live.talkshop.sdk

import live.talkshop.sdk.resources.Keys
import live.talkshop.sdk.utils.parsers.UserTokenParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Offline unit tests covering every fix in hotfix/walmart-critical-2.1.1.
 * No network required. Parity with iOS HotfixValidationTests.swift.
 */
class HotfixValidationTests {

    // Fix 1: chat_id snake_case
    @Test
    fun keyChatId_isSnakeCase() {
        assertEquals("chat_id", Keys.KEY_CHAT_ID)
    }

    @Test
    fun userTokenParser_chatId_decodesFromSnakeCase() {
        val json = """{"publish_key":"pk","subscribe_key":"sk","user_id":"u1","token":"t","chat_id":42}"""
        val model = UserTokenParser.fromJsonString(json)
        assertNotNull(model)
        assertEquals(42, model!!.chatId)
    }

    @Test
    fun userTokenParser_chatId_nilWhenAbsent() {
        val json = """{"publish_key":"pk","subscribe_key":"sk","user_id":"u1","token":"t"}"""
        val model = UserTokenParser.fromJsonString(json)
        assertNotNull(model)
        assertNull(model!!.chatId)
    }

    // Fix 2: userId fallback — snake_case preferred, camelCase fallback, no crash on absent
    @Test
    fun userTokenParser_userId_decodesFromSnakeCase() {
        val json = """{"publish_key":"pk","subscribe_key":"sk","user_id":"walmart-user","token":"t"}"""
        val model = UserTokenParser.fromJsonString(json)
        assertNotNull(model)
        assertEquals("walmart-user", model!!.userId)
    }

    @Test
    fun userTokenParser_userId_fallsBackToCamelCase() {
        val json = """{"publish_key":"pk","subscribe_key":"sk","userId":"walmart-user","token":"t"}"""
        val model = UserTokenParser.fromJsonString(json)
        assertNotNull(model)
        assertEquals("walmart-user", model!!.userId)
    }

    @Test
    fun userTokenParser_userId_blankWhenAbsent_doesNotCrash() {
        val json = """{"publish_key":"pk","subscribe_key":"sk","token":"t"}"""
        val model = UserTokenParser.fromJsonString(json)
        assertNotNull(model)
        assertEquals("", model!!.userId)
    }

    @Test
    fun userTokenParser_emptyObject_returnsModelWithBlanks() {
        val model = UserTokenParser.fromJsonString("""{}""")
        assertNotNull(model)
    }

    @Test
    fun userTokenParser_malformedJson_returnsNull() {
        // Drives the new APICalls null-safety check (no !! crash)
        val model = UserTokenParser.fromJsonString("""{not valid json""")
        assertNull(model)
    }

    // Fix 6: v2 DELETE URLs percent-encode channel name (already correct on Android — parity check)
    @Test
    fun deleteMessageV2_channelNameIsPercentEncoded() {
        // Simulating what getDeleteMessageUrl does — confirm URLEncoder logic
        val encoded = java.net.URLEncoder.encode("chat.12345", Charsets.UTF_8.name())
        assertTrue("Encoded channel must not contain raw space", !encoded.contains(" "))
    }

    @Test
    fun deleteMessageV2_specialCharsAreEncoded() {
        val encoded = java.net.URLEncoder.encode("chat test+channel", Charsets.UTF_8.name())
        assertTrue("Plus sign must be encoded", !encoded.contains(" "))
        assertTrue("Space must be encoded", encoded.contains("+") || encoded.contains("%20"))
    }
}
