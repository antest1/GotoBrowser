package com.antest1.gotobrowser.Helpers.chs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChsJsonCleanerTest {

    @Test
    fun `stripJsonComments preserves URLs inside strings and removes comments, BOM, leading zeros, and trailing commas`() {
        val raw = "\uFEFF" + """
            {
              // single-line comment
              "url": "https://raw.githubusercontent.com/asaico/Kancolle_to_CHS/main/ /* not comment */ // also not",
              /* multi-line comment
                 second line */
              "api_id": 007,
              "zero_val": 000,
              "nested": [
                "item1",
                "item2",
              ],
            }
        """.trimIndent()

        val parsed = ChsJsonCleaner.safeParseJson(raw)
        assertNotNull("Should parse JSON containing comments and relaxed syntax", parsed)
        assertTrue(parsed!!.isJsonObject)

        val obj = parsed.asJsonObject
        assertEquals(
            "https://raw.githubusercontent.com/asaico/Kancolle_to_CHS/main/ /* not comment */ // also not",
            obj.get("url").asString
        )
        assertEquals(7, obj.get("api_id").asInt)
        assertEquals(0, obj.get("zero_val").asInt)
        assertEquals(2, obj.getAsJsonArray("nested").size())
    }

    @Test
    fun `validateRegexPattern and sanitizeRegexRules filter out invalid regexes without throwing`() {
        assertTrue(ChsJsonCleaner.validateRegexPattern("""^Item_(\d+)$"""))
        assertFalse(ChsJsonCleaner.validateRegexPattern("""[unclosed group"""))
        assertFalse(ChsJsonCleaner.validateRegexPattern(""))

        val rawRulesJson = """
            {
              "dummy": "forNoComma",
              "^Item_(\\d+)$": "Item #$1",
              "[invalid_regex": "should_be_skipped",
              "Score\\s*(\\d+)": "Points: $1"
            }
        """.trimIndent()

        val el = ChsJsonCleaner.safeParseJson(rawRulesJson)
        val rules = ChsJsonCleaner.sanitizeRegexRules(el, "test_regex.json")
        assertEquals(2, rules.size)
        assertEquals("""^Item_(\d+)$""" to "Item #$1", rules[0])
        assertEquals("""Score\s*(\d+)""" to "Points: $1", rules[1])
    }
}
