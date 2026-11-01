package com.antest1.gotobrowser.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.antest1.gotobrowser.R;

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

        // Change the create.js ticker mode from Timer to to RAF
        Pattern pattern = Pattern.compile("(createjs[^,;=]{0,40})(\\=createjs[^,;=]{0,40}),");
        Matcher matcher = pattern.matcher(main_js);
        return matcher.replaceFirst("$1=createjs.Ticker.RAF,");
    }
}
