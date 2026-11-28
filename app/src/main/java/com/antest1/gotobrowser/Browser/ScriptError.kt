package com.antest1.gotobrowser.Browser

import java.net.URI

/** An uncaught exception thrown by a game script, as the WebView console reported it. */
data class ScriptError(
    /** The error's type ("Error", "TypeError"), or empty when something else was thrown. */
    val name: String,
    val message: String,
    /** Script file name and line, e.g. "main.js:1". */
    val source: String
)

private const val UNCAUGHT = "Uncaught "
private const val UNCAUGHT_IN_PROMISE = "Uncaught (in promise)"
private val ERROR_NAME = Regex("([A-Za-z_$][\\w$]*)?(Error|Exception)")

/**
 * Reads an error-level console [message] ("Uncaught Error: ..."). Null for anything else:
 * rejected promises, which the game may leave unhandled without breaking, and scripts that
 * are not served from a game server.
 */
fun parseScriptError(message: String?, sourceId: String?, line: Int): ScriptError? {
    if (message == null || !message.startsWith(UNCAUGHT) || message.startsWith(UNCAUGHT_IN_PROMISE)) return null
    val uri = runCatching { URI(sourceId) }.getOrNull() ?: return null
    if (!KcsInterface.isGameHost(uri.host)) return null

    val body = message.removePrefix(UNCAUGHT)
    val head = body.substringBefore(": ", "")
    val (name, text) = if (ERROR_NAME.matches(head)) head to body.substring(head.length + 2) else "" to body
    val file = uri.path.orEmpty().substringAfterLast('/')
    return ScriptError(name, text, "$file:$line")
}
