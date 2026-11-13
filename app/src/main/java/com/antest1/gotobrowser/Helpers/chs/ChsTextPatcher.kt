package com.antest1.gotobrowser.Helpers.chs

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.util.regex.Pattern

/**
 * Injects UI dictionary, regex, and typewriter pre-translation hooks into main.js.
 * Ported from poi-plugin-kancolle-chs (text-patcher.es / main-patcher.es).
 */
object ChsTextPatcher {
    private const val TAG = "GOTO-CHS"
    private val gson = Gson()

    // 1. MessageBox.activate: construction, drops, equipment/item rewards, modernization notices
    private val TYPEWRITER_MSGBOX_PATTERN = Pattern.compile(
        """(null==this\[['"]_t['"]\]&&null!=this\[(_0x[a-f0-9]+\(0x[a-f0-9]+\))\]&&0x0!=this\[\2\]\[_0x[a-f0-9]+\(0x[a-f0-9]+\)\]&&\(this\[['"]_t['"]\]=createjs)"""
    )

    // 2. ModelChangeMessageBox.activate: ship remodel notices
    private val TYPEWRITER_MODEL_CHANGE_PATTERN = Pattern.compile(
        """(\[_0x[a-f0-9]+\(0x[a-f0-9]+\)\]\(([a-zA-Z0-9_${'$'}]+)\)\{const [a-zA-Z0-9_${'$'}]+=_0x[a-f0-9]+;)(null==this\[['"]_t['"]\]&&null!=\2&&0x0!=\2\[)"""
    )

    // 3. MessageBoxView: sortie/event typewriter dialog component
    private val TYPEWRITER_MSGVIEW_PATTERN = Pattern.compile(
        """(\[_0x[a-f0-9]+\(0x[a-f0-9]+\)\]\(([a-zA-Z0-9_${'$'}]+),([a-zA-Z0-9_${'$'}]+)=0x28\)\{const [a-zA-Z0-9_${'$'}]+=_0x[a-f0-9]+;)(let [a-zA-Z0-9_${'$'}]+=createjs)"""
    )

    /**
     * Applies Simplified Chinese text/typewriter patches to [mainJs] when enabled;
     * returns [mainJs] unchanged when disabled or if loading fails.
     */
    fun patchChs(context: Context, mainJs: String): String {
        if (!ChsConfig.isPatcherEnabled()) {
            return mainJs
        }
        return try {
            val bundle = ChsDataRepository.getOrLoad(context)
            patchMainJsWithData(
                mainJs = mainJs,
                textDict = bundle.textDict,
                regexRules = bundle.regexRules
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject CHS text patcher, falling back to original main.js: ${e.message}")
            mainJs
        }
    }

    /**
     * Pure entry point used for JVM unit tests.
     */
    fun patchMainJsWithData(
        mainJs: String,
        textDict: Map<String, String>,
        regexRules: List<Pair<String, String>>
    ): String {
        val processedJs = injectTypewriterAnchors(mainJs)
        val textScript = generateTextPatcherScript(
            textDict = textDict,
            regexRules = regexRules
        )
        if (textScript.isEmpty()) {
            return processedJs
        }
        return StringBuilder(processedJs.length + textScript.length + 16)
            .append(processedJs)
            .append(";\n")
            .append(textScript)
            .toString()
    }

    /**
     * Inserts pre-translation hooks before typewriter animations start; silently skips
     * if game obfuscation signatures change.
     */
    fun injectTypewriterAnchors(mainJs: String): String {
        var js = mainJs
        try {
            var matchedAnchors = 0
            val m1 = TYPEWRITER_MSGBOX_PATTERN.matcher(js)
            if (m1.find()) {
                val full = m1.group(1)
                val keyVar = m1.group(2)
                if (full != null && keyVar != null) {
                    val replacement = "(typeof window.__kanchs_translate_typewriter==='function'?(this[$keyVar]=window.__kanchs_translate_typewriter(this[$keyVar])):null),$full"
                    js = js.replaceFirst(Pattern.quote(full).toRegex(), MatcherReplacement.escapeLiteral(replacement))
                    matchedAnchors++
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
                    matchedAnchors++
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
                    matchedAnchors++
                }
            }

            if (matchedAnchors == 0) {
                Log.w(TAG, "No typewriter animation anchors matched in main.js; skipping typewriter injection")
            } else if (matchedAnchors < 3) {
                Log.i(TAG, "Partially matched $matchedAnchors/3 typewriter anchors in main.js")
            }
        } catch (e: Exception) {
            try { Log.w(TAG, "Skipped typewriter anchor injection: ${e.message}") } catch (_: Throwable) {}
            return mainJs
        }
        return js
    }

    /**
     * Builds the PIXI.Text hook, regex rules, and typewriter pre-translation runtime script.
     */
    fun generateTextPatcherScript(
        textDict: Map<String, String>,
        regexRules: List<Pair<String, String>>
    ): String {
        val dictJson = gson.toJson(textDict)
        val regexArray = gson.toJson(regexRules.map { listOf(it.first, it.second) })

        return """
/* === Kancolle_to_CHS: PIXI.Text dictionary, regex & typewriter pre-translator === */
(function() {
  if (typeof window === 'undefined') return;
  window.__kanchs_dynamic_dict = window.__kanchs_dynamic_dict || {};

  var KCT_TLS = $dictJson;
  try {
    Object.assign(window.__kanchs_dynamic_dict, KCT_TLS);
  } catch (_) {}

  var KCT_RAW_RULES = $regexArray;
  var KCT_REPLACEMENTS = [];

  for (var i = 0; i < KCT_RAW_RULES.length; i++) {
    try {
      KCT_REPLACEMENTS.push([new RegExp(KCT_RAW_RULES[i][0], 'gm'), KCT_RAW_RULES[i][1]]);
    } catch (e) {
      console.warn('[KanCHS] Skipped invalid regex:', KCT_RAW_RULES[i][0]);
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
      var replaced = clean;
      for (var i = 0; i < KCT_REPLACEMENTS.length; i++) {
        replaced = replaced.replace(KCT_REPLACEMENTS[i][0], KCT_REPLACEMENTS[i][1]);
      }
      if (replaced !== clean) return replaced;
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

        var replaced = KCT_TLS[text];
        if (replaced === undefined && window.__kanchs_dynamic_dict) {
          replaced = window.__kanchs_dynamic_dict[text];
        }
        if (replaced !== undefined) {
          text = replaced;
        }

        if (text) {
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
        fun escapeLiteral(s: String): String =
            java.util.regex.Matcher.quoteReplacement(s)
    }
}
