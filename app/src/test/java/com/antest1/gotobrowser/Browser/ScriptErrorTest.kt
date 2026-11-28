package com.antest1.gotobrowser.Browser

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ScriptErrorTest {
    private val mainJs = "https://w14h.kancolle-server.com/kcs2/js/main.js?version=6.2.0.0"

    @Test
    fun readsTypeMessageAndSource() {
        val error = parseScriptError(
            "Uncaught Error: Texture Error: frame does not fit inside the base Texture dimensions", mainJs, 1
        )
        assertEquals(
            ScriptError("Error", "Texture Error: frame does not fit inside the base Texture dimensions", "main.js:1"),
            error
        )
    }

    @Test
    fun thrownValueWithoutTypeKeepsWholeMessage() {
        assertEquals(ScriptError("", "foo bar: baz", "main.js:3"), parseScriptError("Uncaught foo bar: baz", mainJs, 3))
        assertEquals(ScriptError("", "oops", "main.js:3"), parseScriptError("Uncaught oops", mainJs, 3))
    }

    @Test
    fun ooiIsAGameHost() {
        val error = parseScriptError("Uncaught TypeError: x is not a function", "https://ooi.moe/kcs2/js/main.js", 2)
        assertEquals(ScriptError("TypeError", "x is not a function", "main.js:2"), error)
    }

    @Test
    fun ignoresRejectedPromisesOtherHostsAndLogs() {
        assertNull(parseScriptError("Uncaught (in promise) DOMException: play() failed", mainJs, 1))
        assertNull(parseScriptError("Uncaught Error: x", "https://play.games.dmm.com/assets/index.js", 1))
        assertNull(parseScriptError("Uncaught Error: x", "", 1))
        assertNull(parseScriptError("Uncaught Error: x", null, 1))
        assertNull(parseScriptError("Error: x", mainJs, 1))
    }
}
