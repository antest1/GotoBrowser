package com.antest1.gotobrowser.Helpers.chs

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.antest1.gotobrowser.Helpers.ModPatchCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ChsPatcherTest {

    @Test
    fun `when CHS patch is disabled, patchChs returns byte-for-byte identical script`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ChsConfig.init(context)
        ChsConfig.setEnabled(false)
        ChsConfig.prepare(context)

        val originalMainJs = """
            function initGame() {
              const x = 1;
              return x + 2;
            }
        """.trimIndent()

        val result = ChsTextPatcher.patchChs(context, originalMainJs)
        assertEquals("main.js must remain byte-for-byte identical when CHS is disabled", originalMainJs, result)
        assertFalse(result.contains("__kanchs_"))
    }

    @Test
    fun `injectTypewriterAnchors injects pre-translation hooks when patterns match and degrades gracefully otherwise`() {
        val obfuscatedSample = """
            class MsgBox {
              activate() {
                null==this['_t']&&null!=this[_0x12ab(0x1f)]&&0x0!=this[_0x12ab(0x1f)][_0x34cd(0x2a)]&&(this['_t']=createjs.Tween);
              }
              [_0x56ef(0x10)](_msgText){const _0x999=_0x12ab;null==this['_t']&&null!=_msgText&&0x0!=_msgText['length']}
              [_0x78ab(0x20)](_viewText,_speed=0x28){const _0x888=_0x12ab;let _tween=createjs.Tween}
            }
        """.trimIndent()

        val injected = ChsTextPatcher.injectTypewriterAnchors(obfuscatedSample)
        assertTrue("Should inject MessageBox pre-translation hook", injected.contains("this[_0x12ab(0x1f)]=window.__kanchs_translate_typewriter(this[_0x12ab(0x1f)])"))
        assertTrue("Should inject ModelChangeMessageBox pre-translation hook", injected.contains("_msgText=window.__kanchs_translate_typewriter(_msgText)"))
        assertTrue("Should inject MessageBoxView pre-translation hook", injected.contains("_viewText=window.__kanchs_translate_typewriter(_viewText)"))

        // Degrade gracefully when obfuscated signatures change
        val unknownJs = "function completelyChangedMainJs() { return 42; }"
        assertEquals(unknownJs, ChsTextPatcher.injectTypewriterAnchors(unknownJs))
    }

    @Test
    fun `mutual exclusion between CHS and KCCP works in both directions via ModPatchCoordinator`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mainPref = context.getSharedPreferences(
            context.getString(com.antest1.gotobrowser.R.string.preference_key),
            Context.MODE_PRIVATE
        )

        // Enabling CHS automatically disables KCCP
        mainPref.edit().putBoolean(com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH, true).commit()
        ModPatchCoordinator.onChsToggled(context, true)
        assertTrue(ChsConfig.isEnabled())
        assertFalse(
            "Enabling CHS should disable KCCP",
            mainPref.getBoolean(com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH, false)
        )

        // Enabling KCCP automatically disables CHS
        mainPref.edit().putBoolean(com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH, true).commit()
        ModPatchCoordinator.onKccpToggled(context, true)
        assertFalse("Enabling KCCP should disable CHS", ChsConfig.isEnabled())
        assertFalse(ChsConfig.isPatcherEnabled())
    }

    @Test
    fun `ChsApiMerger injects only Axios interceptor without XHR prototype hooks`() {
        val textResult = ChsTextPatcher.patchMainJsWithData(
            mainJs = "function gameMain() {}",
            textDict = mapOf("k" to "v"),
            regexRules = emptyList()
        )
        assertFalse(textResult.contains("mergeSvdataString"))
        assertFalse(textResult.contains("XMLHttpRequest"))

        val apiScript = ChsApiMerger.generateApiPatcherScript(inlineApiTables = null)
        assertTrue(apiScript.contains("axios.interceptors.response.use"))
        assertFalse(apiScript.contains("XMLHttpRequest"))
    }

    @Test
    fun `PIXI text hook wraps defineProperty in try-catch and sets hooked flag after success`() {
        val textResult = ChsTextPatcher.patchMainJsWithData(
            mainJs = "function gameMain() {}",
            textDict = mapOf("k" to "v"),
            regexRules = emptyList()
        )
        val hookIdx = textResult.indexOf("function hookPixiText()")
        val tryIdx = textResult.indexOf("try {", hookIdx)
        val defIdx = textResult.indexOf("Object.defineProperty(PIXI.Text.prototype, 'text'", hookIdx)
        val flagIdx = textResult.indexOf("PIXI.Text.prototype.__kanchs_text_hooked = true;", hookIdx)
        val catchIdx = textResult.indexOf("} catch (e) {", hookIdx)
        assertTrue("Expected hookPixiText() in generated script", hookIdx >= 0)
        assertTrue("Expected try block around Object.defineProperty", tryIdx in (hookIdx + 1) until defIdx)
        assertTrue("Expected __kanchs_text_hooked = true after defineProperty", defIdx < flagIdx && flagIdx < catchIdx)
    }
}
