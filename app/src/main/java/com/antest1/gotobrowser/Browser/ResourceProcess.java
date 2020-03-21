package com.antest1.gotobrowser.Browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.widget.TextView;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.KcSubtitleUtils;
import com.google.gson.JsonObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.antest1.gotobrowser.Constants.ALTER_GADGET_URL;
import static com.antest1.gotobrowser.Constants.CAPTURE_LISTEN;
import static com.antest1.gotobrowser.Constants.GADGET_URL;
import static com.antest1.gotobrowser.Constants.MUTE_LISTEN;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET;
import static com.antest1.gotobrowser.Constants.PREF_BROADCAST;
import static com.antest1.gotobrowser.Constants.PREF_FONT_PREFETCH;
import static com.antest1.gotobrowser.Constants.REQUEST_BLOCK_RULES;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.downloadResource;
import static com.antest1.gotobrowser.Helpers.KcUtils.getEmptyStream;

public class ResourceProcess {
    private static final int RES_IMAGE = 0b000001;
    private static final int RES_AUDIO = 0b000010;
    private static final int RES_JSON  = 0b000100;
    private static final int RES_JS    = 0b001000;
    private static final int RES_FONT  = 0b010000;
    private static final int RES_KCSAPI = 0b100000;
    private static final int CACHE_MAX = 60;

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
    public static boolean isFont(int state) {
        return (state & RES_FONT) > 0;
    }
    public static boolean isKcsApi(int state) {
        return (state & RES_KCSAPI) > 0;
    }

    private BrowserActivity activity;
    private Context context;
    private VersionDatabase versionTable;
    private final OkHttpClient resourceClient = new OkHttpClient();
    SharedPreferences sharedPref;

    private List<String> titlePath = new ArrayList<>();
    private List<File> titleFiles = new ArrayList<>();

    private TextView subtitleText;
    private final Handler shipVoiceHandler = new Handler();
    private final Handler clearSubHandler = new Handler();
    boolean prefAlterGadget = false;
    
    ResourceProcess(BrowserActivity activity) {
        this.activity = activity;
        context = activity.getApplicationContext();
        versionTable = new VersionDatabase(context, null, VERSION_TABLE_VERSION);
        sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);
        prefAlterGadget = sharedPref.getBoolean(PREF_ALTER_GADGET, false);
        subtitleText = activity.findViewById(R.id.subtitle_view);
        subtitleText.setOnClickListener(v -> clearSubHandler.postDelayed(clearSubtitle, 250));
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
        if ((url.contains("/js/") || url.contains("/script/")) && url.contains(".js")) {
            state |= RES_JS;
        }
        if (url.contains(".woff2")) {
            state |= RES_FONT;
        }
        if (url.contains("kcsapi") && !url.contains("osapi.dmm.com")) {
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
        boolean is_font = ResourceProcess.isFont(resource_type);
        boolean is_kcsapi = ResourceProcess.isKcsApi(resource_type);

        if (checkBlockedContent(url)) return getEmptyResponse();
        if (url.contains("ooi.css")) return getOoiSheetFromAsset();
        if (url.contains("tweenjs-0.6.2")) return getTweenJs();
        if (url.contains("gadget_html5/script/rollover.js")) return getMuteInjectedRolloverJs();
        if (url.contains("gadget_html5/js/kcs_cda.js")) return getInjectedKcaCdaJs();
        if (url.contains("kcscontents/css/common.css")) return getBlackBackgroundSheet();
        if (prefAlterGadget) {
            if (url.contains("html/maintenance.html")) return getMaintenanceFiles(false);
            if (url.contains("html/maintenance.png")) return getMaintenanceFiles(true);
        }
        if (resource_type == 0) return null;

        JsonObject file_info = getPathAndFileInfo(source);
        String path = file_info.get("path").getAsString();
        String filename = file_info.get("filename").getAsString();
        String filepath = file_info.get("out_file_path").getAsString();

        try {
            if (path != null && filename != null) {
                Log.e("GOTO", source.getPath());
                if (filename.equals("version.json") || filename.contains("index.php")) {
                    titlePath.clear();
                    titleFiles.clear();
                    return null;
                }

                // load game data
                if (is_kcsapi && path.contains("/api_start2")) {
                    // checkSpecialSubtitleMode();
                    return null;
                }

                JsonObject update_info = checkResourceUpdate(source);
                if (is_image || is_json) return processImageDataResource(file_info, update_info, resource_type);
                if (is_js) return processScriptFile(file_info);
                if (is_audio) return processAudioFile(file_info, update_info);
                if (is_font && sharedPref.getBoolean(PREF_FONT_PREFETCH, true)) return getFontFile(filename);
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

    private WebResourceResponse getEmptyResponse() {
        return new WebResourceResponse("text/css", "utf-8", getEmptyStream());
    }

    private WebResourceResponse getBlackBackgroundSheet() {
        String replace_css = "#globalNavi, #contentsWrap {display:none;} body {background-color: black;}";
        InputStream is = new ByteArrayInputStream(replace_css.getBytes());
        return new WebResourceResponse("text/css", "utf-8", is);
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

    private WebResourceResponse processImageDataResource(JsonObject file_info, JsonObject update_info, int resource_type) {
        String version = update_info.get("version").getAsString();
        boolean is_last_modified = update_info.get("is_last_modified").getAsBoolean();
        boolean update_flag = update_info.get("update_flag").getAsBoolean();
        String last_modified = is_last_modified ? version : null;

        String path = file_info.get("path").getAsString();
        String resource_url = file_info.get("full_url").getAsString();
        String out_file_path = file_info.get("out_file_path").getAsString();
        try {
            File file = getImageFile(out_file_path);
            Log.e("GOTO", "requested: " + file.getPath());
            if (update_flag || !file.exists()) {
                String result = downloadResource(resourceClient, resource_url, last_modified, file);
                String new_value = version;
                if (new_value.length() == 0 || VersionDatabase.isDefaultValue(new_value)) new_value = result;
                if (result == null) {
                    Log.e("GOTO", "return null: " + path + " " + new_value);
                    return null;
                } else if (result.equals("304")) {
                    Log.e("GOTO", "load 304 resource: " + path + " " + new_value);
                } else {
                    Log.e("GOTO", "cache resource: " + path + " " + new_value);
                    versionTable.putValue(path, new_value);
                }
            } else {
                Log.e("GOTO", "load cached resource: " + file.getPath() + " " + version);
            }

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
        boolean broadcast_mode = sharedPref.getBoolean(PREF_BROADCAST, false);
        String url = file_info.get("url").getAsString();
        if (prefAlterGadget && url.contains("gadget_html5")) {
            url = url.replace(GADGET_URL, ALTER_GADGET_URL);
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
            InputStream is = new ByteArrayInputStream(byteArray);
            return new WebResourceResponse("application/javascript", "utf-8", is);
        }

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
            String main_js = patchMainScript(new String(byteArray, StandardCharsets.UTF_8), broadcast_mode);
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
            if (new_value.length() == 0 || VersionDatabase.isDefaultValue(new_value))
                new_value = result;
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

        String voicesize = String.valueOf(file.length());
        if (url.contains("/kcs/sound/kc")) {
            String info = path.replace("/kcs/sound/kc", "").replace(".mp3", "");
            String[] fn_code = info.split("/");
            String voiceline = "";
            String voice_filename = fn_code[0];
            String voice_code = fn_code[1];
            String ship_id = voice_filename;
            if (KcSubtitleUtils.filenameToShipId.containsKey(voice_filename)) {
                ship_id = KcSubtitleUtils.filenameToShipId.get(voice_filename);
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
                Runnable r = new VoiceSubtitleRunnable(ship_id, voiceline, voicesize);
                shipVoiceHandler.removeCallbacks(r);
                shipVoiceHandler.postDelayed(r, diff_msec);
                Log.e("GOTO", "playHourVoice after: " + diff_msec + " msec");
            } else {
                setSubtitle(ship_id, voiceline, voicesize);
            }
        } else if (url.contains("/voice/titlecall_")) {
            String info = path.replace("/kcs2/resources/voice/", "").replace(".mp3", "");
            String[] fn_code = info.split("/");
            setSubtitle(fn_code[0], fn_code[1], voicesize);
        }

        InputStream is = new BufferedInputStream(new FileInputStream(file));
        return new WebResourceResponse("audio/mpeg", "binary", is);
    }

    private File getImageFile(String path) {
        return new File(path);
    }

    /*
    private void checkSpecialSubtitleMode() {
        try {
            String voice_url = "http://antest1.cf/gotobrowser/sub_special";
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
    }*/

    private WebResourceResponse getOoiSheetFromAsset() {
        try {
            AssetManager as = context.getAssets();
            InputStream is = as.open("ooi.css");
            return new WebResourceResponse("text/css", "utf-8", is);
        } catch (IOException e) {
            return null;
        }
    }

    private WebResourceResponse getMuteInjectedRolloverJs() {
        try {
            AssetManager as = context.getAssets();
            InputStream is = as.open("rollover.js");
            return new WebResourceResponse("application/x-javascript", "utf-8", is);
        } catch (IOException e) {
            return null;
        }
    }

    private WebResourceResponse getInjectedKcaCdaJs() {
        try {
            AssetManager as = context.getAssets();
            InputStream is = as.open("kcs_cda.js");
            return new WebResourceResponse("application/x-javascript", "utf-8", is);
        } catch (IOException e) {
            return null;
        }
    }

    private WebResourceResponse getTweenJs() {
        try {
            AssetManager as = context.getAssets();
            InputStream is = as.open("tweenjs-0.6.2.min.js");
            return new WebResourceResponse("application/x-javascript", "utf-8", is);
        } catch (IOException e) {
            return null;
        }
    }

    private WebResourceResponse getFontFile(String filename) {
        try {
            AssetManager as = context.getAssets();
            InputStream is = as.open(filename);
            return new WebResourceResponse("application/octet-stream", "utf-8", is);
        } catch (IOException e) {
            return null;
        }
    }

    private WebResourceResponse getMaintenanceFiles(boolean is_image) {
        try {
            AssetManager as = context.getAssets();
            if (is_image) {
                InputStream is = as.open("maintenance.png");
                return new WebResourceResponse("image/png", "utf-8", is);
            } else {
                InputStream is = as.open("maintenance.html");
                return new WebResourceResponse("text/html", "utf-8", is);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private String patchMainScript(String main_js, boolean broadcast_mode) {
        // manage bgm loading strategy with global mute variable for audio focus issue
        if (activity.isMuteMode()) {
            main_js = "var global_mute=1;Howler.mute(true);\n".concat(main_js);
        } else {
            main_js = "var global_mute=0;Howler.mute(false);\n".concat(main_js);
        }
        main_js = "var gb_h=null;\nfunction add_bgm(b){b.onend=function(){(global_mute||gb_h.volume()==0)&&(gb_h.unload(),console.log('unload'))};global_mute&&(b.autoplay=false);gb_h=new Howl(b);return gb_h;}\n" + main_js;

        // main_js = main_js.replaceAll("new Howl\\(d\\)", "add_bgm(d)");
        Pattern howlPattern = Pattern.compile("new Howl\\((_0x\\w+)\\)");
        Matcher howlPatternMatcher = howlPattern.matcher(main_js);
        boolean howl_found = howlPatternMatcher.find();
        if (howl_found) {
            String _instance = howlPatternMatcher.group(1);
            main_js = main_js.replaceAll("new Howl\\(".concat(_instance).concat("\\)"), "add_bgm(".concat(_instance).concat(")"));
        }

        // main_js = main_js.replaceAll("function\\(_0x17657d\\)\\{", "function(_0x17657d){GotoBrowser.kcs_axios_error(_0x17657d['stack']);");
        Pattern axiosPattern = Pattern.compile("axios\\[.{10,300}\\(function\\(\\w+.{10,300}\\(function\\((\\w+)\\)\\{");
        Matcher axiosPatternMatcher = axiosPattern.matcher(main_js);
        boolean axios_found = axiosPatternMatcher.find();
        if (axios_found) {
            String _failureCode = axiosPatternMatcher.group(0);
            String _var = axiosPatternMatcher.group(1);
            main_js = main_js.replace(_failureCode, _failureCode.concat("GotoBrowser.kcs_axios_error(".concat(_var).concat("['stack']);")));
        }

        // Low Frame Rate Issue
        main_js = main_js.replaceFirst(
                "(createjs(?:\\[\\w+\\('\\w+'\\)\\]){2})\\=createjs(?:\\[\\w+\\('\\w+'\\)\\]){2},",
                "$1=createjs.Ticker.RAF,");

        // Prevent Possible Item Purchase Crash
        main_js = main_js.replaceFirst(
                "function\\((\\w+)\\)\\{(window\\[\\w+\\('\\w+'\\)]\\(\\w+\\('\\w+'\\),\\w+\\[\\w+\\('\\w+'\\)]\\);)",
                "function($1){if(typeof $1['data']=='number')$2");

        // handling port button behavior (sally)
        // handling port button behavior (others)
        // main_js = main_js.replaceAll("_.EventType\\.MOUSEUP,this\\._onMouseUp", "_.EventType.MOUSEDOWN,this._onMouseUp");
        // main_js = main_js.replaceAll("c.EventType\\.MOUSEUP,this\\._onMouseUp", "c.EventType.MOUSEDOWN,this._onMouseUp");

        String mouseup_detect_code = "=!0x1,".concat(TextUtils.join(",", Collections.nCopies(4,
                "this(?:\\[\\w+\\('\\w+'\\)]){2}\\((\\w+\\[\\w+\\('\\w+'\\)\\])(\\[\\w+\\('\\w+'\\)\\]),this(\\[\\w+\\('\\w+'\\)\\])\\)")));
        Pattern buttonMousePattern = Pattern.compile(mouseup_detect_code);
        Matcher buttonPatternMatcher = buttonMousePattern.matcher(main_js);
        while (buttonPatternMatcher.find()) {
            try {
                String _EventType = buttonPatternMatcher.group(1);
                String _propMOUSEDOWN = buttonPatternMatcher.group(8);
                String _propMOUSEUP = buttonPatternMatcher.group(11);
                String _onMouseUp = buttonPatternMatcher.group(12);
                String regexEventType = _EventType.concat(_propMOUSEUP).concat(",this").concat(_onMouseUp)
                        .replace("(", "\\(").replace(")", "\\)").replace("[", "\\[");
                String replaceEventType = _EventType.concat(_propMOUSEDOWN).concat(",this").concat(_onMouseUp);
                main_js = main_js.replaceAll(regexEventType, replaceEventType);
            } catch (NullPointerException e) {
                KcUtils.reportException(e);;
                // do nothing
            }
        }

        // Simulate mouse hover effects by dispatching new custom events "touchover" and "touchout"
        main_js +=  "function patchInteractionManager () {\n" +
                    "  var proto = PIXI.interaction.InteractionManager.prototype;\n" +
                    "\n" +
                    "  function extendMethod (method, extFn) {\n" +
                    "    var old = proto[method];\n" +
                    "    proto[method] = function () {\n" +
                    "      old.call(this, ...arguments);\n" +
                    "      extFn.call(this, ...arguments);\n" +
                    "    };\n" +
                    "  }\n" +
                    "  proto.update = mobileUpdate;\n" +
                    "\n" +
                    "  function mobileUpdate(deltaTime) {\n" +
                    "    if (!this.interactionDOMElement) {\n" +
                    "      return;\n" +
                    "    }\n" +
                         // Only trigger "touchout" when there is another object start "touchover", do nothing when "touchend"
                         // So that alert bubbles persist after a simple tap, do not disappear when the finger leaves
                    "    if (this.eventData.data && (this.eventData.type == 'touchmove' || this.eventData.type == 'touchstart')) {\n" +
                    "      window.__eventData = this.eventData;\n" +
                    "      this.processInteractive(this.eventData, this.renderer._lastObjectRendered, this.processTouchOverOut, true);\n" +
                    "    }\n" +
                    "  }\n" +
                    "\n" +
                    "  extendMethod('processTouchMove', function(displayObject, hit) {\n" +
                    "      this.processTouchOverOut('processTouchMove', displayObject, hit);\n" +
                    "  });\n" +
                    "  extendMethod('processTouchStart', function(displayObject, hit) {\n" +
                    "      this.processTouchOverOut('processTouchStart', displayObject, hit);\n" +
                    "  });\n" +
                    "\n" +
                    "  proto.processTouchOverOut = function (interactionEvent, displayObject, hit) {\n" +
                    "    if(hit) {\n" +
                    "      if(!displayObject.__over && displayObject._events.touchover) {\n" +
                    "        if (displayObject.parent._onClickAll2) return;\n" +
                    "        displayObject.__over = true;\n" +
                    "        proto.dispatchEvent( displayObject, 'touchover', window.__eventData);\n" +
                    "      }\n" +
                    "    } else {\n" +
                             // Only trigger "touchout" when user starts touching another object
                    "        if(displayObject.__over && displayObject._events.touchover && interactionEvent.target != displayObject) {\n" +
                    "            displayObject.__over = false;\n" +
                    "            proto.dispatchEvent( displayObject, 'touchout', window.__eventData);\n" +
                    "        }\n" +
                    "    }\n" +
                    "  };\n" +
                    "}\n" +
                    "patchInteractionManager();";

        // Rename the original "mouseout" and "mouseover" event name to custom names for objects to listen on
        // Reusing original names will cause a lot of conflict issues
        //main_js = main_js.replace("over:n.pointer?\"pointerover\":\"mouseover\"", "over:\"touchover\"");
        //main_js = main_js.replace("out:n.pointer?\"pointerout\":\"mouseout\"", "out:\"touchout\"");
        main_js = main_js.replaceFirst("'over':\\w+\\[\\w+\\('\\w+'\\)]\\?\\w+\\('\\w+'\\):\\w+\\('\\w+'\\)", "'over':'touchover'");
        main_js = main_js.replaceFirst("'out':\\w+\\[\\w+\\('\\w+'\\)]\\?\\w+\\('\\w+'\\):\\w+\\('\\w+'\\)", "'out':'touchout'");

        main_js = main_js.concat(MUTE_LISTEN);
        main_js = main_js.concat(CAPTURE_LISTEN);
        main_js = main_js.concat("\n").concat(KcsInterface.AXIOS_INTERCEPT_SCRIPT);
        return main_js;
    }

    // Reference: https://github.com/KC3Kai/KC3Kai/blob/master/src/library/modules/Translation.js
    private Runnable clearSubtitle = new Runnable() {
        @Override
        public void run() {
            subtitleText.setText("");
        }
    };

    private void setSubtitle(String id, String code, String size) {
        if (activity.isCaptionAvailable()) {
            shipVoiceHandler.removeCallbacksAndMessages(null);
            JsonObject subtitle = KcSubtitleUtils.getQuoteString(id, code, size);
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
        String ship_id, voiceline, voicesize;

        VoiceSubtitleRunnable(String ship_id, String voiceline, String voicesize) {
            this.ship_id = ship_id;
            this.voiceline = voiceline;
            this.voicesize = voicesize;
        }

        @Override
        public void run() {
            setSubtitle(ship_id, voiceline, voicesize);
        }
    }
}
