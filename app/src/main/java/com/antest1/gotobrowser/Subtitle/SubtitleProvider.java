package com.antest1.gotobrowser.Subtitle;

import android.content.Context;

import androidx.preference.Preference;

import com.antest1.gotobrowser.Activity.SettingsActivity;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.google.gson.JsonObject;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public interface SubtitleProvider {

    void loadKcApiData(JsonObject api_data);

    boolean loadQuoteData(Context context, String localeCode);

    SubtitleData getSubtitleData(String url, String path, String voiceSize) throws ParseException;

    void checkUpdateFromPreference(SettingsActivity.SettingsFragment fragment, String localeCode, Preference subtitleUpdate, VersionDatabase versionTable);

    void downloadUpdateFromPreference(SettingsActivity.SettingsFragment fragment, VersionDatabase versionTable);

    // Common data preset

    Integer[] voiceDiffs = {
            2475, 6547, 1471, 8691, 7847, 3595, 1767, 3311, 2507,
            9651, 5321, 4473, 7117, 5947, 9489, 2669, 8741, 6149,
            1301, 7297, 2975, 6413, 8391, 9705, 2243, 2091, 4231,
            3107, 9499, 4205, 6013, 3393, 6401, 6985, 3683, 9447,
            3287, 5181, 7587, 9353, 2135, 4947, 5405, 5223, 9457,
            5767, 9265, 8191, 3927, 3061, 2805, 3273, 7331
    };


    // valentines 2016, hinamatsuri 2015
    // valentines 2016, hinamatsuri 2015
    // whiteday 2015
    // whiteday 2015
    Map<Integer, Integer> specialDiffs = new HashMap<Integer, Integer>() {{
        put(1555, 1);
        put(3347, 2);
    }};

    // Graf Zeppelin (Kai):
    //   17:Yasen(2) is replaced with 917. might map to 17, but not for now;
    //   18 still used at day as random Attack, 918 used at night opening
    Map<String, Map<String, String>> specialShipVoices =
            new HashMap<String, Map<String, String>>() {{
                put("432", new HashMap<String, String>() {{
                    put("917", "917");
                    put("918", "918");
                }});
                put("353", new HashMap<String, String>() {{
                    put("917", "917");
                    put("918", "918");
                }});
            }};
}
