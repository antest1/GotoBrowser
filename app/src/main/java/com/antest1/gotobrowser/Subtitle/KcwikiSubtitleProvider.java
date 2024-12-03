package com.antest1.gotobrowser.Subtitle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;

import com.antest1.gotobrowser.Activity.SettingsActivity;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_UPDATE;
import static com.antest1.gotobrowser.Helpers.KcUtils.getRetrofitAdapter;
import static com.antest1.gotobrowser.Helpers.KcUtils.getStringFromException;

public class KcwikiSubtitleProvider implements SubtitleProvider  {

    public static final String QUOTES_FILENAME_FORMAT = "quotes_kcwiki_%s.json";

    private static JsonObject subtitleJson = new JsonObject();

    private static Map<String, String> filenameToShipId = new HashMap<>();

    private static final List<Integer> voiceDiffsList = Arrays.asList(voiceDiffs);

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

    private String getVoiceIndex(String shipId, String voiceLine) {
        return subtitleJson.get(shipId).getAsJsonObject().get(voiceLine).getAsString();
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

    private static JsonObject shipDataGraph = new JsonObject();

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

    public boolean loadQuoteData(Context context, String localeCode){
        String filename = String.format(Locale.US, QUOTES_FILENAME_FORMAT, localeCode);
        String subtitle_folder = KcUtils.getAppCacheFileDir(context, "/subtitle/");
        String subtitle_path = subtitle_folder.concat(filename);

        try {
            subtitleJson = KcUtils.readJsonObjectFromFile(subtitle_path);
            return subtitleJson != null;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private static int getDuration(String data) {
        return 2000 + 250 * data.length();
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
            data = getSubtitleDataInternal(shipId, voiceLine);
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
            // Unfortunately KC WIKI subtitle data doesn't contain game start welcome quote
            return null;
        }
        return data;
    }

    private SubtitleData getSubtitleDataInternal(String shipId, String voiceLine) {
        try {
            if (shipId != null && voiceLine != null) {
                String quote = getVoiceIndex(shipId, voiceLine);
                return new SubtitleData(quote, 0, getDuration(quote));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    private String subtitleLocaleToDownload = "zh-cn";

    public void checkUpdateFromPreference(SettingsActivity.SettingsFragment fragment, String localeCode, Preference subtitleUpdate, VersionDatabase versionTable) {
        subtitleLocaleToDownload = localeCode;

        subtitleUpdate.setSummary("checking updates...");
        subtitleUpdate.setEnabled(false);

        Context context = fragment.getContext();
        if (context != null) {
            KcwikiSubtitleApi downloader = getRetrofitAdapter(context, "https://api.kcwiki.moe/").create(KcwikiSubtitleApi.class);
            Call<JsonObject> call = downloader.getSubTitleVersion();
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (fragment.getActivity() == null) return;
                    JsonObject versionObject = response.body();
                    if (versionObject != null && !versionObject.isJsonNull()) {
                        String filename = String.format(Locale.US, QUOTES_FILENAME_FORMAT, localeCode);


                        String subtitle_folder = KcUtils.getAppCacheFileDir(fragment.getContext(), "/subtitle/");
                        String subtitle_path = subtitle_folder.concat(filename);
                        String currentVersion = versionTable.getValue(subtitle_path);
                        if (versionObject.size() > 0) {
                            String newVersion = versionObject.get("version").getAsString();
                            if (!currentVersion.equals(newVersion)) {
                                String summary = String.format(Locale.US,
                                        fragment.getString(R.string.setting_latest_download_subtitle),
                                        newVersion);
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
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    if (fragment.getActivity() == null) return;
                    subtitleUpdate.setSummary("failed loading subtitle data");
                }
            });
        }
    }

    public void downloadUpdateFromPreference(SettingsActivity.SettingsFragment fragment, VersionDatabase versionTable) {
        try {
            KcwikiSubtitleApi downloader = getRetrofitAdapter(fragment.requireContext(), "https://api.kcwiki.moe/").create(KcwikiSubtitleApi.class);
            Call<JsonObject> call = downloader.getSubtitle(subtitleLocaleToDownload);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    saveQuotesFile(fragment, response, versionTable);
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    KcUtils.showToast(fragment.getContext(), t.getLocalizedMessage());
                }
            });
        } catch (IllegalStateException e) {
            Log.e("GOTO", getStringFromException(e));
        }
    }

    private void saveQuotesFile(SettingsActivity.SettingsFragment fragment, Response<JsonObject> response, VersionDatabase versionTable) {
        String message;
        String locale_code = subtitleLocaleToDownload;

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
                    versionTable.putValue(subtitle_path, data.get("version").getAsString());
                    Preference subtitleUpdate = fragment.findPreference(PREF_SUBTITLE_UPDATE);
                    if (subtitleUpdate != null) {
                        subtitleUpdate.setSummary(fragment.getString(R.string.setting_latest_version));
                        subtitleUpdate.setEnabled(false);
                    }
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
