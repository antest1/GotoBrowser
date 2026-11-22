package com.antest1.gotobrowser.Helpers.chs

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Lenient JSON cleaner (comments, trailing commas, leading zeros) and regex rule validator.
 * Ported from poi-plugin-kancolle-chs (json-cleaner.es / regex-validator.es).
 */
object ChsJsonCleaner {
    private const val TAG = "GOTO-CHS"

    private val LEADING_ZERO_NONZERO_REGEX = Regex(""":\s*0+([1-9]\d*)\b""")
    private val LEADING_ZERO_ONLY_REGEX = Regex(""":\s*0+\b""")

    /**
     * Strips BOM, comments, non-standard leading zeros, and trailing commas without altering strings.
     */
    fun stripJsonComments(str: String?): String {
        if (str.isNullOrEmpty()) return ""

        val content = if (str[0] == '\uFEFF') str.substring(1) else str
        val sb = StringBuilder(content.length)

        var insideString = false
        var insideSingleLineComment = false
        var insideMultiLineComment = false
        var escaped = false

        var i = 0
        val len = content.length
        while (i < len) {
            val ch = content[i]
            val next = if (i + 1 < len) content[i + 1] else '\u0000'

            if (insideSingleLineComment) {
                if (ch == '\n' || ch == '\r') {
                    insideSingleLineComment = false
                    sb.append(ch)
                }
                i++
                continue
            }

            if (insideMultiLineComment) {
                if (ch == '*' && next == '/') {
                    insideMultiLineComment = false
                    i += 2
                    continue
                }
                i++
                continue
            }

            if (insideString) {
                sb.append(ch)
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    insideString = false
                }
                i++
                continue
            }

            if (ch == '"') {
                insideString = true
                escaped = false
                sb.append(ch)
                i++
                continue
            }

            if (ch == '/' && next == '/') {
                insideSingleLineComment = true
                i += 2
                continue
            }

            if (ch == '/' && next == '*') {
                insideMultiLineComment = true
                i += 2
                continue
            }

            sb.append(ch)
            i++
        }

        // Strip leading zeros and trailing commas only outside string literals.
        val stripped = stripTrailingCommasAndLeadingZerosOutsideStrings(sb.toString())
        return stripped
    }

    /**
     * Strips non-standard leading zeros and trailing commas outside quoted strings.
     */
    private fun stripTrailingCommasAndLeadingZerosOutsideStrings(input: String): String {
        // Split input into inside-string and outside-string segments first.
        val segments = mutableListOf<Pair<Boolean, String>>()
        val current = StringBuilder()
        var inStr = false
        var esc = false
        for (ch in input) {
            if (inStr) {
                current.append(ch)
                if (esc) {
                    esc = false
                } else if (ch == '\\') {
                    esc = true
                } else if (ch == '"') {
                    inStr = false
                    segments.add(true to current.toString())
                    current.setLength(0)
                }
            } else {
                if (ch == '"') {
                    if (current.isNotEmpty()) {
                        segments.add(false to current.toString())
                        current.setLength(0)
                    }
                    inStr = true
                    current.append(ch)
                } else {
                    current.append(ch)
                }
            }
        }
        if (current.isNotEmpty()) {
            segments.add(inStr to current.toString())
        }

        val reassembled = StringBuilder(input.length)
        for ((isStr, text) in segments) {
            if (isStr) {
                reassembled.append(text)
            } else {
                var fixed = LEADING_ZERO_NONZERO_REGEX.replace(text, ": $1")
                fixed = LEADING_ZERO_ONLY_REGEX.replace(fixed, ": 0")
                reassembled.append(fixed)
            }
        }

        // Remove trailing commas immediately preceding ']' or '}' outside strings.
        val s = reassembled.toString()
        val out = StringBuilder(s.length)
        inStr = false
        esc = false
        var idx = 0
        while (idx < s.length) {
            val c = s[idx]
            if (inStr) {
                out.append(c)
                if (esc) esc = false
                else if (c == '\\') esc = true
                else if (c == '"') inStr = false
                idx++
                continue
            }
            if (c == '"') {
                inStr = true
                out.append(c)
                idx++
                continue
            }
            if (c == ',') {
                var lookahead = idx + 1
                while (lookahead < s.length && s[lookahead].isWhitespace()) {
                    lookahead++
                }
                if (lookahead < s.length && (s[lookahead] == ']' || s[lookahead] == '}')) {
                    // Skip trailing comma.
                    idx++
                    continue
                }
            }
            out.append(c)
            idx++
        }
        return out.toString()
    }

    /**
     * Parses JSON safely, falling back to comment/BOM/comma cleanup when strict parsing fails.
     */
    fun safeParseJson(jsonText: String?): JsonElement? {
        if (jsonText.isNullOrBlank()) return null
        val trimmed = if (jsonText[0] == '\uFEFF') jsonText.substring(1) else jsonText
        return try {
            JsonParser.parseString(trimmed)
        } catch (_: Exception) {
            try {
                val cleaned = stripJsonComments(trimmed)
                JsonParser.parseString(cleaned)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse JSON: ${e.message}")
                null
            }
        }
    }

    /**
     * Returns true when [pattern] compiles cleanly under multiline regex mode.
     */
    fun validateRegexPattern(pattern: String?): Boolean {
        if (pattern.isNullOrBlank()) return false
        return try {
            Pattern.compile(pattern, Pattern.MULTILINE)
            true
        } catch (_: PatternSyntaxException) {
            false
        }
    }

    /**
     * Validates and extracts regex replacement pairs from either a JsonObject or JsonArray.
     */
    fun sanitizeRegexRules(rawElement: JsonElement?, sourceFileName: String = ""): List<Pair<String, String>> {
        val validRules = mutableListOf<Pair<String, String>>()
        if (rawElement == null || rawElement.isJsonNull) return validRules

        if (rawElement.isJsonObject) {
            for ((pattern, valueElem) in rawElement.asJsonObject.entrySet()) {
                val replacement = if (valueElem != null && valueElem.isJsonPrimitive) valueElem.asString else ""
                addIfValid(validRules, pattern, replacement, sourceFileName)
            }
        } else if (rawElement.isJsonArray) {
            val arr: JsonArray = rawElement.asJsonArray
            for (i in 0 until arr.size()) {
                val item = arr[i] ?: continue
                var pattern = ""
                var replacement = ""
                if (item.isJsonArray && item.asJsonArray.size() >= 2) {
                    val pair = item.asJsonArray
                    pattern = if (pair[0].isJsonPrimitive) pair[0].asString else ""
                    replacement = if (pair[1].isJsonPrimitive) pair[1].asString else ""
                } else if (item.isJsonObject) {
                    val obj: JsonObject = item.asJsonObject
                    pattern = obj.get("match")?.takeIf { it.isJsonPrimitive }?.asString
                        ?: obj.get("from")?.takeIf { it.isJsonPrimitive }?.asString
                        ?: ""
                    replacement = obj.get("replace")?.takeIf { it.isJsonPrimitive }?.asString
                        ?: obj.get("to")?.takeIf { it.isJsonPrimitive }?.asString
                        ?: ""
                }
                addIfValid(validRules, pattern, replacement, "$sourceFileName#$i")
            }
        }
        return validRules
    }

    private fun addIfValid(
        target: MutableList<Pair<String, String>>,
        pattern: String,
        replacement: String,
        sourceTag: String
    ) {
        if (pattern.isEmpty() || pattern == "dummy" || replacement == "forNoComma") {
            return
        }
        if (validateRegexPattern(pattern)) {
            target.add(pattern to replacement)
        } else {
            Log.w(TAG, "[$sourceTag] Skipped invalid regex: $pattern")
        }
    }
}
