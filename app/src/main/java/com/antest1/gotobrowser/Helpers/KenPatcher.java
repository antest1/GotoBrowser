package com.antest1.gotobrowser.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.antest1.gotobrowser.R;

import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN;

import java.io.File;
import java.util.Map;

public class KenPatcher {
    private Activity activity;

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

    public static String patchKantaiEn(String main_js) {
        if (!isPatcherEnabled) {
            return main_js;
        }
        String raw_text_patch = "";
        String[] rawTextTL;
        String[] rawTextTLRegex;
        File path = new File("src/main/res/en-patch-strings/ignore-raw_text_translations");
        File path_regex = new File("src/main/res/en-patch-strings/ignore-raw_text_translations_regex");
        rawTextTL = path.list();
        rawTextTLRegex = path_regex.list();

        for (String file : rawTextTL) {

        }


        return main_js + raw_text_patch;
    }
}