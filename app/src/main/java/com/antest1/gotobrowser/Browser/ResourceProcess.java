package com.antest1.gotobrowser.Browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebResourceResponse;
import android.widget.TextView;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.KcSubtitleUtils;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.antest1.gotobrowser.Browser.BrowserSoundPlayer.AUDIO_POOL_LIMIT;
import static com.antest1.gotobrowser.Browser.BrowserSoundPlayer.PLAYER_BGM;
import static com.antest1.gotobrowser.Browser.BrowserSoundPlayer.PLAYER_SE;
import static com.antest1.gotobrowser.Browser.BrowserSoundPlayer.PLAYER_VOICE;
import static com.antest1.gotobrowser.Constants.REQUEST_BLOCK_RULES;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.downloadResource;
import static com.antest1.gotobrowser.Helpers.KcUtils.getEmptyStream;

public class ResourceProcess {
    private static final int RES_IMAGE = 0b00001;
    private static final int RES_AUDIO = 0b00010;
    private static final int RES_JSON = 0b00100;
    private static final int RES_JS = 0b01000;
    private static final int RES_KCSAPI = 0b10000;

    public static boolean isImage(int state) {
        return (state & RES_IMAGE) > 0;
    }
    public static boolean isAudio(int state) {
        return (state & RES_AUDIO) > 0;
    }
    public static boolean isJson(int state) {
        return (state & RES_JSON) > 0;
    }
    public static boolean isScript(int state) {
        return (state & RES_JS) > 0;
    }
    public static boolean isKcsApi(int state) {
        return (state & RES_KCSAPI) > 0;
    }

    private BrowserActivity activity;
    private Context context;
    private VersionDatabase versionTable;
    private final OkHttpClient resourceClient = new OkHttpClient();
    private BrowserSoundPlayer browserPlayer;

    private boolean isBattleMode = false;
    private boolean isOnPractice = false;
    private int currentMapId = 0;
    private int currentBattleBgmId = 0;
    private String currentCookieHost = "";
    private List<String> titlePath = new ArrayList<>();
    private List<File> titleFiles = new ArrayList<>();
    private Map<String, String> filenameToShipId = new HashMap<>();

    private TextView subtitleText;
    private final Handler shipVoiceHandler = new Handler();
    private final Handler clearSubHandler = new Handler();
    private ScheduledExecutorService executor;

    ResourceProcess(BrowserActivity activity) {
        this.activity = activity;
        context = activity.getApplicationContext();
        versionTable = new VersionDatabase(context, null, VERSION_TABLE_VERSION);
        subtitleText = activity.findViewById(R.id.subtitle_view);
        subtitleText.setOnClickListener(v -> clearSubHandler.postDelayed(clearSubtitle, 250));
        browserPlayer = new BrowserSoundPlayer(shipVoiceHandler);
        activity.setBrowserPlayer(browserPlayer);
    }

    public static int getCurrentState(String url) {
        int state = 0;
        if (url.contains("kcs2") && (url.contains(".png") || url.contains(".jpg"))) {
            state |= RES_IMAGE;
        }
        if (url.contains(".mp3")) {
            state |= RES_AUDIO;
        }
        if (url.contains(".json")) {
            state |= RES_JSON;
        }
        if (url.contains("/js/") && url.contains(".js")) {
            state |= RES_JS;
        }

        if (url.contains("kcsapi")) {
            state |= RES_KCSAPI;
        }
        return state;
    }


    public WebResourceResponse processWebRequest(Uri source) {
        String url = source.toString();
        int resource_type = getCurrentState(url);
        if (resource_type > 0) Log.e("GOTO", url + " - " + String.valueOf(resource_type));
        boolean is_image = ResourceProcess.isImage(resource_type);
        boolean is_audio = ResourceProcess.isAudio(resource_type);
        boolean is_json = ResourceProcess.isJson(resource_type);
        boolean is_js = ResourceProcess.isScript(resource_type);
        boolean is_kcsapi = ResourceProcess.isKcsApi(resource_type);

        if (checkBlockedContent(url)) return getEmptyResponse();
        if (url.contains("kcscontents/css/common.css")) return getBlackBackgroundSheet();
        if (resource_type == 0) return null;

        JsonObject file_info = getPathAndFileInfo(source);
        String path = file_info.get("path").getAsString();
        String filename = file_info.get("filename").getAsString();
        String filepath = file_info.get("out_file_path").getAsString();
        setVolumeFromCookie(file_info);

        try {
            if (path != null && filename != null) {
                Log.e("GOTO", source.getPath());
                if (filename.equals("version.json") || filename.contains("index.php")) {
                    titlePath.clear();
                    titleFiles.clear();
                    return null;
                }

                // block ooi.moe background
                if (path.contains("ooi.css")) return getOoiSheetFromAsset();

                // load game data
                if (is_kcsapi && path.contains("/api_start2")) {
                    checkSpecialSubtitleMode();
                    checkAndUpdateGameData();
                    loadGameDataFromStorage();
                    Log.e("GOTO", versionTable.getValue("api_start2"));
                    Log.e("GOTO", "ship_filename: " + filenameToShipId.size());
                    return null;
                }

                checkPortVolume(path);
                updateCurrentMapInfo(path, filename);

                isBattleMode = checkBattleMode(path, isBattleMode);
                isOnPractice = checkPracticeMode(path, isOnPractice);
                browserPlayer.setStreamsLimit(PLAYER_VOICE, isBattleMode ? AUDIO_POOL_LIMIT : 1);
                Log.e("GOTO ", "isBattleMode: " + isBattleMode);
                Log.e("GOTO", "voicePlayers: streams_limit " + (isBattleMode ? AUDIO_POOL_LIMIT : 1));

                String source_path = source.getPath();
                browserPlayer.stopSound(source_path, isOnPractice, currentMapId, currentBattleBgmId);
                // browserPlayer.stopMp3(bgmPlayer, filepath);

                JsonObject update_info = checkResourceUpdate(source);
                if (is_image || is_json) return processImageDataResource(file_info, update_info, resource_type);
                if (is_js) return processScriptFile(file_info);
                if (is_audio) return processAudioFile(file_info, update_info);
            }
        } catch (Exception e) {
            KcUtils.reportException(e);
        }
        return null;
    }

    private JsonObject getPathAndFileInfo(Uri source) {
        JsonObject file_info = new JsonObject();

        String url = source.toString();
        String host = source.getHost();
        String path = "";
        String filename = "";
        String fullpath = "";
        String outputpath = "";
        if (source.getPath() != null) {
            path = source.getPath();
            filename = source.getLastPathSegment();
            fullpath = String.format(Locale.US, "http://%s%s", host, path);
            outputpath = context.getFilesDir().getAbsolutePath().concat("/cache/");
            if (filename != null) {
                outputpath = outputpath.concat(path.replace(filename, "").substring(1));
            }
        }
        String filepath = outputpath;
        if (filename != null) filepath = filepath.concat(filename);

        file_info.addProperty("url", url);
        file_info.addProperty("host", host);
        file_info.addProperty("path", path);
        file_info.addProperty("filename", filename);
        file_info.addProperty("full_url", fullpath);
        file_info.addProperty("out_folder_dir", outputpath);
        file_info.addProperty("out_file_path", filepath);
        return file_info;
    }

    private boolean checkBlockedContent(String url) {
        for (String rule : REQUEST_BLOCK_RULES) {
            if (url.contains(rule)) {
                Log.e("GOTO", "blocked: ".concat(url));
                return true;
            }
        }
        return false;
    }

    private void setVolumeFromCookie(JsonObject file_info) {
        String path = file_info.get("path").getAsString();
        String host = file_info.get("host").getAsString();
        if (path.contains("/kcs2/img/") && host != null) {
            currentCookieHost = host.concat("/kcs2/");
            String cookie = CookieManager.getInstance().getCookie(currentCookieHost);
            if (cookie != null) {
                String[] data = cookie.split(";");
                for (String item : data) {
                    String[] row = item.trim().split("=");
                    if (!row[1].matches("[0-9]+")) continue;
                    String key = row[0];
                    float value = (float) Integer.parseInt(row[1]) / 100;
                    if (key.equals("vol_bgm")) browserPlayer.setVolume(PLAYER_BGM, value);
                    if (key.equals("vol_voice")) browserPlayer.setVolume(PLAYER_VOICE, value);
                    if (key.equals("vol_se")) browserPlayer.setVolume(PLAYER_SE, value);
                }
            }
            // Log.e("GOTO", cookie);
        }
    }

    private WebResourceResponse getEmptyResponse() {
        return new WebResourceResponse("text/css", "utf-8", getEmptyStream());
    }

    private WebResourceResponse getBlackBackgroundSheet() {
        String replace_css = "#globalNavi, #contentsWrap {display:none;} body {background-color: black;}";
        InputStream is = new ByteArrayInputStream(replace_css.getBytes());
        return new WebResourceResponse("text/css", "utf-8", is);
    }

    private boolean checkApiDataVersion() {
        boolean update_flag = false;
        try {
            String version_url = "http://52.55.91.44/kcanotify/dv.php";
            Request versionRequest = new Request.Builder().url(version_url)
                    .header("Referer", "goto/webkit").build();
            Response version_response = resourceClient.newCall(versionRequest).execute();
            if (version_response.body() != null) {
                String version_check = version_response.body().string();
                if (!versionTable.getValue("api_start2").equals(version_check)) {
                    update_flag = true;
                }
            }
        } catch (IOException e) {
            KcUtils.reportException(e);
        }
        return update_flag;
    }

    private File getGameDataFile() {
        return new File(context.getFilesDir().getAbsolutePath()
                .concat("/cache/").concat("api_start2"));
    }

    private void checkAndUpdateGameData() {
        boolean update_flag = checkApiDataVersion();
        try {
            File file = getGameDataFile();
            if (!file.exists() || update_flag) {
                file.createNewFile();
                Log.e("GOTO", "download resource");
                String api_url = "http://52.55.91.44/kcanotify/kca_api_start2.php?v=recent";
                Request dataRequest = new Request.Builder().url(api_url)
                        .header("Referer", "goto/webkit")
                        .header("Accept-Encoding", "gzip")
                        .build();
                Response response = resourceClient.newCall(dataRequest).execute();
                ResponseBody body = response.body();
                String api_version = response.header("X-Api-Version", "");
                versionTable.putValue("api_start2", api_version);
                Log.e("GOTO", "version: " + api_version);
                if (body != null) {
                    InputStream in = body.byteStream();
                    byte[] buffer = new byte[2 * 1024];
                    int bytes;
                    FileOutputStream fos = new FileOutputStream(file);
                    while ((bytes = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytes);
                    }
                    fos.close();
                    body.close();
                }
            }
        } catch (IOException e) {
            KcUtils.reportException(e);
        }
    }

    private void loadGameDataFromStorage() {
        File file = getGameDataFile();
        InputStream buf = null;
        try {
            buf = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)));
            Gson gson = new Gson();
            Reader reader = new InputStreamReader(buf);
            JsonObject api_data = gson.fromJson(reader, JsonObject.class).getAsJsonObject("api_data");
            JsonArray api_mst_shipgraph = api_data.getAsJsonArray("api_mst_shipgraph");
            JsonArray api_mst_ship = api_data.getAsJsonArray("api_mst_ship");
            JsonArray api_mst_mapbgm = api_data.getAsJsonArray("api_mst_mapbgm");
            KcSubtitleUtils.buildShipGraph(api_mst_ship);
            KcSubtitleUtils.buildMapBgmGraph(api_mst_mapbgm);
            for (JsonElement item : api_mst_shipgraph) {
                JsonObject ship = item.getAsJsonObject();
                String shipId = ship.get("api_id").getAsString();
                String shipFn = ship.get("api_filename").getAsString();
                filenameToShipId.put(shipFn, shipId);
            }
        } catch (IOException e) {
            KcUtils.reportException(e);
        }
    }

    private void checkPortVolume(String path) {
        if (path.contains("/api_port/port")) {
            currentMapId = 0;
            Log.e("GOTO", "run executor");
            executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(portVolumeCheckRunnable, 0, 1, TimeUnit.SECONDS);
        } else if (path.contains("/api")) {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
        }
    }

    private JsonObject checkResourceUpdate(Uri source) {
        JsonObject update_info = new JsonObject();
        String version = "";
        boolean is_last_modified = false;

        String path = source.getPath();
        if (source.getQueryParameterNames().contains("version")) {
            version = source.getQueryParameter("version");
            if (version == null) version = "";
            if (version.length() == 0) is_last_modified = true;
        } else {
            is_last_modified = true;
        }

        String version_tb = versionTable.getValue(path);
        boolean update_flag = version_tb == null || !version_tb.equals(version);
        update_info.addProperty("version", is_last_modified ? version_tb : version);
        update_info.addProperty("is_last_modified", is_last_modified);
        update_info.addProperty("update_flag", update_flag);
        Log.e("GOTO-R", "check resource " + path + ": " + version);
        Log.e("GOTO-R", update_info.toString());

        return update_info;
    }

    private void updateCurrentMapInfo(String path, String filename) {
        if (path.contains("/api_port/port")) {
            currentMapId = 0;
            currentBattleBgmId = 0;
        } else {
            if (path.contains("/kcs2/resources/map/") && path.contains(".json")) {
                String[] map_info = path.replace("/kcs2/resources/map/", "").split("/");
                int world = Integer.parseInt(map_info[0]);
                int map = Integer.parseInt(map_info[1].split("_")[0]);
                Log.e("GOTO", "wm: " + world + "-" + map);
                currentMapId = world * 10 + map;
            }
            if (path.contains("/kcs2/resources/bgm/battle/")) {
                int bgm_no = Integer.parseInt(filename.replace("mp3", "").split("_")[0]);
                currentBattleBgmId = bgm_no;
                Log.e("GOTO", "battle_bgm_id " + currentBattleBgmId);
            }
        }
    }

    private WebResourceResponse processImageDataResource(JsonObject file_info, JsonObject update_info, int resource_type) {
        String version = update_info.get("version").getAsString();
        boolean is_last_modified = update_info.get("is_last_modified").getAsBoolean();
        boolean update_flag = update_info.get("update_flag").getAsBoolean();
        String last_modified = is_last_modified ? version : null;

        String path = file_info.get("path").getAsString();
        String resource_url = file_info.get("full_url").getAsString();
        String out_file_path = file_info.get("out_file_path").getAsString();

        File file = new File(out_file_path);
        if (update_flag) {
            String result = downloadResource(resourceClient, resource_url, last_modified, file);
            String new_value = version;
            if (new_value.length() == 0 || VersionDatabase.isDefaultValue(new_value)) new_value = result;
            if (result == null) {
                Log.e("GOTO", "return null: " + path + " " + new_value);
                return null;
            } else if (result.equals("304")) {
                Log.e("GOTO", "load cached resource: " + path + " " + new_value);
            } else {
                Log.e("GOTO", "cache resource: " + path + " " + new_value);
                versionTable.putValue(path, new_value);
            }
        } else {
            Log.e("GOTO", "load cached resource: " + path + " " + version);
        }

        try {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            Log.e("GOTO" , out_file_path + " " + is.available());
            if (ResourceProcess.isImage(resource_type)) {
                return new WebResourceResponse("image/png", "utf-8", is);
            } else if (ResourceProcess.isJson(resource_type)) {
                return new WebResourceResponse("application/json", "utf-8", is);
            }
        } catch (IOException e) {
            KcUtils.reportException(e);
        }
        return null;
    }

    private WebResourceResponse processScriptFile(JsonObject file_info) throws IOException {
        String url = file_info.get("url").getAsString();
        if (url.contains("kcs2/js/main.js")) {
            InputStream in = new BufferedInputStream(new URL(url).openStream());
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            in.close();

            byte[] byteArray = buffer.toByteArray();
            String main_js = patchMainScript(new String(byteArray, StandardCharsets.UTF_8));
            InputStream is = new ByteArrayInputStream(main_js.getBytes());
            return new WebResourceResponse("application/javascript", "utf-8", is);
        } else {
            return null;
        }
    }

    private WebResourceResponse processAudioFile(JsonObject file_info, JsonObject update_info) throws IOException, ParseException {
        String url = file_info.get("url").getAsString();
        String version = update_info.get("version").getAsString();
        boolean is_last_modified = update_info.get("is_last_modified").getAsBoolean();
        boolean update_flag = update_info.get("update_flag").getAsBoolean();
        String last_modified = is_last_modified ? version : null;

        String path = file_info.get("path").getAsString();
        String resource_url = file_info.get("full_url").getAsString();
        String out_file_path = file_info.get("out_file_path").getAsString();

        File file = new File(out_file_path);
        if (update_flag) {
            String result = downloadResource(resourceClient, resource_url, last_modified, file);
            String new_value = version;
            if (new_value.length() == 0 || VersionDatabase.isDefaultValue(new_value)) new_value = result;
            if (result == null) {
                Log.e("GOTO", "return null: " + path + " " + new_value);
                return null;
            } else if (result.equals("304")) {
                Log.e("GOTO", "load cached resource: " + path + " " + new_value);
            } else {
                Log.e("GOTO", "cache resource: " + path + " " + new_value);
                versionTable.putValue(path, new_value);
            }
        } else {
            Log.e("GOTO", "load cached resource: " + path + " " + version);
        }

        if (url.contains("resources/se")) browserPlayer.play(PLAYER_SE, file);
        else if (url.contains("/kcs/sound/kc")) {
            String info = path.replace("/kcs/sound/kc", "").replace(".mp3", "");
            String[] fn_code = info.split("/");
            String voiceline = "";
            String voice_filename = fn_code[0];
            String voice_code = fn_code[1];
            String ship_id = voice_filename;
            if (filenameToShipId.containsKey(voice_filename)) {
                ship_id = filenameToShipId.get(voice_filename);
                voiceline = KcSubtitleUtils.getVoiceLineByFilename(ship_id, voice_code);
            } else {
                voiceline = KcSubtitleUtils.getVoiceLineByFilename(voice_filename, voice_code);
            }
            Log.e("GOTO", "file info: " + info);
            Log.e("GOTO", "voiceline: " + String.valueOf(voiceline));
            int voiceline_value = Integer.parseInt(voiceline);
            if (voiceline_value >= 30 && voiceline_value <= 53) { // hourly voiceline
                Date now = new Date();
                String voiceline_time = String.format(Locale.US, "%02d:00:00", voiceline_value - 30);
                @SuppressLint("SimpleDateFormat") SimpleDateFormat time_fmt = new SimpleDateFormat("HH:mm:ss");
                Date time_src = time_fmt.parse(time_fmt.format(now));
                Date time_tgt = time_fmt.parse(voiceline_time);
                long diff_msec = time_tgt.getTime() - time_src.getTime();
                if (voiceline_value == 30) diff_msec += 86400000;
                //KcSubtitleUtils.setHourlyVoiceInfo(file.getAbsolutePath(), ship_id, voiceline);
                Runnable r = new VoiceSubtitleRunnable(file.getAbsolutePath(), ship_id, voiceline);
                shipVoiceHandler.removeCallbacks(r);
                shipVoiceHandler.postDelayed(r, diff_msec);
                Log.e("GOTO", "playHourVoice after: " + diff_msec + " msec");
            } else {
                setSubtitle(ship_id, voiceline);
                if (ship_id.equals("9999") && voiceline_value >= 411 && voiceline_value <= 424) {
                    if (BrowserSoundPlayer.ismute())
                        return new WebResourceResponse("audio/mpeg", "binary", getEmptyStream());
                    else return null;
                }
                if (voiceline_value / 10 == 14 || voiceline_value / 10 == 24 || voiceline_value / 10 == 34) {
                    if (BrowserSoundPlayer.ismute())
                        return new WebResourceResponse("audio/mpeg", "binary", getEmptyStream());
                    else return null;
                }
                if (ship_id.equals("9998") && false) { // temp code: play abyssal sound from browser
                    if (BrowserSoundPlayer.ismute())
                        return new WebResourceResponse("audio/mpeg", "binary", getEmptyStream());
                    return null;
                } else {
                    browserPlayer.play(PLAYER_VOICE, file);
                }
            }
        } else if (url.contains("/voice/titlecall_")) {
            String info = path.replace("/kcs2/resources/voice/", "").replace(".mp3", "");
            titlePath.add(info);
            titleFiles.add(file);
            if (titleFiles.size() == 2) {
                String[] first = titlePath.get(0).split("/");
                browserPlayer.play(PLAYER_VOICE, titleFiles.get(0));
                setSubtitle(first[0], first[1]);

                String[] second = titlePath.get(1).split("/");
                String next_subtitle = KcSubtitleUtils.getQuoteString(second[0], second[1]).get("0").getAsString();
                SubtitleRunnable sr_next = new SubtitleRunnable(next_subtitle);
                browserPlayer.setNextPlay(PLAYER_VOICE, titleFiles.get(1), sr_next);
            }
        } else if (url.contains("resources/bgm")) {
            browserPlayer.stop(PLAYER_BGM);
            browserPlayer.play(PLAYER_BGM, file);
        }
        return new WebResourceResponse("audio/mpeg", "binary", getEmptyStream());
    }

    private void checkSpecialSubtitleMode() {
        try {
            String voice_url = "http://52.55.91.44/gotobrowser/sub_special";
            Request voiceCodeRequest = new Request.Builder().url(voice_url)
                    .header("Referer", "goto/webkit").build();
            Response voice_special = resourceClient.newCall(voiceCodeRequest).execute();
            if (voice_special.body() != null) {
                String voice_special_code = voice_special.body().string();
                KcSubtitleUtils.specialVoiceCode = voice_special_code.trim();
                Log.e("GOTO", "special_voice: " + voice_special_code);
            }
        } catch (IOException e) {
            KcUtils.reportException(e);
        }
    }

    private boolean checkBattleMode(String path, boolean current_mode) {
        if (path.contains("api_port")) {
            return false;
        } else {
            return current_mode
                    || path.contains("api_req_battle")
                    || path.contains("api_req_map")
                    || path.contains("api_req_practice");
        }
    }

    private boolean checkPracticeMode(String path, boolean current_mode) {
        if (path.contains("api_port")) {
            return false;
        } else {
            return current_mode || path.contains("/kcs2/img/prac/prac_main");
        }
    }

    private WebResourceResponse getOoiSheetFromAsset() {
        try {
            AssetManager as = context.getAssets();
            InputStream is = as.open("ooi.css");
            return new WebResourceResponse("text/css", "utf-8", is);
        } catch (IOException e) {
            return null;
        }
    }

    private String patchMainScript(String main_js) {
        // LABS Targeting
        main_js = main_js.replaceAll(
                "this\\._panel\\.on\\(a\\.EventType\\.MOUSEOVER,this\\._onMouseOver\\)",
                "this._panel.on(a.EventType.MOUSEDOWN,this._onMouseOver)");
        main_js = main_js.replaceAll(
                "this\\._panel\\.off\\(a\\.EventType\\.MOUSEOVER,this\\._onMouseOver\\)",
                "this._panel.off(a.EventType.MOUSEDOWN,this._onMouseOver)");
        // preset check
        main_js = main_js.replaceAll("expandButton\\.addListener\\(r\\.EventType\\.MOUSEOVER",
                "expandButton.addListener(r.EventType.MOUSEDOWN");
        main_js = main_js.replaceAll("expandButton\\.addListener\\(r\\.EventType\\.MOUSEOUT",
                "expandButton.addListener(r.EventType.CLICK");
        main_js = main_js.replaceAll("on\\(s\\.EventType\\.MOUSEOUT, i\\._onMouseOut\\)",
                "on(s.EventType.CLICK, i._onMouseOut)");
        main_js = main_js.replaceAll("on\\(s\\.EventType\\.MOUSEOVER, i\\._onMouseOver\\)",
                "on(s.EventType.MOUSEDOWN, i._onMouseOver)");
        return main_js;
    }

    private Runnable portVolumeCheckRunnable = () -> {
        String cookie = CookieManager.getInstance().getCookie(currentCookieHost);
        String[] data = cookie.split(";");
        for (String item : data) {
            String[] row = item.trim().split("=");
            if (!row[1].matches("[0-9]+")) continue;
            String key = row[0];
            float value = (float) Integer.parseInt(row[1]) / 100;
            if (key.equals("vol_bgm")) {
                browserPlayer.setVolume(PLAYER_BGM, value);
            }
            if (key.equals("vol_voice")) {
                browserPlayer.setVolume(PLAYER_VOICE, value);
            }
            if (key.equals("vol_se")) {
                browserPlayer.setVolume(PLAYER_SE, value);
            }
        }
    };

    // Reference: https://github.com/KC3Kai/KC3Kai/blob/master/src/library/modules/Translation.js
    private Runnable clearSubtitle = new Runnable() {
        @Override
        public void run() {
            subtitleText.setText("");
        }
    };

    private void setSubtitle(String id, String code) {
        if (activity.isCaptionAvailable()) {
            shipVoiceHandler.removeCallbacksAndMessages(null);
            JsonObject subtitle = KcSubtitleUtils.getQuoteString(id, code);
            Log.e("GOTO", subtitle.toString());
            for (String key : subtitle.keySet()) {
                String start_time = key.split(",")[0];
                if (Pattern.matches("[0-9]+", start_time)) {
                    String text = subtitle.get(key).getAsString();
                    int delay = Integer.parseInt(start_time);
                    SubtitleRunnable sr = new SubtitleRunnable(text);
                    shipVoiceHandler.postDelayed(sr, delay);
                }
            }
        }
    }

    class SubtitleRunnable implements Runnable {
        String subtitle_text = "";

        SubtitleRunnable(String text) {
            subtitle_text = text;
        }

        @Override
        public void run() {
            activity.runOnUiThread(() -> {
                clearSubHandler.removeCallbacks(clearSubtitle);
                if (activity.isSubtitleAvailable()) {
                    subtitle_text = subtitle_text.replace("<br>", "\n");
                    subtitle_text = subtitle_text.replace("<br />", "\n");
                    subtitle_text = subtitle_text.replace("<br>", "\n");
                } else {
                    subtitle_text = context.getString(R.string.no_subtitle_file);
                }

                if (activity.isCaptionAvailable()) {
                    subtitleText.setText(subtitle_text);
                }
                int delay = KcSubtitleUtils.getDefaultTiming(subtitle_text);
                clearSubHandler.postDelayed(clearSubtitle, delay);
            });
        }
    }

    class VoiceSubtitleRunnable implements Runnable {
        String path, ship_id, voiceline;

        VoiceSubtitleRunnable(String path, String ship_id, String voiceline) {
            this.ship_id = ship_id;
            this.path = path;
            this.voiceline = voiceline;
        }

        @Override
        public void run() {
            if (path != null) {
                if (!activity.isBrowserPaused()) {
                    File file = new File(path);
                    browserPlayer.play(PLAYER_VOICE, file);
                }
            }
            setSubtitle(ship_id, voiceline);
        }
    }
}
