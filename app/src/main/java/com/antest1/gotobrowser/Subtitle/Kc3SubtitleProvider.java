package com.antest1.gotobrowser.Subtitle;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.antest1.gotobrowser.Constants.GITHUBAPI_ROOT;
import static com.antest1.gotobrowser.Constants.SUBTITLE_SIZE_PATH;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.getRetrofitAdapter;
import static com.antest1.gotobrowser.Helpers.KcUtils.reportException;

// Reference: https://github.com/KC3Kai/KC3Kai/issues/1180
//            https://github.com/KC3Kai/KC3Kai/blob/master/src/library/modules/Translation.js
public class Kc3SubtitleProvider {
    public static final String SUBTITLE_META_ROOT_FORMAT =
            "https://raw.githubusercontent.com/KC3Kai/KC3Kai/%s/src/data/quotes_size.json";

    public static JsonObject quoteSizeData = new JsonObject();
    public static final int SPECIAL_VOICE_START_YEAR = 2014;
    public static final int SPECIAL_VOICE_END_YEAR = 2019;
    public static final int MAX_LOOP = 9;
    public static final int[] resourceKeys = {
            6657, 5699, 3371, 8909, 7719, 6229, 5449, 8561, 2987, 5501,
            3127, 9319, 4365, 9811, 9927, 2423, 3439, 1865, 5925, 4409,
            5509, 1517, 9695, 9255, 5325, 3691, 5519, 6949, 5607, 9539,
            4133, 7795, 5465, 2659, 6381, 6875, 4019, 9195, 5645, 2887,
            1213, 1815, 8671, 3015, 3147, 2991, 7977, 7045, 1619, 7909,
            4451, 6573, 4545, 8251, 5983, 2849, 7249, 7449, 9477, 5963,
            2711, 9019, 7375, 2201, 5631, 4893, 7653, 3719, 8819, 5839,
            1853, 9843, 9119, 7023, 5681, 2345, 9873, 6349, 9315, 3795,
            9737, 4633, 4173, 7549, 7171, 6147, 4723, 5039, 2723, 7815,
            6201, 5999, 5339, 4431, 2911, 4435, 3611, 4423, 9517, 3243
    };
    public static final Integer[] voiceDiffs = {
            2475,    0,    0, 8691, 7847, 3595, 1767, 3311, 2507,
            9651, 5321, 4473, 7117, 5947, 9489, 2669, 8741, 6149,
            1301, 7297, 2975, 6413, 8391, 9705, 2243, 2091, 4231,
            3107, 9499, 4205, 6013, 3393, 6401, 6985, 3683, 9447,
            3287, 5181, 7587, 9353, 2135, 4947, 5405, 5223, 9457,
            5767, 9265, 8191, 3927, 3061, 2805, 3273, 7331
    };
    public static final List<Integer> voiceDiffsList = Arrays.asList(voiceDiffs);

    public static final int[] workingDiffs = {
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
    public static final JsonObject specialDiffs =
            new JsonParser().parse("{\"1555\":\"2\",\"3347\":\"3\",\"6547\":\"2\",\"1471\":\"3\"}")
                    .getAsJsonObject();

    // Graf Zeppelin (Kai):
    //   17:Yasen(2) is replaced with 917. might map to 17, but not for now;
    //   18 still used at day as random Attack, 918 used at night opening
    public static final JsonObject specialShipVoices =
            new JsonParser().parse("{\"432\": {\"917\": 917, \"918\": 918}, \"353\": {\"917\": 917, \"918\": 918}}")
                    .getAsJsonObject();

    // These ships got special (unused?) voice line (6, aka. Repair) implemented,
    // tested by trying and succeeding to http fetch mp3 from kc server
    public static final int[] specialRepairVoiceShips = {
        56, 160, 224,  // Naka
        65, 194, 268,  // Haguro
        114, 200, 290, // Abukuma
        123, 142, 295, // Kinukasa
        126, 398,      // I-168
        127, 399,      // I-58
        135, 304,      // Naganami
        136,           // Yamato Kai
        418,           // Satsuki Kai Ni
        496,           // Zara due
    };

    public static Map<String, String> filenameToShipId = new HashMap<>();
    public static JsonObject shipDataGraph = new JsonObject();
    public static JsonObject mapBgmGraph = new JsonObject();
    public static JsonObject quoteLabel = new JsonObject();
    public static JsonObject quoteData = new JsonObject();
    public static JsonObject quoteTimingData = new JsonObject();

    public static int getFilenameByVoiceLine(int shipId, int lineNum) {
        return lineNum <= 53 ? 100000 + 17 * (shipId + 7) * (workingDiffs[lineNum - 1]) % 99173 : lineNum;
    }

    public static int getVoiceDiffByFilename(String shipId, String filename) {
        int ship_id_val = Integer.parseInt(shipId, 10);
        int f = Integer.parseInt(filename, 10);
        int k = 17 * (ship_id_val + 7);
        int r = f - 100000;
        if(f > 53 && r < 0) {
            return f;
        } else {
            for (int i = 0; i < 2600; ++i) {
                int a = r + i * 99173;
                if (a % k == 0) {
                    return a / k;
                }
            }
        }
        return -1;
    }

    public static String getVoiceLineByFilename(String shipId, String filename) {
        if (shipId.equals("9998") || shipId.equals("9999")) {
            return filename;
        }
        // Some ships use special voice line filenames
        JsonObject specialMap = specialShipVoices.getAsJsonObject(shipId);
        if (specialMap != null && specialMap.has(filename)) {
            return specialMap.get(filename).getAsString();
        }
        int computedDiff = getVoiceDiffByFilename(shipId, filename);
        int computedIndex = voiceDiffsList.indexOf(computedDiff);
        // If computed diff is not in voiceDiffs, return the computedDiff itself so we can lookup quotes via voiceDiff
        return String.valueOf(computedIndex > -1 ? computedIndex + 1 : computedDiff);
    }

    public static void buildShipGraph(JsonArray data) {
        List<Map.Entry<Integer, JsonObject>> list = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            JsonObject item = data.get(i).getAsJsonObject();
            int api_id = item.get("api_id").getAsInt();
            Map.Entry<Integer, JsonObject> item_entry =
                    new AbstractMap.SimpleEntry<>(api_id, item);
            list.add(item_entry);
        }
        Collections.sort(list, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
        Set<String> checked = new HashSet<>();
        shipDataGraph = new JsonObject();

        for (Map.Entry<Integer, JsonObject> item: list) {
            JsonObject ship_data = item.getValue();
            if (ship_data.has("api_aftershipid")) {
                String ship_id = ship_data.get("api_id").getAsString();
                String ship_afterid = ship_data.get("api_aftershipid").getAsString();
                if (ship_id.equals("624")) continue;
                if (ship_id.equals("646")) continue;
                if (ship_id.equals("650")) continue;
                if (!checked.contains(ship_id+ "_" + ship_afterid) && !ship_afterid.equals("0")) {
                    shipDataGraph.addProperty(ship_afterid, ship_id);
                    checked.add(ship_afterid + "_" + ship_id);
                    // Log.e("GOTO-ship", "" + ship_afterid + " -> " + ship_id);
                }
            }
        }
        Log.e("GOTO", "ship_graph: " + shipDataGraph.size());
    }

    public static void loadQuoteAnnotation(Context context) {
        AssetManager as = context.getAssets();
        try {
            final Gson gson = new Gson();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(
                    as.open("quotes_label.json")));
            quoteLabel = gson.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            reportException(e);
            e.printStackTrace();
        }

        String filename = "quotes_size.json";
        String data_dir = KcUtils.getAppCacheFileDir(context, "/subtitle/");
        String submeta_path = data_dir.concat(filename);
        File submeta_file = new File(submeta_path);

        VersionDatabase versionTable = new VersionDatabase(context, null, VERSION_TABLE_VERSION);
        SubtitleCheck updateCheck = getRetrofitAdapter(context, GITHUBAPI_ROOT).create(SubtitleCheck.class);

        Call<JsonArray> call = updateCheck.checkMeta(SUBTITLE_SIZE_PATH);
        call.enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                JsonArray commit_log = response.body();
                if (commit_log != null && !commit_log.isJsonNull() && commit_log.size() > 0) {
                    Log.e("GOTO", response.headers().toString());
                    Log.e("GOTO", commit_log.toString());
                    String commit = commit_log.get(0).getAsJsonObject().get("sha").getAsString();
                    downloadQuoteSizeData(versionTable, context, commit, submeta_file);
                    loadQuoteSizeData(submeta_path);
                    Log.e("GOTO", "quote_size: " + quoteSizeData.size());
                }
            }
            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                loadQuoteSizeData(submeta_path);
                Log.e("GOTO", "quote_size: " + (quoteSizeData == null ? -1 : quoteSizeData.size()));
            }
        });


        Log.e("GOTO", "quote_meta: " + quoteLabel.size());
    }

    private static void downloadQuoteSizeData(VersionDatabase table, Context context, String commit, File file) {
        String filename = "quotes_size.json";
        String download_path = String.format(Locale.US, SUBTITLE_META_ROOT_FORMAT, commit);
        OkHttpClient resourceClient = new OkHttpClient();
        Thread downloadThread = new Thread() {
            @Override
            public void run() {
                String last_modified = table.getValue(file.getAbsolutePath());
                if (!last_modified.equals(commit)) {
                    String new_last_modified = KcUtils.downloadResource(resourceClient, download_path, commit, file);
                    if (new_last_modified != null && !new_last_modified.equals("304")) {
                        table.putValue(file.getAbsolutePath(), commit);
                    }
                }
            }
        };
        try {
            downloadThread.start();
            downloadThread.join();
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    private static boolean loadQuoteSizeData(String filename) {
        quoteSizeData = KcUtils.readJsonObjectFromFile(filename);
        return quoteSizeData != null;
    }

    public static boolean loadQuoteData(Context context, String localeCode) {
        String filename = String.format(Locale.US, "quotes_%s.json", localeCode);
        String data_dir = KcUtils.getAppCacheFileDir(context, "/subtitle/");
        String subtitle_path = data_dir.concat(filename);
        quoteData = KcUtils.readJsonObjectFromFile(subtitle_path);
        if (quoteData != null) {
            quoteTimingData = quoteData.getAsJsonObject("timing");
            return true;
        }
        return false;
    }

    public static int getDefaultTiming(String data) {
        if (quoteTimingData.size() == 2) {
            int default_time = quoteTimingData.get("baseMillisVoiceLine").getAsInt();
            int extra_time = quoteTimingData.get("extraMillisPerChar").getAsInt() * data.length();
            return default_time + extra_time;
        }
        return 3000;
    }

    public static String findQuoteKeyByFileSize(String shipId, String voiceLine, String voiceSize) {
        // Special seasonal key check by file size
        JsonObject specialSeasonalKey = quoteLabel.getAsJsonObject("specialQuotesSizes");
        String base_id = shipId;
        int find_limit = 7;
        while (shipDataGraph.has(base_id) && find_limit > 0) {
            base_id = shipDataGraph.get(shipId).getAsString();
            find_limit--;
        }
        if (specialSeasonalKey.has(base_id)) {
            JsonObject shipData = specialSeasonalKey.getAsJsonObject(base_id);
            if (shipData.has(voiceLine)) {
                JsonObject sizeTable = shipData.getAsJsonObject(voiceLine);
                if (sizeTable.has(voiceSize)) {
                    JsonObject data = sizeTable.getAsJsonObject(voiceSize);
                    for (String key: data.keySet()) {
                        JsonArray months = data.getAsJsonArray(key);
                        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
                        if (months.contains(new JsonPrimitive(month))) {
                            return voiceLine + "@" + key;
                        }
                    }
                }
            }
        }

        // Special key check by file size
        JsonObject shipData = quoteSizeData.getAsJsonObject(shipId);
        if (shipData != null && shipData.has(voiceLine)) {
            JsonObject sizeTable = shipData.getAsJsonObject(voiceLine);
            if (sizeTable.has(voiceSize)) {
                String value = sizeTable.get(voiceSize).getAsString();
                if (value.length() > 0) {
                    return voiceLine + "@" + value;
                } else {
                    return voiceLine;
                }
            }
        }
        return null;
    }

    public static JsonObject getQuoteString(String shipId, String voiceLine, String voiceSize) {
        return getQuoteString(shipId, voiceLine, voiceSize, MAX_LOOP);
    }

    public static JsonObject getQuoteString(String shipId, String voiceLine, String voiceSize, int maxLoop) {
        Log.e("GOTO", shipId + " " +voiceLine + " " + voiceSize);
        String voiceline_original = voiceLine;
        JsonObject voicedata_base = new JsonObject();
        voicedata_base.addProperty("0", "");
        if (maxLoop > 0 && shipDataGraph.has(shipId)) {
            String before_id = shipDataGraph.get(shipId).getAsString();
            voicedata_base = getQuoteString(before_id, voiceline_original, voiceSize, maxLoop - 1);
            Log.e("GOTO", "prev:" + voicedata_base.toString());
        }

        try {
            boolean is_abyssal = shipId.equals("9998");
            boolean is_npc = shipId.equals("9999");
            boolean is_title = shipId.contains("titlecall");
            boolean is_special = is_abyssal || is_npc || is_title;
            if (quoteData.size() == 0 || !(is_special || quoteData.has(shipId))) {
                return voicedata_base;
            }

            boolean current_special_flag = false;
            boolean prev_special_flag = voicedata_base.has("special");
            if (is_abyssal) shipId = "abyssal";
            if (is_npc) shipId = "npc";
            if (!is_special) {
                if (specialDiffs.has(voiceLine)) {
                    voiceLine = specialDiffs.get(voiceLine).getAsString();
                }
                String specialVoiceLine = findQuoteKeyByFileSize(shipId, voiceLine, voiceSize);
                if (specialVoiceLine != null && !specialVoiceLine.equals(voiceLine)) {
                    current_special_flag = true;
                    voiceLine = specialVoiceLine;
                } else {
                    voiceLine = quoteLabel.get(voiceLine).getAsString();
                }
            }

            if (!quoteData.has(shipId)) return voicedata_base;
            JsonObject ship_data = quoteData.getAsJsonObject(shipId);
            Log.e("GOTO", shipId + " " +voiceLine + " " + voiceSize);
            if (current_special_flag || !prev_special_flag) {
                if (ship_data.has(voiceLine)) {
                    JsonElement text_data = ship_data.get(voiceLine);
                    if (text_data.isJsonPrimitive()) {
                        voicedata_base.addProperty("0", text_data.getAsString());
                    } else {
                        voicedata_base = text_data.getAsJsonObject();
                    }
                }
            }
            if (current_special_flag) voicedata_base.addProperty("special", true);
        } catch (Exception e){
            e.printStackTrace();
            reportException(e);
            voicedata_base.addProperty("0", e.getMessage());
        }

        return voicedata_base;
    }

    public static void buildMapBgmGraph(JsonArray data) {
        for (int i = 0; i < data.size(); i++) {
            JsonObject item = data.get(i).getAsJsonObject();
            mapBgmGraph.add(item.get("api_id").getAsString(), item);
        }
    }
}
