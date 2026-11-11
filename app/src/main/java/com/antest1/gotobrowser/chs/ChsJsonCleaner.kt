package com.antest1.gotobrowser.chs

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * 健壮的 JSON 清洗器与正则表达式校验器
 * 移植自 poi-plugin-kancolle-chs 的 json-cleaner.es 与 regex-validator.es
 */
object ChsJsonCleaner {
    private const val TAG = "GOTO-CHS"

    private fun logWarn(msg: String) {
        try {
            Log.w(TAG, msg)
        } catch (_: Throwable) {
        }
    }

    private val LEADING_ZERO_NONZERO_REGEX = Regex(""":\s*0+([1-9]\d*)\b""")
    private val LEADING_ZERO_ONLY_REGEX = Regex(""":\s*0+\b""")

    /**
     * 状态机逐字符剥除 JSON 中的 BOM、单行与多行注释、非标前导零与尾随逗号。
     * 确保字符串字面量内部出现的单行或多行注释符号（如 URL）不被误删。
     */
    @JvmStatic
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

        // 在非字符串区域修复前导零与尾随逗号
        val stripped = stripTrailingCommasAndLeadingZerosOutsideStrings(sb.toString())
        return stripped
    }

    /**
     * 仅在字符串外部修复前导零数值与尾随逗号，防止破坏字符串内容
     */
    private fun stripTrailingCommasAndLeadingZerosOutsideStrings(input: String): String {
        // 先分段处理字符串内外区域
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

        // 移除尾随逗号（扫描非字符串区的逗号后仅跟空白并紧随 ] 或 }）
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
                    // 跳过该尾随逗号
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
     * 安全解析可能包含注释、BOM、前导零与尾随逗号的 JSON 文本
     */
    @JvmStatic
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
                logWarn("JSON 解析失败: ${e.message}")
                null
            }
        }
    }

    /**
     * 校验正则表达式模式是否合法
     */
    @JvmStatic
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
     * 校验并提取合法的正则替换规则列表 (同时兼容 JsonObject 与 JsonArray)
     */
    @JvmStatic
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
            logWarn("[$sourceTag] 跳过非法正则: $pattern")
        }
    }
}
