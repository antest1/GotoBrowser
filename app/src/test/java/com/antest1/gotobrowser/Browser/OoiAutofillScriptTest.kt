package com.antest1.gotobrowser.Browser

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class OoiAutofillScriptTest {
    @Test
    fun valuesAreQuotedAsJsStrings() {
        // Password: a"b\c
        val script = WebViewManager.ooiAutofillScript("user@example.com", "a\"b\\c")
        val expected = "(function(){function f(n,v){var e=document.querySelector('input[name=\"'+n+'\"]');" +
                "if(e&&v)e.value=v;}f(\"login_id\",\"user@example.com\");f(\"password\",\"a\\\"b\\\\c\");})();"
        assertEquals(expected, script)
    }

    @Test
    fun missingValuesBecomeEmptyStrings() {
        val script = WebViewManager.ooiAutofillScript(null, null)
        assertTrue(script.contains("f(\"login_id\",\"\");f(\"password\",\"\");"))
    }
}
