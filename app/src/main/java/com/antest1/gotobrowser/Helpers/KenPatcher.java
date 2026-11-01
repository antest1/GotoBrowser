package com.antest1.gotobrowser.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.antest1.gotobrowser.R;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.google.gson.JsonSyntaxException;

import static com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH_EN;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH_ID;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH_NAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KenPatcher {

    public enum PatchLanguage {
        NONE,
        EN,
        ID,
    }

    private static PatchLanguage patchLanguage = PatchLanguage.NONE;

    public static boolean isPatcherEnabled() {
        return patchLanguage != PatchLanguage.NONE;
    }

    public static PatchLanguage getPatchLanguage() {
        return patchLanguage;
    }

    public static void setPatchLanguage(PatchLanguage language) {
        patchLanguage = language;
    }

    public static PatchLanguage getCurrentPatchLanguage(String pref) {
        if (PREF_MOD_KCCP_LANG_PATCH_EN.equals(pref)) {
            return PatchLanguage.EN;
        } else if (PREF_MOD_KCCP_LANG_PATCH_ID.equals(pref)) {
            return PatchLanguage.ID;
        } else { // fallback
            return PatchLanguage.EN;
        }
    }

    public void prepare(Activity activity) {
        // Only update the enable status when opening the browser view
        // Require reopening the browser after switching the MOD on or off
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);
        if (sharedPref.getBoolean(PREF_MOD_KCCP_LANG_PATCH, false)) {
            patchLanguage = getCurrentPatchLanguage(
                    sharedPref.getString(PREF_MOD_KCCP_LANG_PATCH_NAME, "")
            );
        } else {
            patchLanguage = PatchLanguage.NONE;
        }
    }

    public static String patchKantaiEn(String main_js, Activity activity) {
        if (!isPatcherEnabled()) {
            return main_js;
        }

        List<String> translationFiles = new ArrayList<>();
        List<String> regexFiles = new ArrayList<>();

        JsonObject translations = new JsonObject();
        StringBuilder regex = new StringBuilder("[");
        String rawText = KcEnUtils.getAssetPath() + "/kcs2/js/main.js/ignore-raw_text_translations";
        String patcherPath = activity.getExternalFilesDir(null).getAbsolutePath()
                + "/" + KcEnUtils.getAssetPath() + "/kcs2/js/main.js/ignore-patcher_contents.js";

        String patcherContents = loadExternalText(patcherPath);

        listExternalFiles(rawText, translationFiles, activity);
        listExternalFiles(rawText + "_regex", regexFiles, activity);

        for (String file : translationFiles) {
            JsonElement json = loadExternalJSON(file, activity);
            if (!(json instanceof JsonObject)) {
                continue;
            }
            for (Map.Entry<String, JsonElement> entry : ((JsonObject) json).entrySet()) {
                translations.add(entry.getKey(), entry.getValue());
            }
        }

        for (String file : regexFiles) {
            JsonElement json = loadExternalJSON(file, activity);
            if (!(json instanceof JsonObject)) {
                continue;
            }
            for (Map.Entry<String, JsonElement> entry : ((JsonObject) json).entrySet()) {
                regex.append("[\"")
                        .append(entry.getKey()
                                .replace("\n", "\\n").replace("\\", "\\\\"))
                        .append("\",")
                        .append(entry.getValue().toString()
                                .replace("\n", "\\n"))
                        .append("],");
            }
        }
        for (String file : regexFiles) {
            File testFile = new File(file);
            if(testFile.exists()){
                regex.setLength(regex.length() - 1);
                break;
            }
        }
        regex.append("]");

        if (patcherContents == null || patcherContents.length() < 50) {
            Log.e("GOTO", "Failed to load patcher file or invalid patcher");
            // return fallback behaviour
            return main_js + ";\n" +
                    "var KCT_TLS = " + translations + ";\n" +
                    "var KCT_REPLACEMENTS = " + regex + ";\n\n" +

                    "(function () {\n" +
                    "    const origText = Object.getOwnPropertyDescriptor(PIXI.Text.prototype, \"text\");\n" +
                    "    const origStyle = Object.getOwnPropertyDescriptor(PIXI.Text.prototype, \"style\");\n\n" +

                    "    function parseTags(text) {\n" +
                    "        let hasTags = false;\n" +
                    "        let scale = 1, color = null, weight = null, line = null, leading = null;\n" +
                    "        if (text && text.charCodeAt(0) === 60) {\n" +
                    "            let i = 0;\n" +
                    "            while (text.charCodeAt(i) === 60) {\n" +
                    "                const end = text.indexOf('>', i);\n" +
                    "                if (end === -1) break;\n" +
                    "                hasTags = true;\n" +
                    "                const tag = text.slice(i + 1, end);\n" +
                    "                switch (tag[0]) {\n" +
                    "                    case 's': scale = parseFloat(tag.slice(1)) || 1; break;\n" +
                    "                    case 'c': color = tag.slice(1); break;\n" +
                    "                    case 'w': weight = tag.slice(1); break;\n" +
                    "                    case 'n': line = parseFloat(tag.slice(1)); break;\n" +
                    "                    case 'l': leading = parseFloat(tag.slice(1)); break;\n" +
                    "                }\n" +
                    "                i = end + 1;\n" +
                    "            }\n" +
                    "            text = text.slice(i);\n" +
                    "        }\n" +
                    "        return { text, hasTags, scale, color, weight, line, leading };\n" +
                    "    }\n\n" +

                    "    const compiledRegex = KCT_REPLACEMENTS.map(function(pair) {\n" +
                    "        return [new RegExp(pair[0], \"gm\"), pair[1]];\n" +
                    "    });\n\n" +

                    "    Object.defineProperty(PIXI.Text.prototype, \"text\", {\n" +
                    "        get: origText.get,\n" +
                    "        set: function(text) {\n" +
                    "            const replaced = KCT_TLS[text];\n" +
                    "            if (replaced !== undefined) {\n" +
                    "                text = replaced;\n" +
                    "            } else if (text != null) {\n" +
                    "                for (var i = 0; i < compiledRegex.length; i++) {\n" +
                    "                    text = text.replace(compiledRegex[i][0], compiledRegex[i][1]);\n" +
                    "                }\n" +
                    "            }\n\n" +

                    "            text = String(text == null ? ' ' : text);\n" +
                    "            const parsed = parseTags(text);\n\n" +

                    "            if (!this._kctBaseStyle && this.style) {\n" +
                    "                this._kctBaseStyle = new PIXI.TextStyle(this.style);\n" +
                    "            }\n\n" +

                    "            if (!parsed.hasTags) {\n" +
                    "                if (this._kctBaseStyle) {\n" +
                    "                    this.style = this._kctBaseStyle;\n" +
                    "                }\n" +
                    "                origText.set.call(this, parsed.text);\n" +
                    "                return;\n" +
                    "            }\n\n" +

                    "            const base = this._kctBaseStyle;\n" +
                    "            if (base) {\n" +
                    "                const next = new PIXI.TextStyle(base);\n\n" +

                    "                if (parsed.scale !== 1)\n" +
                    "                    next.fontSize = base.fontSize * parsed.scale;\n\n" +

                    "                if (parsed.color)\n" +
                    "                    next.fill = parsed.color;\n\n" +

                    "                if (parsed.weight)\n" +
                    "                    next.fontWeight = parsed.weight;\n\n" +

                    "                if (parsed.line) {\n" +
                    "                    next.wordWrap = true;\n" +
                    "                    next.wordWrapWidth = parsed.line;\n" +
                    "                }\n\n" +

                    "                if (parsed.leading)\n" +
                    "                    next.leading = parsed.leading;\n\n" +

                    "                this.style = next;\n" +
                    "            }\n\n" +

                    "            origText.set.call(this, parsed.text);\n" +
                    "        }\n" +
                    "    });\n" +
                    "})();\n";
        }

        return main_js + ";\n" +
                "var KCT_TLS = " + translations + ";\n" +
                "var KCT_REPLACEMENTS = " + regex + ";\n\n" +
                patcherContents;
    }

    private static boolean listExternalFiles(String path, List<String> fileList, Activity activity) {
        String absolutePath = activity.getExternalFilesDir(null).getAbsolutePath();
        File[] files = new File(absolutePath + "/" + path).listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(String.valueOf(file));
                } else {
                    return false;
                }
            }
            Log.e("GOTO", "fileList:" + fileList.toString());
            return true;
        }
        return false;
    }

    public static JsonElement loadExternalJSON(String filename, Activity activity) {
        try {
            File file = new File(filename);
            FileInputStream stream = new FileInputStream(file);
            byte[] buffer = new byte[stream.available()];
            stream.read(buffer);
            stream.close();
            return new JsonParser().parse(new String(buffer, "UTF-8"));

        } catch (IOException | JsonSyntaxException ex) {
            Log.e("GOTO", KcUtils.getStringFromException(ex));
        }
        return null;
    }

    public static String loadExternalText(String filename) {
        try {
            File file = new File(filename);
            FileInputStream stream = new FileInputStream(file);
            byte[] buffer = new byte[stream.available()];
            stream.read(buffer);
            stream.close();
            return new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.e("GOTO", KcUtils.getStringFromException(ex));
        }
        return null;
    }
}