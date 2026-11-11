package com.antest1.gotobrowser.chs

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
    fun `when CHS patch is disabled, patchMainJsIfEnabled returns byte-for-byte identical script`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ChsConfig.setEnabled(context, false)
        ChsConfig.prepare(context)

        val originalMainJs = """
            function initGame() {
              const x = 1;
              return x + 2;
            }
        """.trimIndent()

        val result = ChsTextPatcher.patchMainJsIfEnabled(context, originalMainJs)
        assertEquals("关闭汉化开关时 main.js 必须逐字节保持原样", originalMainJs, result)
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
        assertTrue("应当注入通用消息框预翻译钩子", injected.contains("this[_0x12ab(0x1f)]=window.__kanchs_translate_typewriter(this[_0x12ab(0x1f)])"))
        assertTrue("应当注入改装消息框预翻译钩子", injected.contains("_msgText=window.__kanchs_translate_typewriter(_msgText)"))
        assertTrue("应当注入战况消息组件预翻译钩子", injected.contains("_viewText=window.__kanchs_translate_typewriter(_viewText)"))

        // 当游戏更新导致混淆结构变化时，应静默返回原脚本而不报错
        val unknownJs = "function completelyChangedMainJs() { return 42; }"
        assertEquals(unknownJs, ChsTextPatcher.injectTypewriterAnchors(unknownJs))
    }

    @Test
    fun `mutual exclusion between CHS and KCCP works in both directions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mainPref = context.getSharedPreferences(
            context.getString(com.antest1.gotobrowser.R.string.preference_key),
            Context.MODE_PRIVATE
        )

        // 先开启 KCCP，再开启简中汉化 -> KCCP 应自动关闭
        mainPref.edit().putBoolean(com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH, true).commit()
        ChsConfig.setEnabled(context, true)
        assertTrue(ChsConfig.isEnabled(context))
        assertFalse(
            "开启简中汉化时应自动关闭 KCCP",
            mainPref.getBoolean(com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH, false)
        )

        // 再重新开启 KCCP 并执行互斥检查 -> 简中汉化应自动关闭
        mainPref.edit().putBoolean(com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH, true).commit()
        ChsConfig.enforceMutualExclusionWithKccp(context, chsJustEnabled = false)
        assertFalse("开启 KCCP 时应自动关闭简中汉化", ChsConfig.isEnabled(context))
        assertFalse(ChsConfig.isPatcherEnabled())
    }
}
