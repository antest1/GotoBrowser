package com.antest1.gotobrowser.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.antest1.gotobrowser.R;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.google.gson.JsonSyntaxException;

import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KenPatcher {

    private static boolean isPatcherEnabled = false;

    public boolean isPatcherEnabled() {
        return isPatcherEnabled;
    }

    public void prepare(Activity activity) {
        // Only update the enable status when opening the browser view
        // Require reopening the browser after switching the MOD on or off
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);
        isPatcherEnabled = sharedPref.getBoolean(PREF_MOD_KANTAIEN, false);
    }

    public static String patchKantaiEn(String main_js, Activity activity) {
        if (!isPatcherEnabled) {
            return main_js;
        }

        List<String> translationFiles = new ArrayList<String>();
        List<String> regexFiles = new ArrayList<String>();

        JsonObject translations = new JsonObject();
        StringBuilder regex = new StringBuilder("[");

        listAssetFiles("en-patch-strings/ignore-raw_text_translations", translationFiles, activity);
        listAssetFiles("en-patch-strings/ignore-raw_text_translations_regex", regexFiles, activity);

        for(String file:translationFiles) {
            JsonElement json = loadJSON(file, activity);
            if(!(json instanceof JsonObject)) {
                continue;
            }
            for(Map.Entry<String, JsonElement> entry: ((JsonObject)json).entrySet()) {
                translations.add(entry.getKey(), entry.getValue());
            }
        }

        for(String file:regexFiles) {
            JsonElement json = loadJSON(file, activity);
            if(!(json instanceof JsonObject)) {
                continue;
            }
            for(Map.Entry<String, JsonElement> entry: ((JsonObject)json).entrySet()) {
                regex.append("[\"")
                        .append(entry.getKey()
                                .replace("\n", "\\n").replace("\\", "\\\\"))
                        .append("\",")
                        .append(entry.getValue().toString()
                                .replace("\n", "\\n"))
                        .append("],");
            }
        }
        regex.setLength(regex.length() - 1);
        regex.append("]");

        return main_js + ";\n" +
            "var KCT_TLS = " + translations.toString() + "\n" +
            "var KCT_REPLACEMENTS = " + regex.toString() + "\n\n" +

            "Object.defineProperty(PIXI.Text.prototype, \"text\", {  get() { return this._text; }, set(text) {\n" +
            "        const replaced = KCT_TLS[text]\n" +
            "        if (replaced !== undefined)\n" +
            "            text = replaced\n" +
            "        else if (text != null) {\n" +
            "            for (const [from, to] of KCT_REPLACEMENTS)\n" +
            "                text = text.replace(new RegExp(from, \"g\"), to)\n" +
            "        }\n" +
            "        text = String(text === '' || text === null || text === undefined ? ' ' : text);\n\n" +

            "        if (this._text === text)\n" +
            "            return;\n\n" +

            "        this._text = text;\n" +
            "        this.dirty = true;\n" +
            "}})\n";
    }

    private static boolean listAssetFiles(String path, List<String> fileList, Activity activity) {
        String[] list;
        try {
            list = activity.getAssets().list(path);
            if (list.length > 0) {
                for (String file : list) {
                    if (!listAssetFiles(path + "/" + file, fileList, activity))
                        return false;
                    else {
                        fileList.add(path + "/" + file);
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static JsonElement loadJSON(String filename, Activity activity) {
        try {
            InputStream stream = activity.getAssets().open(filename);
            byte[] buffer = new byte[stream.available()];
            stream.read(buffer);
            stream.close();
            return new JsonParser().parse(new String(buffer, "UTF-8"));

        } catch (IOException | JsonSyntaxException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}