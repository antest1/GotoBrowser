package com.antest1.gotobrowser.chs

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.util.regex.Pattern

/**
 * 简中汉化 (PLUS) main.js 文本字典、动态正则与打字机前置预翻译注入器
 * 移植自 poi-plugin-kancolle-chs 的 text-patcher.es 与 main-patcher.es
 */
object ChsTextPatcher {
    private const val TAG = "GOTO-CHS"
    private val gson = Gson()

    // 1. MessageBox.activate: 通用建造、掉落、道具装备获取、近代化改修提示
    private val TYPEWRITER_MSGBOX_PATTERN = Pattern.compile(
        """(null==this\[['"]_t['"]\]&&null!=this\[(_0x[a-f0-9]+\(0x[a-f0-9]+\))\]&&0x0!=this\[\2\]\[_0x[a-f0-9]+\(0x[a-f0-9]+\)\]&&\(this\[['"]_t['"]\]=createjs)"""
    )

    // 2. ModelChangeMessageBox.activate: 舰娘改装/改二消息框
    private val TYPEWRITER_MODEL_CHANGE_PATTERN = Pattern.compile(
        """(\[_0x[a-f0-9]+\(0x[a-f0-9]+\)\]\(([a-zA-Z0-9_${'$'}]+)\)\{const [a-zA-Z0-9_${'$'}]+=_0x[a-f0-9]+;)(null==this\[['"]_t['"]\]&&null!=\2&&0x0!=\2\[)"""
    )

    // 3. MessageBoxView: 战况与剧情事件打字机消息组件
    private val TYPEWRITER_MSGVIEW_PATTERN = Pattern.compile(
        """(\[_0x[a-f0-9]+\(0x[a-f0-9]+\)\]\(([a-zA-Z0-9_${'$'}]+),([a-zA-Z0-9_${'$'}]+)=0x28\)\{const [a-zA-Z0-9_${'$'}]+=_0x[a-f0-9]+;)(let [a-zA-Z0-9_${'$'}]+=createjs)"""
    )

    /**
     * 在原生 main.js 上应用简中汉化补丁（当汉化未启用时原样返回 mainJs）
     */
    @JvmStatic
    fun patchMainJsIfEnabled(context: Context, mainJs: String): String {
        if (!ChsConfig.isPatcherEnabled()) {
            return mainJs
        }
        return try {
            val enableDict = ChsConfig.getBoolean(context, ChsConfig.KEY_ENABLE_TEXT_DICT, true)
            val enableRegex = ChsConfig.getBoolean(context, ChsConfig.KEY_ENABLE_TEXT_REGEX, true)
            val apiOptions = ChsConfig.getApiOptions(context)

            val bundle = ChsDataRepository.getOrLoad(context)
            patchMainJsWithData(
                mainJs = mainJs,
                textDict = if (enableDict) bundle.textDict else emptyMap(),
                regexRules = if (enableRegex) bundle.regexRules else emptyList(),
                enableTextDict = enableDict,
                enableTextRegex = enableRegex,
                apiOptions = apiOptions,
                inlineApiTables = null // 运行时通过 GotoBrowser.chs_get_api_table 按需获取，减少 main.js 体积
            )
        } catch (e: Exception) {
            Log.e(TAG, "注入简中汉化 main.js 异常，已安全回退原版: ${e.message}")
            mainJs
        }
    }

    /**
     * 纯函数入口：便于 JVM 单元测试与 Node VM 门禁验证
     */
    @JvmStatic
    fun patchMainJsWithData(
        mainJs: String,
        textDict: Map<String, String>,
        regexRules: List<Pair<String, String>>,
        enableTextDict: Boolean = true,
        enableTextRegex: Boolean = true,
        apiOptions: ChsConfig.ApiOptions = ChsConfig.ApiOptions(),
        inlineApiTables: Map<String, com.google.gson.JsonObject>? = null
    ): String {
        var processedJs = injectTypewriterAnchors(mainJs)

        val textScript = generateTextPatcherScript(
            textDict = textDict,
            regexRules = regexRules,
            enableTextDict = enableTextDict,
            enableTextRegex = enableTextRegex
        )
        val apiScript = ChsApiMerger.generateApiPatcherScript(
            options = apiOptions,
            inlineApiTables = inlineApiTables
        )

        val sb = StringBuilder(processedJs.length + textScript.length + apiScript.length + 64)
        sb.append(processedJs)
        if (textScript.isNotEmpty()) {
            sb.append(";\n").append(textScript)
        }
        if (apiScript.isNotEmpty()) {
            sb.append(";\n").append(apiScript)
        }
        return sb.toString()
    }

    /**
     * 注入打字机组件前置预翻译锚点；若官方更新导致混淆模式变化則静默跳过，不抛异常。
     */
    @JvmStatic
    fun injectTypewriterAnchors(mainJs: String): String {
        var js = mainJs
        try {
            val m1 = TYPEWRITER_MSGBOX_PATTERN.matcher(js)
            if (m1.find()) {
                val full = m1.group(1)
                val keyVar = m1.group(2)
                if (full != null && keyVar != null) {
                    val replacement = "(typeof window.__kanchs_translate_typewriter==='function'?(this[$keyVar]=window.__kanchs_translate_typewriter(this[$keyVar])):null),$full"
                    js = js.replaceFirst(Pattern.quote(full).toRegex(), MatcherReplacement.escapeLiteral(replacement))
                }
            }

            val m2 = TYPEWRITER_MODEL_CHANGE_PATTERN.matcher(js)
            if (m2.find()) {
                val full = m2.group(0)
                val header = m2.group(1)
                val param = m2.group(2)
                val body = m2.group(3)
                if (full != null && header != null && param != null && body != null) {
                    val replacement = "${header}if(typeof window.__kanchs_translate_typewriter==='function'){try{$param=window.__kanchs_translate_typewriter($param);}catch(_e){}}$body"
                    js = js.replaceFirst(Pattern.quote(full).toRegex(), MatcherReplacement.escapeLiteral(replacement))
                }
            }

            val m3 = TYPEWRITER_MSGVIEW_PATTERN.matcher(js)
            if (m3.find()) {
                val full = m3.group(0)
                val header = m3.group(1)
                val param = m3.group(2)
                val body = m3.group(4)
                if (full != null && header != null && param != null && body != null) {
                    val replacement = "${header}if(typeof window.__kanchs_translate_typewriter==='function'){try{$param=window.__kanchs_translate_typewriter($param);}catch(_e){}}$body"
                    js = js.replaceFirst(Pattern.quote(full).toRegex(), MatcherReplacement.escapeLiteral(replacement))
                }
            }
        } catch (e: Exception) {
            try { Log.w(TAG, "打字机前置钩子注入跳过: ${e.message}") } catch (_: Throwable) {}
            return mainJs
        }
        return js
    }

    /**
     * 生成 PIXI.Text 词典、动态正则与打字机预翻译脚本
     */
    @JvmStatic
    fun generateTextPatcherScript(
        textDict: Map<String, String>,
        regexRules: List<Pair<String, String>>,
        enableTextDict: Boolean = true,
        enableTextRegex: Boolean = true
    ): String {
        if (!enableTextDict && !enableTextRegex) {
            return ""
        }

        val dictJson = if (enableTextDict) gson.toJson(textDict) else "{}"
        val regexArray = if (enableTextRegex) {
            gson.toJson(regexRules.map { listOf(it.first, it.second) })
        } else {
            "[]"
        }

        return """
/* === 砍口垒汉化计划PLUS: PIXI.Text 文本、动态正则与打字机前置汉化引擎 === */
(function() {
  if (typeof window === 'undefined') return;
  window.__kanchs_dynamic_dict = window.__kanchs_dynamic_dict || {};

  var ENABLE_DICT = ${if (enableTextDict) "true" else "false"};
  var ENABLE_REGEX = ${if (enableTextRegex) "true" else "false"};

  var KCT_TLS = $dictJson;
  try {
    Object.assign(window.__kanchs_dynamic_dict, KCT_TLS);
  } catch (_) {}

  var KCT_RAW_RULES = $regexArray;
  var KCT_REPLACEMENTS = [];

  if (ENABLE_REGEX) {
    for (var i = 0; i < KCT_RAW_RULES.length; i++) {
      try {
        KCT_REPLACEMENTS.push([new RegExp(KCT_RAW_RULES[i][0], 'gm'), KCT_RAW_RULES[i][1]]);
      } catch (e) {
        console.warn('[KanCHS] 跳过非法正则:', KCT_RAW_RULES[i][0]);
      }
    }
  }

  function parseTags(text) {
    var hasTags = false;
    var scale = 1, color = null, weight = null, line = null, leading = null;
    if (text && text.charCodeAt(0) === 60) {
      var i = 0;
      while (text.charCodeAt(i) === 60) {
        var end = text.indexOf('>', i);
        if (end === -1) break;
        var tag = text.slice(i + 1, end);
        var c0 = tag[0];
        if (c0 === 's' || c0 === 'c' || c0 === 'w' || c0 === 'n' || c0 === 'l') {
          hasTags = true;
          switch (c0) {
            case 's': scale = parseFloat(tag.slice(1)) || 1; break;
            case 'c': color = tag.slice(1); break;
            case 'w': weight = tag.slice(1); break;
            case 'n': line = parseFloat(tag.slice(1)); break;
            case 'l': leading = parseFloat(tag.slice(1)); break;
          }
          i = end + 1;
        } else {
          break;
        }
      }
      if (hasTags) {
        text = text.slice(i);
      }
    }
    return { text: text, hasTags: hasTags, scale: scale, color: color, weight: weight, line: line, leading: leading };
  }

  window.__kanchs_translate_typewriter = function(rawText) {
    if (!rawText || typeof rawText !== 'string') return rawText;
    try {
      var clean = rawText.replace(/<br\s*\/?>/gi, '\n').replace(/\u2019/g, "'");
      if (ENABLE_DICT) {
        if (KCT_TLS[clean] !== undefined) return KCT_TLS[clean];
        if (KCT_TLS[rawText] !== undefined) return KCT_TLS[rawText];
        if (window.__kanchs_dynamic_dict) {
          if (window.__kanchs_dynamic_dict[clean] !== undefined) return window.__kanchs_dynamic_dict[clean];
          if (window.__kanchs_dynamic_dict[rawText] !== undefined) return window.__kanchs_dynamic_dict[rawText];
        }
        var trimmed = clean.trim();
        if (KCT_TLS[trimmed] !== undefined) return KCT_TLS[trimmed];
        if (window.__kanchs_dynamic_dict && window.__kanchs_dynamic_dict[trimmed] !== undefined) {
          return window.__kanchs_dynamic_dict[trimmed];
        }
        var noEndNl = clean.replace(/\n+$/, '');
        if (KCT_TLS[noEndNl] !== undefined) return KCT_TLS[noEndNl];
        if (window.__kanchs_dynamic_dict && window.__kanchs_dynamic_dict[noEndNl] !== undefined) {
          return window.__kanchs_dynamic_dict[noEndNl];
        }
      }
      if (ENABLE_REGEX) {
        var replaced = clean;
        for (var i = 0; i < KCT_REPLACEMENTS.length; i++) {
          replaced = replaced.replace(KCT_REPLACEMENTS[i][0], KCT_REPLACEMENTS[i][1]);
        }
        if (replaced !== clean) return replaced;
      }
      var mSlot = clean.match(/^(.+?)(?:\s+(★(?:MAX|\+\d+)))?を(\d+個)?手に入れました。$/);
      if (mSlot) {
        var itemName = mSlot[1];
        var star = mSlot[2] ? ' ' + mSlot[2] : '';
        var count = mSlot[3] ? ' ' + mSlot[3] : '';
        var transItem = (ENABLE_DICT && (KCT_TLS[itemName] || (window.__kanchs_dynamic_dict && window.__kanchs_dynamic_dict[itemName]))) || itemName;
        return '获得了 ' + transItem + star + count + '。';
      }
      if (clean.indexOf('近代化改修（合成）に失敗しました。') !== -1) {
        return clean.replace('近代化改修（合成）に失敗しました。', '近代化改装（合成）失败了。');
      }
      if (clean.indexOf('近代化改修（合成）が成功しました。') !== -1) {
        return clean.replace('近代化改修（合成）が成功しました。', '近代化改装（合成）成功了。');
      }
      return clean;
    } catch (_err) {
      return rawText;
    }
  };

  function hookPixiText() {
    if (typeof PIXI === 'undefined' || !PIXI.Text || !PIXI.Text.prototype) {
      return false;
    }
    if (PIXI.Text.prototype.__kanchs_text_hooked) {
      return true;
    }
    PIXI.Text.prototype.__kanchs_text_hooked = true;

    var origText = Object.getOwnPropertyDescriptor(PIXI.Text.prototype, 'text');

    Object.defineProperty(PIXI.Text.prototype, 'text', {
      get: origText && origText.get ? origText.get : function() { return this._text; },
      set: function(val) {
        var text = (val === null || val === undefined) ? '' : String(val);

        if (ENABLE_DICT) {
          var replaced = KCT_TLS[text];
          if (replaced === undefined && window.__kanchs_dynamic_dict) {
            replaced = window.__kanchs_dynamic_dict[text];
          }
          if (replaced !== undefined) {
            text = replaced;
          }
        }

        if (ENABLE_REGEX && text) {
          for (var i = 0; i < KCT_REPLACEMENTS.length; i++) {
            text = text.replace(KCT_REPLACEMENTS[i][0], KCT_REPLACEMENTS[i][1]);
          }
        }

        var parsed = parseTags(text);
        if (parsed.hasTags && typeof PIXI.TextStyle === 'function' && this.style) {
          if (!this._kctBaseStyle) {
            this._kctBaseStyle = new PIXI.TextStyle(this.style);
          }
          var base = this._kctBaseStyle;
          var next = new PIXI.TextStyle(base);
          if (parsed.scale !== 1) next.fontSize = base.fontSize * parsed.scale;
          if (parsed.color) next.fill = parsed.color;
          if (parsed.weight) next.fontWeight = parsed.weight;
          if (parsed.line) { next.wordWrap = true; next.wordWrapWidth = parsed.line; }
          if (parsed.leading) next.leading = parsed.leading;
          this.style = next;
          text = parsed.text;
        } else if (this._kctBaseStyle) {
          this.style = this._kctBaseStyle;
        }

        if (origText && origText.set) {
          origText.set.call(this, text);
        } else {
          if (this._text === text) return;
          this._text = text;
          this.dirty = true;
        }
      },
      configurable: true,
      enumerable: true
    });
    return true;
  }

  if (!hookPixiText()) {
    var timer = setInterval(function() {
      if (hookPixiText()) clearInterval(timer);
    }, 50);
    setTimeout(function() { clearInterval(timer); }, 10000);
  }
})();
""".trimIndent()
    }

    private object MatcherReplacement {
        fun escapeLiteral(s: String): String = Pattern.quote(s).let {
            java.util.regex.Matcher.quoteReplacement(s)
        }
    }
}
