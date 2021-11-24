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

import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        List<String> translationFiles = new ArrayList<>();
        List<String> regexFiles = new ArrayList<>();

        JsonObject translations = new JsonObject();
        StringBuilder regex = new StringBuilder("[");
        String rawText = "KanColle-English-Patch-KCCP-master/EN-patch/kcs2/js/main.js/ignore-raw_text_translations";

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

    private static boolean listExternalFiles(String path, List<String> fileList, Activity activity) {
        String absolutePath = activity.getExternalFilesDir(null).getAbsolutePath();
        File[] files = new File(absolutePath + "/" + path).listFiles();
        if (files != null) {
            for(File file : files){
                if(file.isFile()){
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
            ex.printStackTrace();
        }
        return null;
    }

    public static String patchAsset(String path, Activity activity) {
        return path;
        /*if (path.equals("/storage/emulated/0/Android/data/com.antest1.gotobrowser.debug/files/cache/kcs2/img/title/title2.png")) {
            String funiFolder = "KanColle-English-Patch-KCCP-master/EN-patch/kcs2/";
            List<String> patches = new ArrayList<String>();
            listExternalFiles(funiFolder + "img/title/title2.png", patches, activity);
            Log.e("GOTO", "a certain path:" + patches.get(1));
            return patches.get(1);
        } else {
            return path;
        }*/
    }
}