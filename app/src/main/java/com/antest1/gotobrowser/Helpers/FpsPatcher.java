package com.antest1.gotobrowser.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.antest1.gotobrowser.R;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.antest1.gotobrowser.Constants.PREF_MOD_FPS;

public class FpsPatcher {
    private static boolean isPatcherEnabled = false;

    public void prepare(Activity activity) {
        // Only update the enable status when opening the browser view
        // Require reopening the browser after switching the MOD on or off
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);
        isPatcherEnabled = sharedPref.getBoolean(PREF_MOD_FPS, false);
    }


    public static String patchFps(String main_js){
        if (!isPatcherEnabled) {
            return main_js;
        }

        Map<String, String> stringsToReplace = new LinkedHashMap<>();

        // Change the create.js ticker mode from Timer to to RAF
        stringsToReplace.put(
                "(createjs(?:\\[\\w+\\('\\w+'\\)\\]){2})\\=createjs(?:\\[\\w+\\('\\w+'\\)\\]){2},",
                "$1=createjs.Ticker.RAF,");

        String replaced = main_js;
        for (Map.Entry<String, String> stringToReplace : stringsToReplace.entrySet()) {
            Pattern pattern = Pattern.compile(stringToReplace.getKey());
            Matcher matcher = pattern.matcher(replaced);
            if (matcher.find() && !matcher.find()) {
                // Find one and only one match
                matcher.reset();
                replaced = matcher.replaceFirst(stringToReplace.getValue());
            } else {
                // The main.js is probably updated and no longer support the 3D patch currently
                // Immediately return the unpatched main.js
                return main_js;
            }
        }
        return replaced;
    }
}
