package com.antest1.gotobrowser.Subtitle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.preference.Preference;

import com.antest1.gotobrowser.Activity.SettingsActivity;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.R;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.antest1.gotobrowser.Constants.GITHUBAPI_ROOT;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_UPDATE;
import static com.antest1.gotobrowser.Constants.SUBTITLE_PATH_FORMAT;
import static com.antest1.gotobrowser.Constants.SUBTITLE_ROOT;
import static com.antest1.gotobrowser.Constants.SUBTITLE_SIZE_PATH;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.getRetrofitAdapter;
import static com.antest1.gotobrowser.Helpers.KcUtils.getStringFromException;
import static com.antest1.gotobrowser.Helpers.KcUtils.reportException;

// Reference: https://github.com/KC3Kai/KC3Kai/issues/1180
//            https://github.com/KC3Kai/KC3Kai/blob/master/src/library/modules/Translation.js
public class Kc3SubtitleProvider implements SubtitleProvider {
    private static final String SUBTITLE_META_ROOT_FORMAT =
            "https://raw.githubusercontent.com/KC3Kai/KC3Kai/%s/src/data/quotes_size.json";
    public static final String QUOTES_FILENAME_FORMAT = "quotes_%s.json";

    private static JsonObject quoteSizeData = new JsonObject();
    private static final int MAX_LOOP = 9;


    private static final List<Integer> voiceDiffsList = Arrays.asList(voiceDiffs);

    private static final Map<String, String> filenameToShipId = new HashMap<>();

    private static JsonObject shipDataGraph = new JsonObject();
    private static JsonObject quoteLabel = new JsonObject();
    private static JsonObject quoteData = new JsonObject();

    // Voice line duration in ms
    private static int baseMillisVoiceLine = 3000;
    private static int extraMillisPerChar = 0;

    private static final Map<String, Map<String, Integer>> cachedVoiceDiffMap = new HashMap<>();

    private static int getVoiceDiffByFilename(String shipId, String filename) {
        Map<String, Integer> currentShipVoiceMap = cachedVoiceDiffMap.get(shipId);
        if (currentShipVoiceMap == null) {
            currentShipVoiceMap = new HashMap<>();
            cachedVoiceDiffMap.put(shipId, currentShipVoiceMap);
        }

        Integer voiceDiff = currentShipVoiceMap.get(filename);
        if (voiceDiff == null) {
            voiceDiff = computeVoiceDiff(shipId, filename);
            currentShipVoiceMap.put(filename, voiceDiff);
        }
        return voiceDiff;
    }

    private static int computeVoiceDiff(String shipId, String filename) {
        int ship_id_val = Integer.parseInt(shipId, 10);
        int f = Integer.parseInt(filename, 10);
        int k = 17 * (ship_id_val + 7);
        int r = f - 100000;
        if (f > 53 && r < 0) {
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

    private static String getVoiceLineByFilename(String shipId, String filename) {
        if (shipId.equals("9998") || shipId.equals("9999")) {
            return filename;
        }
        // Some ships use special voice line filenames
        Map<String, String> specialMap = specialShipVoices.get(shipId);
        if (specialMap != null && specialMap.containsKey(filename)) {
            return specialMap.get(filename);
        }
        int computedDiff = getVoiceDiffByFilename(shipId, filename);
        int computedIndex = getComputedIndex(computedDiff);
        // If computed diff is not in voiceDiffs, return the computedDiff itself so we can lookup quotes via voiceDiff
        return String.valueOf(computedIndex > -1 ? computedIndex + 1 : computedDiff);
    }

    private static int getComputedIndex(int computedDiff) {
        int index = voiceDiffsList.indexOf(computedDiff);
        if (specialDiffs.containsKey(computedDiff)) {
            return specialDiffs.get(computedDiff);
        } else {
            return index;
        }
    }


    public void loadKcApiData(JsonObject api_data) {
        JsonArray api_mst_shipgraph = api_data.getAsJsonArray("api_mst_shipgraph");
        JsonArray api_mst_ship = api_data.getAsJsonArray("api_mst_ship");
        buildShipGraph(api_mst_ship);
        for (JsonElement item : api_mst_shipgraph) {
            JsonObject ship = item.getAsJsonObject();
            String shipId = ship.get("api_id").getAsString();
            String shipFn = ship.get("api_filename").getAsString();
            filenameToShipId.put(shipFn, shipId);
        }
        Log.e("GOTO", "filenameToShipId: " + filenameToShipId.size());
    }

    private void buildShipGraph(JsonArray data) {
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

    public boolean loadQuoteData(Context context, String localeCode) {
        // For KC3 Kai data format, need to load the quote size meta first
        loadQuoteAnnotation(context);

        String filename = String.format(Locale.US, "quotes_%s.json", localeCode);
        String data_dir = KcUtils.getAppCacheFileDir(context, "/subtitle/");
        String subtitle_path = data_dir.concat(filename);
        quoteData = KcUtils.readJsonObjectFromFile(subtitle_path);
        if (quoteData != null) {
            // Update duration config
            JsonObject quoteTimingData = quoteData.getAsJsonObject("timing");
            if (quoteTimingData != null && quoteTimingData.size() == 2 &&
                    quoteTimingData.get("baseMillisVoiceLine") != null &&
                    quoteTimingData.get("extraMillisPerChar") != null) {
                baseMillisVoiceLine = quoteTimingData.get("baseMillisVoiceLine").getAsInt();
                extraMillisPerChar = quoteTimingData.get("extraMillisPerChar").getAsInt();
            }

            return true;
        }
        return false;
    }

    private void loadQuoteAnnotation(Context context) {
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
        Kc3SubtitleCheck updateCheck = getRetrofitAdapter(context, GITHUBAPI_ROOT).create(Kc3SubtitleCheck.class);
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
                    Log.e("GOTO", "quote_size: " + (quoteSizeData == null ? -1 : quoteSizeData.size()));
                }
            }
            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                loadQuoteSizeData(submeta_path);
                Log.e("GOTO", "quote_size: " + (quoteSizeData == null ? -1 : quoteSizeData.size()));
            }
        });


        Log.e("GOTO", "quote_meta: " + (quoteLabel == null ? -1 : quoteLabel.size()));
    }

    private static void downloadQuoteSizeData(VersionDatabase table, Context context, String commit, File file) {
        String key = "|kc3_quote_size|" + file.getPath();
        String download_path = String.format(Locale.US, SUBTITLE_META_ROOT_FORMAT, commit);
        OkHttpClient resourceClient = new OkHttpClient();
        Thread downloadThread = new Thread() {
            @Override
            public void run() {
                String last_modified = table.getValue(key);
                if (!last_modified.equals(commit)) {
                    JsonObject result = KcUtils.downloadResource(resourceClient, download_path, file);
                    int response_code = -1;
                    if (result.has("response_code")) {
                        response_code = result.get("response_code").getAsInt();
                        if (response_code != 304) table.putValue(key, commit);
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


    private static int getDuration(String data) {
        return baseMillisVoiceLine + extraMillisPerChar * data.length();
    }

    private String findQuoteKeyByFileSize(String shipId, String voiceLine, String voiceSize) {
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


    public SubtitleData getSubtitleData(String url, String path, String voiceSize) {
        SubtitleData data = null;
        if (url.contains("/kcs/sound/kc")) {
            String info = path.replace("/kcs/sound/kc", "").replace(".mp3", "");
            String[] fn_code = info.split("/");
            String voiceLine;
            String voice_filename = fn_code[0];
            String voice_code = fn_code[1];
            String shipId = voice_filename;
            if (filenameToShipId.containsKey(voice_filename)) {
                shipId = filenameToShipId.get(voice_filename);
                voiceLine = getVoiceLineByFilename(shipId, voice_code);
            } else {
                voiceLine = getVoiceLineByFilename(voice_filename, voice_code);
            }
            Log.e("GOTO", "file info: " + info);
            Log.e("GOTO", "voiceline: " + voiceLine);
            int voiceLineValue = Integer.parseInt(voiceLine);
            data = getSubtitleDataInternal(shipId, voiceLine, voiceSize);
            if (data != null && voiceLineValue >= 30 && voiceLineValue <= 53) {
                // hourly voice line
                Date now = new Date();
                String voiceLineTime = String.format(Locale.US, "%02d:00:00", voiceLineValue - 30);
                @SuppressLint("SimpleDateFormat") SimpleDateFormat time_fmt = new SimpleDateFormat("HH:mm:ss");
                Date time_src, time_tgt;
                try {
                    time_src = time_fmt.parse(time_fmt.format(now));
                    time_tgt = time_fmt.parse(voiceLineTime);
                } catch (ParseException e) {
                    return null;
                }
                long diffMsec = time_tgt.getTime() - time_src.getTime();
                if (voiceLineValue == 30) diffMsec += 86400000;

                data.setExtraDelay(diffMsec);
            }
        } else if (url.contains("/voice/titlecall_")) {
            String info = path.replace("/kcs2/resources/voice/", "").replace(".mp3", "");
            String[] fn_code = info.split("/");
            data = getSubtitleDataInternal(fn_code[0], fn_code[1], voiceSize);
        }
        return data;
    }

    private SubtitleData getSubtitleDataInternal(String shipId, String voiceLine, String voiceSize) {
        // Get the subtitle as well as subtitle of the ship before Remodel (i.e. Kai Ni -> Kai)
        JsonObject subtitle = getQuoteString(shipId, voiceLine, voiceSize);
        Log.e("GOTO", subtitle.toString());

        // Find the first existing match
        for (String key : subtitle.keySet()) {
            String start_time = key.split(",")[0];
            if (Pattern.matches("[0-9]+", start_time)) {
                String text = subtitle.get(key).getAsString();
                int delay = Integer.parseInt(start_time);
                return new SubtitleData(text, delay, getDuration(text));
            }
        }
        return null;
    }

    private JsonObject getQuoteString(String shipId, String voiceLine, String voiceSize) {
        return getQuoteString(shipId, voiceLine, voiceSize, MAX_LOOP);
    }

    private JsonObject getQuoteString(String shipId, String voiceLine, String voiceSize, int maxLoop) {
        Log.e("GOTO", shipId + " " +voiceLine + " " + voiceSize);
        JsonObject voicedata_base = new JsonObject();
        voicedata_base.addProperty("0", "");
        if (maxLoop > 0 && shipDataGraph.has(shipId)) {
            String before_id = shipDataGraph.get(shipId).getAsString();
            voicedata_base = getQuoteString(before_id, voiceLine, voiceSize, maxLoop - 1);
            Log.e("GOTO", "prev:" + voicedata_base.toString());
        }

        try {
            boolean is_abyssal = shipId.equals("9998");
            boolean is_npc = shipId.equals("9999");
            boolean is_title = shipId.contains("titlecall");
            boolean is_special = is_abyssal || is_npc || is_title;
            if (quoteData == null || quoteData.size() == 0 || !(is_special || quoteData.has(shipId))) {
                return voicedata_base;
            }

            boolean current_special_flag = false;
            boolean prev_special_flag = voicedata_base.has("special");
            if (is_abyssal) shipId = "abyssal";
            if (is_npc) shipId = "npc";
            if (!is_special) {
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


    private JsonObject subtitleData = null;

    public void checkUpdateFromPreference(SettingsActivity.SettingsFragment fragment, String localeCode, Preference subtitleUpdate, VersionDatabase versionTable) {
        subtitleData = null;
        subtitleUpdate.setSummary("checking updates...");
        subtitleUpdate.setEnabled(false);

        Context context = fragment.getContext();
        if (context != null) {
            String subtitlePath = String.format(Locale.US, SUBTITLE_PATH_FORMAT, localeCode);
            Kc3SubtitleCheck updateCheck = getRetrofitAdapter(context, GITHUBAPI_ROOT).create(Kc3SubtitleCheck.class);
            Call<JsonArray> call = updateCheck.check(subtitlePath);
            call.enqueue(new Callback<JsonArray>() {
                @Override
                public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                    if (fragment.getActivity() == null) return;
                    JsonArray commit_log = response.body();
                    if (commit_log != null && !commit_log.isJsonNull()) {
                        String filename = String.format(Locale.US, "quotes_%s.json", localeCode);
                        String subtitle_folder = KcUtils.getAppCacheFileDir(fragment.getContext(), "/subtitle/");
                        String subtitle_path = subtitle_folder.concat(filename);
                        String currentCommit = versionTable.getValue(subtitle_path);
                        if (commit_log.size() > 0) {
                            JsonObject latestData = commit_log.get(0).getAsJsonObject();
                            String latestCommit = latestData.get("sha").getAsString();
                            if (!currentCommit.equals(latestCommit)) {
                                subtitleData = new JsonObject();
                                subtitleData.addProperty("locale_code", localeCode);
                                subtitleData.addProperty("latest_commit", latestCommit);
                                subtitleData.addProperty("download_url", subtitlePath);
                                String summary = String.format(Locale.US,
                                        fragment.getString(R.string.setting_latest_download_subtitle),
                                        latestCommit.substring(0, 6));
                                subtitleUpdate.setSummary(summary);
                                subtitleUpdate.setEnabled(true);
                            } else {
                                subtitleUpdate.setSummary(fragment.getString(R.string.setting_latest_version));
                            }
                        } else {
                            subtitleUpdate.setSummary("no data");
                        }
                    }
                }

                @Override
                public void onFailure(Call<JsonArray> call, Throwable t) {
                    if (fragment.getActivity() == null) return;
                    subtitleUpdate.setSummary("failed loading subtitle data");
                }
            });
        }
    }

    public void downloadUpdateFromPreference(SettingsActivity.SettingsFragment fragment, VersionDatabase versionTable) {
        if (subtitleData != null) {
            try {
                String commit = subtitleData.get("latest_commit").getAsString();
                String path = subtitleData.get("download_url").getAsString();

                Kc3SubtitleRepo subtitleRepo = getRetrofitAdapter(fragment.requireContext(), SUBTITLE_ROOT).create(Kc3SubtitleRepo.class);
                Call<JsonObject> call = subtitleRepo.download(commit, path);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        saveQuotesFile(fragment, response, versionTable);
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        KcUtils.showToast(fragment.requireContext(), t.getLocalizedMessage());
                    }
                });
            } catch (IllegalStateException e) {
                Log.e("GOTO", getStringFromException(e));
            }
        }
    }

    private void saveQuotesFile(SettingsActivity.SettingsFragment fragment, Response<JsonObject> response, VersionDatabase versionTable) {
        String message;
        String locale_code = subtitleData.get("locale_code").getAsString();
        String commit = subtitleData.get("latest_commit").getAsString();

        Context context = fragment.getContext();
        if (context != null) {
            String filename = String.format(Locale.US, QUOTES_FILENAME_FORMAT, locale_code);
            String subtitle_folder = KcUtils.getAppCacheFileDir(context, "/subtitle/");
            String subtitle_path = subtitle_folder.concat(filename);
            File fileDirectory = new File(subtitle_folder);
            try {
                if (!fileDirectory.exists()) {
                    fileDirectory.mkdirs();
                }
                JsonObject data = response.body();
                if (data != null) {
                    File subtitleFile = new File(subtitle_path);
                    FileOutputStream fos = new FileOutputStream(subtitleFile);
                    fos.write(data.toString().getBytes());
                    fos.close();
                    versionTable.putValue(subtitle_path, commit);
                    Preference subtitleUpdate = fragment.findPreference(PREF_SUBTITLE_UPDATE);
                    subtitleUpdate.setSummary(fragment.getString(R.string.setting_latest_version));
                    subtitleUpdate.setEnabled(false);
                } else {
                    message = "No data to write: quotes_".concat(locale_code).concat(".json");
                    KcUtils.showToast(context, message);
                }
            } catch (IOException e) {
                KcUtils.reportException(e);
                message = "IOException while saving quotes_".concat(locale_code).concat(".json");
                KcUtils.showToast(context, message);
            }
        }
    }
}
