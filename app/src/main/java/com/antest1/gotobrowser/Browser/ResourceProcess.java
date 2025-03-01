package com.antest1.gotobrowser.Browser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebResourceResponse;
import android.widget.TextView;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.Activity.EntranceActivity;
import com.antest1.gotobrowser.Helpers.CritPatcher;
import com.antest1.gotobrowser.Helpers.FpsPatcher;
import com.antest1.gotobrowser.Helpers.K3dPatcher;
import com.antest1.gotobrowser.Helpers.KcEnUtils;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.KenPatcher;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.SubtitleData;
import com.antest1.gotobrowser.Subtitle.SubtitleProviderUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;

import static com.antest1.gotobrowser.Constants.CAPTURE_LISTEN;
import static com.antest1.gotobrowser.Constants.DEFAULT_ALTER_GADGET_URL;
import static com.antest1.gotobrowser.Constants.MUTE_LISTEN;
import static com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_ENDPOINT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD_URL;
import static com.antest1.gotobrowser.Constants.PREF_DOWNLOAD_RETRY;
import static com.antest1.gotobrowser.Constants.PREF_FONT_PREFETCH;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN;
import static com.antest1.gotobrowser.Constants.PREF_SILENT;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.REQUEST_BLOCK_RULES;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcEnUtils.GetMD5HashOfString;
import static com.antest1.gotobrowser.Helpers.KcEnUtils.dirMD5;
import static com.antest1.gotobrowser.Helpers.KcUtils.downloadResource;
import static com.antest1.gotobrowser.Helpers.KcUtils.getEmptyStream;
import static com.antest1.gotobrowser.Helpers.KcUtils.getExpireInfoFromCacheControl;
import static com.antest1.gotobrowser.Helpers.KcUtils.getStringFromException;

public class ResourceProcess {
    private static final int RES_IMAGE  = 0b0000001;
    private static final int RES_AUDIO  = 0b0000010;
    private static final int RES_JSON   = 0b0000100;
    private static final int RES_JS     = 0b0001000;
    private static final int RES_FONT   = 0b0010000;
    private static final int RES_CSS    = 0b0100000;
    private static final int RES_KCSAPI = 0b1000000;

    private static String userAgent;

    public static boolean isImage(int state) { return (state & RES_IMAGE) > 0; }
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
    public static boolean isStylesheet(int state) {
        return (state & RES_CSS) > 0;
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

    boolean prefAlterGadget, isGadgetUrlReplaceMode, prefModKantaiEn;
    String alterEndpoint;

    ResourceProcess(BrowserActivity activity) {
        this.activity = activity;
        context = activity.getApplicationContext();
        versionTable = new VersionDatabase(context, null, VERSION_TABLE_VERSION);
        sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);
        prefAlterGadget = sharedPref.getBoolean(PREF_ALTER_GADGET, false);
        isGadgetUrlReplaceMode = sharedPref.getString(PREF_ALTER_METHOD, "")
                .equals(PREF_ALTER_METHOD_URL);
        alterEndpoint = sharedPref.getString(PREF_ALTER_ENDPOINT, DEFAULT_ALTER_GADGET_URL);
        prefModKantaiEn = sharedPref.getBoolean(PREF_MOD_KANTAIEN, false);
        subtitleText = activity.findViewById(R.id.subtitle_view);
        subtitleText.setOnClickListener(v -> clearSubHandler.postDelayed(clearSubtitle, 250));
    }

    public static String getUserAgent() {
        if (userAgent == null) return WebViewManager.USER_AGENT;
        return userAgent;
    }

    public static void setUserAgent(String agent) { userAgent = agent; }

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
        if (url.contains(".css")) {
            state |= RES_CSS;
        }
        if (url.contains("kcsapi") && !url.contains("osapi.dmm.com")) {
            state |= RES_KCSAPI;
        }
        return state;
    }

    @SuppressLint("ApplySharedPref")
    public WebResourceResponse processWebRequest(Uri source) {
        String url = source.toString();
        int resource_type = getCurrentState(url);
        if (resource_type > 0) Log.e("GOTO", url + " - " + String.valueOf(resource_type));
        boolean is_image = ResourceProcess.isImage(resource_type);
        boolean is_audio = ResourceProcess.isAudio(resource_type);
        boolean is_json = ResourceProcess.isJson(resource_type);
        boolean is_js = ResourceProcess.isScript(resource_type);
        boolean is_font = ResourceProcess.isFont(resource_type);
        boolean is_css = ResourceProcess.isStylesheet(resource_type);
        boolean is_kcsapi = ResourceProcess.isKcsApi(resource_type);

        if (checkBlockedContent(url)) return getEmptyResponse();
        if (url.contains("ooi.css")) return getOoiSheetFromAsset();
        if (url.contains("tweenjs.min.js")) return getTweenJs();
        if (url.contains("gadget_html5/script/rollover.js")) return getMuteInjectedRolloverJs();
        if (url.contains("kcscontents/css/common.css")) return getBlackBackgroundSheet();
        if (url.contains("html/maintenance.html")) return getMaintenanceFiles(false);
        if (url.contains("html/maintenance.png")) return getMaintenanceFiles(true);
        if (resource_type == 0) return null;
        if (url.contains("ooi_moe_")) return null; // Prevent OOI from caching the server name display

        if (url.contains("gadget_html5/js/kcs_cda.js")) {
            boolean ip_banned = getIpBannedStatus(url);
            if (prefAlterGadget && !ip_banned) {
                sharedPref.edit().putBoolean(PREF_ALTER_GADGET, false).commit();
            } else if (!prefAlterGadget && ip_banned) {;
                showGadgetIpServerBlockedDialog();
            }
            return getInjectedKcaCdaJs();
        }

        JsonObject file_info = getPathAndFileInfo(source);
        String path = file_info.get("path").getAsString();
        String filename = file_info.get("filename").getAsString();

        try {
            if (!path.isEmpty() && !filename.isEmpty()) {
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

                if (!is_kcsapi) {
                    JsonObject update_info = checkResourceUpdate(source);
                    if (is_image || is_json) return processImageDataResource(file_info, update_info, resource_type);
                    if (is_js) return processScriptFile(file_info);
                    if (is_audio) return processAudioFile(file_info, update_info, resource_type);
                    if (is_css) return processStylesheet(file_info);
                    if (is_font) {
                        if (sharedPref.getBoolean(PREF_FONT_PREFETCH, true)) {
                            return getFontFile(filename);
                        } else {
                            return processFontFile(file_info, update_info);
                        }
                    }
                }
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
            outputpath = KcUtils.getAppCacheFileDir(context, "/cache/");
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
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        JsonObject update_info = new JsonObject();
        String version = "";
        boolean update_flag;

        String path = source.getPath();
        if (source.getQueryParameterNames().contains("version")) {
            version = source.getQueryParameter("version");
            if (version == null) version = "";
        }

        String version_tb = versionTable.getValue(path);
        try {
            Date parsed = formatter.parse(version_tb);
            update_flag = parsed.compareTo(new Date()) < 0;
        } catch (Exception e){
            Log.e("GOTO-E", getStringFromException(e));
            if (version_tb.contains(":")) versionTable.putDefaultValue(path);
            update_flag = !version_tb.equals(version);
        }

        update_info.addProperty("version", version);
        update_info.addProperty("update_flag", update_flag);
        Log.e("GOTO-R", "check resource " + path + ": " + version);
        Log.e("GOTO-R", update_info.toString());

        return update_info;
    }

    private WebResourceResponse processImageDataResource(JsonObject file_info, JsonObject update_info, int resource_type) {
        String version = update_info.get("version").getAsString();
        boolean update_flag = update_info.get("update_flag").getAsBoolean();

        String path = file_info.get("path").getAsString();
        String resource_url = file_info.get("full_url").getAsString();
        String out_file_path = file_info.get("out_file_path").getAsString();
        String log_path = out_file_path;
        File file = getImageFile(out_file_path);

        if (!file.exists()) {
            versionTable.putDefaultValue(path);
            update_flag = true;
        }
        Log.e("GOTO", "requested: " + file.getPath());
        if (update_flag) {
            String result = downloadResource(resourceClient, resource_url, file);
            if (result == null) {
                Log.e("GOTO", "return null: " + path + " " + version);
                return promptForRetry(file_info, update_info, resource_type);
            } else {
                if (version.length() > 0 && !VersionDatabase.isDefaultValue(version)) {
                    if (result.equals("304")) {
                        Log.e("GOTO", "load 304 resource: " + path + " " + version);
                    } else {
                        Log.e("GOTO", "cache resource: " + path + " " + version);
                        versionTable.putValue(path, version);
                    }
                } else {
                    String expire_info = getExpireInfoFromCacheControl(result);
                    Log.e("GOTO-D", path + " Expires at: " + expire_info);
                    versionTable.putValue(path, getExpireInfoFromCacheControl(result));
                }
            }
        } else {
            Log.e("GOTO", "load cached resource: " + file.getPath() + " " + version);
        }

        if (KenPatcher.isPatcherEnabled()) {
            String patchedFilePath = KcUtils.getAppCacheFileDir(context, "/_patched_cache".concat(path));
            String patchFilePath = KcUtils.getAppCacheFileDir(context,
                    "/KanColle-English-Patch-KCCP-master/EN-patch".concat(path));
            File patchedFile = getImageFile(patchedFilePath);
            File patchFile = new File(patchFilePath);

            boolean usePatchedCache = false;
            if (patchFile.isDirectory()) {
                String patchStrings;
                if (new File(patchFilePath.concat("/original")).isDirectory()) {
                    patchStrings = dirMD5(patchFilePath.concat("/original")) + dirMD5(patchFilePath.concat("/patched"));
                } else {
                    patchStrings = dirMD5(patchFilePath);
                }
                String hash = GetMD5HashOfString(patchStrings);
                if (!patchedFile.exists() ||
                        update_flag ||
                        versionTable.getValue(patchFilePath) == null ||
                        !Objects.equals(versionTable.getValue(patchFilePath), hash)) {
                    versionTable.putValue(patchFilePath, hash);
                    Log.e("GOTO", "needs repatch: " + patchedFilePath + " " + hash);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        usePatchedCache = patchImage(out_file_path, patchedFilePath, patchFilePath);
                    }
                } else {
                    Log.e("GOTO", "using cached patched file: " + patchedFilePath + " " + hash);
                    usePatchedCache = true;
                }
            }
            if (usePatchedCache) {
                file = patchedFile;
                log_path = patchedFilePath;
            }
        }

        try {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            Log.e("GOTO", log_path + " " + is.available());

            String type = ResourceProcess.isImage(resource_type) ? "image/png" : "application/json";
            return new WebResourceResponse(type, "utf-8", is);
        } catch (IOException e) {
            KcUtils.reportException(e);
            // Fail to load
            return promptForRetry(file_info, update_info, resource_type);
        }
    }

    private void showGadgetIpServerBlockedDialog() {
        activity.runOnUiThread(() -> {
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Intent intent = new Intent(activity, EntranceActivity.class);
                        activity.startActivity(intent);
                        activity.finish();
                        break;
                }
                dialog.dismiss();
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.dialog_ipblock_title))
                    .setMessage(String.format(activity.getString(R.string.dialog_ipblock_message),
                            activity.getString(R.string.connection_use_alter)))
                    .setPositiveButton(activity.getString(R.string.action_ok), dialogClickListener)
                    .setCancelable(false).show();
        });
    }

    private WebResourceResponse promptForRetry(JsonObject file_info, JsonObject update_info, int resource_type) {
        boolean isRetryPromptEnabled = sharedPref.getBoolean(PREF_DOWNLOAD_RETRY, true);
        if (!isRetryPromptEnabled) {
            return null;
        }

        final AtomicReference<Boolean> cancelled = new AtomicReference<>(false);

        final CountDownLatch retryReady = new CountDownLatch(1);
        activity.runOnUiThread(() -> {
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_NEGATIVE: // no and never ask again
                        // User give up and it is ok to stop loading
                        // And change preference to never ask again
                        sharedPref.edit().putBoolean(PREF_DOWNLOAD_RETRY, false).apply();
                    default:
                    case DialogInterface.BUTTON_NEUTRAL: // no
                        // User give up and it is ok to stop loading
                        cancelled.set(true);
                    case DialogInterface.BUTTON_POSITIVE: // yes
                        // User allow retry recovery
                        // We can proceed to next iteration
                        retryReady.countDown();
                        break;
                }
                dialog.dismiss();
            };

            if (!activity.isFinishing()) {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    String path = file_info.get("path").getAsString();
                    builder.setTitle(activity.getString(R.string.dialog_retry_title))
                            .setMessage(String.format(activity.getString(R.string.dialog_retry_message), path))
                            .setPositiveButton(activity.getString(R.string.dialog_retry_yes), dialogClickListener)
                            .setNeutralButton(activity.getString(R.string.dialog_retry_no), dialogClickListener)
                            .setNegativeButton(activity.getString(R.string.dialog_retry_never), dialogClickListener)
                            .setCancelable(false).show();
                } catch (WindowManager.BadTokenException e) {
                    // invalid activity, do nothing
                }
            }
        });

        try {
            // Wait for the user choice
            retryReady.await();
        } catch (InterruptedException e) {
            // Possible exit: system interrupt
            return null;
        }

        if (cancelled.get()) {
            return null;
        } else {
            if (ResourceProcess.isImage(resource_type) || ResourceProcess.isAudio(resource_type)) {
                return processImageDataResource(file_info, update_info, resource_type);
            } else {
                return processAudioFile(file_info, update_info, resource_type);
            }
        }
    }

    private WebResourceResponse processScriptFile(JsonObject file_info) throws IOException {
        boolean silent_mode = sharedPref.getBoolean(PREF_SILENT, false);
        String url = file_info.get("url").getAsString();
        if (prefAlterGadget && isGadgetUrlReplaceMode && url.contains("gadget_html5")) {
            url = WebViewManager.replaceEndpoint(url, alterEndpoint);
            byte[] byteArray = KcUtils.downloadDataFromURL(url);
            InputStream is = new ByteArrayInputStream(byteArray);
            return new WebResourceResponse("application/javascript", "utf-8", is);
        }

        if (url.contains("kcs2/js/main.js")) {
            byte[] byteArray = KcUtils.downloadDataFromURL(url);
            String main_js = patchMainScript(new String(byteArray, StandardCharsets.UTF_8), silent_mode);
            InputStream is = new ByteArrayInputStream(main_js.getBytes());
            return new WebResourceResponse("application/javascript", "utf-8", is);
        } else {
            return null;
        }
    }

    private WebResourceResponse processStylesheet(JsonObject file_info) throws IOException {
        String url = file_info.get("url").getAsString();
        boolean is_adjustment = sharedPref.getBoolean(PREF_ADJUSTMENT, false);
        if (is_adjustment) {
            AssetManager as = context.getAssets();
            if (url.contains("kcscontents/css/import.css")) {
                InputStream game_in = as.open("game_custom.css");
                byte[] game_css = KcUtils.getBytesFromInputStream(game_in);
                InputStream is = new ByteArrayInputStream(game_css);
                return new WebResourceResponse("text/css", "utf-8", is);
            }

            if (url.contains("kcscontents/css/default.css")) return getEmptyResponse();
            if (url.contains("kcscontents/css/style.css")) return getEmptyResponse();

            if (url.contains("www.dmm.com.netgame.css")) {
                byte[] byteArray = KcUtils.downloadDataFromURL(url);
                String css = new String(byteArray, StandardCharsets.UTF_8);
                InputStream dmm_in = as.open("dmm_custom.css");
                String dmm_css = KcUtils.getStringFromInputStream(dmm_in);
                css = css.concat("\n\n").concat(dmm_css);
                InputStream is = new ByteArrayInputStream(css.getBytes());
                return new WebResourceResponse("text/css", "utf-8", is);
            }
        }
        return null;
    }

    private WebResourceResponse processAudioFile(JsonObject file_info, JsonObject update_info, int resource_type) {
        String url = file_info.get("url").getAsString();
        String version = update_info.get("version").getAsString();
        boolean update_flag = update_info.get("update_flag").getAsBoolean();

        String path = file_info.get("path").getAsString();
        String resource_url = file_info.get("full_url").getAsString();
        String out_file_path = file_info.get("out_file_path").getAsString();

        File file = new File(out_file_path);
        if (!file.exists()) {
            versionTable.putDefaultValue(path);
            update_flag = true;
        }

        if (update_flag) {
            String result = downloadResource(resourceClient, resource_url, file);
            if (result == null) {
                Log.e("GOTO", "return null: " + path + " " + version);
                return promptForRetry(file_info, update_info, resource_type);
            } else {
                if (version.length() > 0 && !VersionDatabase.isDefaultValue(version)) {
                    if (result.equals("304")) {
                        Log.e("GOTO", "load 304 resource: " + path + " " + version);
                    } else {
                        Log.e("GOTO", "cache resource: " + path + " " + version);
                        versionTable.putValue(path, version);
                    }
                } else {
                    versionTable.putValue(path, getExpireInfoFromCacheControl(result));
                }
            }
        } else {
            Log.e("GOTO", "load cached resource: " + file.getPath() + " " + version);
        }

        String voiceSize = String.valueOf(file.length());

        String subtitle_local = sharedPref.getString(PREF_SUBTITLE_LOCALE, "en");
        SubtitleData data = SubtitleProviderUtils.getSubtitleProvider(subtitle_local).getSubtitleData(url, path, voiceSize);

        if (data != null) {
            if (data.getExtraDelay() != null) {
                setSubtitleAfter(data);
            } else {
                setSubtitle(data);
            }
        }

        try {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            return new WebResourceResponse("audio/mpeg", "binary", is);
        } catch (IOException e) {
            KcUtils.reportException(e);
            // Fail to load
            return promptForRetry(file_info, update_info, resource_type);
        }
    }

    private WebResourceResponse processFontFile(JsonObject file_info, JsonObject update_info) throws IOException {
        String version = update_info.get("version").getAsString();
        boolean update_flag = update_info.get("update_flag").getAsBoolean();

        String path = file_info.get("path").getAsString();
        String resource_url = file_info.get("full_url").getAsString();
        String out_file_path = file_info.get("out_file_path").getAsString();

        File file = new File(out_file_path);
        if (!file.exists()) {
            versionTable.putDefaultValue(path);
            update_flag = true;
        }

        if (update_flag) {
            String result = downloadResource(resourceClient, resource_url, file);
            if (version.length() > 0 && !VersionDatabase.isDefaultValue(version)) {
                if (result.equals("304")) {
                    Log.e("GOTO", "load 304 resource: " + path + " " + version);
                } else {
                    Log.e("GOTO", "cache resource: " + path + " " + version);
                    versionTable.putValue(path, version);
                }
            } else {
                versionTable.putValue(path, getExpireInfoFromCacheControl(result));
            }
        } else {
            Log.e("GOTO", "load cached resource: " + file.getPath() + " " + version);
        }

        InputStream is = new BufferedInputStream(new FileInputStream(file));
        return new WebResourceResponse("application/font-woff2", "binary", is);
    }

    private void setSubtitle(SubtitleData data) {
        if (activity.isCaptionAvailable()) {
            shipVoiceHandler.removeCallbacksAndMessages(null);
            if (data != null) {
                SubtitleRunnable sr = new SubtitleRunnable(data.getText(), data.getDuration());
                shipVoiceHandler.postDelayed(sr, data.getDelay());
            }
        }
    }

    private void setSubtitleAfter(SubtitleData data) {
        Runnable r = new VoiceSubtitleRunnable(data);
        shipVoiceHandler.removeCallbacks(r);
        shipVoiceHandler.postDelayed(r, data.getExtraDelay());
        Log.e("GOTO", "playHourVoice after: " + data.getExtraDelay() + " msec");
    }

    private File getImageFile(String path) {
        return new File(path);
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

    private WebResourceResponse getMuteInjectedRolloverJs() {
        try {
            AssetManager as = context.getAssets();
            InputStream is = as.open("rollover.js");
            return new WebResourceResponse("application/x-javascript", "utf-8", is);
        } catch (IOException e) {
            return null;
        }
    }

    private boolean getIpBannedStatus(String url) {
        String result = downloadResource(resourceClient, url, null);
        Log.e("GOTO", "IpBannedStatus: " + result);
        return "403".equals(result);
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

    private String escapeMatchedGroup(String group) {
        if (group != null) {
            return group.replace("(", "\\(").replace(")", "\\)").replace("[", "\\[").replace("]", "\\]");
        }
        return null;
    }

    private String patchMainScript(String main_js, boolean silent_mode) {
        main_js = K3dPatcher.patchKantai3d(context, main_js);
        main_js = KenPatcher.patchKantaiEn(main_js, activity);
        main_js = FpsPatcher.patchFps(main_js);
        main_js = CritPatcher.patchCrit(main_js);

        // 2024.11 update: fix patch logic for silent mode
        if (silent_mode) {
            List<String> initVolumePattern1 = Collections.nCopies(3,"this\\[\\w+\\(\\w+\\)\\]=(\\w+\\[\\w+\\(\\w+\\)\\]\\[\\w+\\(\\w+\\)\\]\\(\\w+,\\w+\\(\\w+\\)\\))");
            List<String> initVolumePattern2 = Collections.nCopies(2,"this\\[\\w+\\(\\w+\\)\\]=0x1===\\w+\\[\\w+\\(\\w+\\)\\]\\[\\w+\\(\\w+\\)\\]\\(\\w+,\\w+\\(\\w+\\)\\)");

            List<String> initVolumePatternConcat = new ArrayList<>();
            initVolumePatternConcat.addAll(initVolumePattern1);
            initVolumePatternConcat.addAll(initVolumePattern2);

            Pattern initVolumePattern = Pattern.compile(TextUtils.join(",", initVolumePatternConcat).concat(";"));
            Matcher invPatternMatcher = initVolumePattern.matcher(main_js);

            if (invPatternMatcher.find()) {
                String statement = invPatternMatcher.group(0);
                String varBgm = invPatternMatcher.group(1);
                String varSe = invPatternMatcher.group(2);
                String varVoice = invPatternMatcher.group(3);

                String newStatement = statement.replace(varBgm, "0").replace(varSe, "0").replace(varVoice, "0");
                main_js = main_js.replace(statement, newStatement);
            }
        }

        Pattern howlPattern = Pattern.compile("(new \\w+\\[\\(\\w+\\(\\w+\\)\\)\\])(\\(\\w+\\)),this(?:\\[\\w+\\(\\w+\\)\\]){2}\\(\\w+,\\w+,\\w+\\)\\):");
        Matcher howlPatternMatcher = howlPattern.matcher(main_js);
        boolean howl_found = howlPatternMatcher.find();
        if (howl_found) {
            String _howl_fn = escapeMatchedGroup(howlPatternMatcher.group(1));
            main_js = main_js.replaceAll(_howl_fn, "add_bgm");
        }

        // Rename the original "mouseout" and "mouseover" event name to custom names for objects to listen on
        // Reusing original names will cause a lot of conflict issues
        //main_js = main_js.replace("over:n.pointer?\"pointerover\":\"mouseover\"", "over:\"touchover\"");
        //main_js = main_js.replace("out:n.pointer?\"pointerout\":\"mouseout\"", "out:\"touchout\"");
        main_js = main_js.replaceFirst("('(out|over|down|move|up)'?:[^,;=}]{20,150},?){5,}",
                "down:void 0!==document.ontouchstart?'touchstart':'mousedown',\n" + "move:void 0!==document.ontouchstart?'touchmove':'mousemove',\n" + "up:void 0!==document.ontouchstart?'touchend':'mouseup',\n" + "over:'touchover',\n" + "out:'touchout'");

        main_js = "var gb_h=null;\nfunction add_bgm(b){b.onend=function(){(global_mute||gb_h.volume()==0)&&(gb_h.unload(),console.log('unload'))};global_mute&&(b.autoplay=false);gb_h=new Howl(b);return gb_h;}\n"

                // manage bgm loading strategy with global mute variable for audio focus issue
                + (activity.isMuteMode() ? "var global_mute=1;Howler.mute(true);\n" : "var global_mute=0;Howler.mute(false);\n")

                + main_js +

                // Simulate mouse hover effects by dispatching new custom events "touchover" and "touchout"
                "function patchInteractionManager() {\n" +
                "    var proto = PIXI.interaction.InteractionManager.prototype;\n" +
                "    proto.update = mobileUpdate;\n" +
                "\n" +
                "    function extendMethod(method, extFn) {\n" +
                "        var old = proto[method];\n" +
                "        proto[method] = function () {\n" +
                "            old.call(this, ...arguments);\n" +
                "            extFn.call(this, ...arguments);\n" +
                "        };\n" +
                "    }\n" +
                "\n" +
                "    extendMethod('onPointerDown', function (displayObject, hit) {\n" +
                "        if (this.eventData.data)\n" +
                "            this.processInteractive(this.eventData, this.renderer._lastObjectRendered, this.processTouchOverOut, true);\n" +
                "    });\n" +
                "\n" +
                "    extendMethod('onPointerUp', function (displayObject, hit) {\n" +
                "        if (this.eventData.data)\n" +
                "            this.processInteractive(this.eventData, this.renderer._lastObjectRendered, this.processTouchOverOut, true);\n" +
                "    });\n" +
                "\n" +
                "    function mobileUpdate(deltaTime) {\n" +
                // Fixed interactionFrequency = 4ms
                "        this._deltaTime += deltaTime;\n" +
                "        if (this._deltaTime < 4)\n" +
                "            return;\n" +
                "        this._deltaTime = 0;\n" +
                "        if (!this.interactionDOMElement)\n" +
                "            return;\n" +
                "        if (!this.eventData || !this.eventData.data)  return;\n" +
                "        if (this.eventData.data && (this.eventData.type == 'touchmove' || this.eventData.type == 'touchend' || this.eventData.type == 'tap'))\n" +
                "            this.processInteractive(this.eventData, this.renderer._lastObjectRendered, this.processTouchOverOut, true);\n" +
                "    }\n" +
                "\n" +
                "    proto.processTouchOverOut = function (interactionEvent, displayObject, hit) {\n" +
                "        if (!interactionEvent.data)  return;\n" +
                "        if (hit) {\n" +
                "            if (!displayObject.___over && displayObject._events.touchover) {\n" +
                "                if (displayObject.parent._onClickAll2) return;\n" +
                "                this._hoverObject = displayObject;\n" +
                "                displayObject.___over = true;\n" +
                "                proto.dispatchEvent(displayObject, 'touchover', interactionEvent);\n" +
                "            }\n" +
                "        } else if (displayObject.___over && displayObject._events.touchover && \n" +
                // Only trigger "touchout" when user starts touching another object or empty space
                // So that alert bubbles persist after a simple tap, do not disappear when the finger leaves
                "            ((this._hoverObject && this._hoverObject != displayObject) || !interactionEvent.target)) {\n" +
                "            displayObject.___over = false;\n" +
                "            proto.dispatchEvent(displayObject, 'touchout', interactionEvent);\n" +
                "        }\n" +
                "    };\n" +
                "}" +
                "patchInteractionManager();"
                + MUTE_LISTEN
                + CAPTURE_LISTEN + "\n"
                + KcsInterface.AXIOS_INTERCEPT_SCRIPT;
        return main_js;
    }

    // Reference: https://github.com/KC3Kai/KC3Kai/blob/master/src/library/modules/Translation.js
    private Runnable clearSubtitle = new Runnable() {
        @Override
        public void run() {
            subtitleText.setText("");
        }
    };


    class SubtitleRunnable implements Runnable {
        String subtitle_text = "";
        int duration;

        SubtitleRunnable(String text, int duration) {
            this.subtitle_text = text;
            this.duration = duration;
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
                clearSubHandler.postDelayed(clearSubtitle, duration);
            });
        }
    }

    class VoiceSubtitleRunnable implements Runnable {
        SubtitleData data;

        VoiceSubtitleRunnable(SubtitleData data) {
            this.data = data;
        }

        @Override
        public void run() {
            setSubtitle(data);
        }
    }

    public static boolean patchImage(String ogDestination, String ptDestination, String patchFile) {
        if (ResourceProcess.isImage(ResourceProcess.getCurrentState(ptDestination))) {
            Bitmap ogSpritesheet = BitmapFactory.decodeFile(ogDestination);
            File metadataFile = new File(ogDestination.replace(".png", ".json"));
            Log.e("GOTO-F", "patchImage-src: " + metadataFile.getAbsolutePath());
            File dest = new File(ptDestination);
            Log.e("GOTO-F", "patchImage-desc: " + metadataFile.getAbsolutePath());
            if (!metadataFile.exists()) {
                Bitmap ogImage = BitmapFactory.decodeFile(patchFile.concat("/original.png"));
                if (ogSpritesheet != null && ogImage != null && KcEnUtils.bitmapEqual(ogSpritesheet, ogImage)) {
                    File source = new File(patchFile.concat("/patched.png"));
                    try {
                        dest.getParentFile().mkdirs();
                        dest.createNewFile();
                        KcUtils.copyFileUsingStream(source, dest);
                        Log.e("GOTO", "image patched: " + ptDestination);
                        return true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    String ogFolder = "/original/";
                    String ptFolder = "/patched/";
                    boolean patchFound = false;

                    Reader reader = new FileReader(metadataFile);
                    JsonObject metadata = new JsonParser().parse(reader).getAsJsonObject();
                    reader.close();

                    JsonObject frames = metadata.getAsJsonObject("frames");

                    Set<String> originalFiles = KcEnUtils.listFiles(patchFile.concat(ogFolder));
                    Set<String> patchedFiles = KcEnUtils.listFiles(patchFile.concat(ptFolder));

                    Bitmap ptSpritesheet = ogSpritesheet.copy(ogSpritesheet.getConfig(), true);
                    int ptSpritesheetWidth = ptSpritesheet.getWidth();
                    int ptSpritesheetHeight = ptSpritesheet.getHeight();
                    int[] ptSpritesheetPixels = new int[ptSpritesheetWidth * ptSpritesheetHeight];
                    ptSpritesheet.getPixels(ptSpritesheetPixels, 0, ptSpritesheetWidth, 0, 0, ptSpritesheetWidth, ptSpritesheetHeight);

                    for (Object originalFile : originalFiles) {
                        if (patchedFiles.contains(String.valueOf(originalFile))) {
                            Bitmap ogSprite = getPatchFolderSprite(patchFile, ogFolder, originalFile);
                            Bitmap ptSprite = getPatchFolderSprite(patchFile, ptFolder, originalFile);
                            int ogSpriteWidth = ogSprite.getWidth();
                            int ogSpriteHeight = ogSprite.getHeight();
                            int ptSpriteWidth = ptSprite.getWidth();
                            int ptSpriteHeight = ptSprite.getHeight();
                            if (ogSpriteWidth == ptSpriteWidth && ogSpriteHeight == ptSpriteHeight) {
                                patchFound = isSpritePatched(frames, ogSpritesheet, ptSpritesheet, ogSprite, ptSprite, ogSpriteWidth, ogSpriteHeight);
                            }
                        }
                    }
                    return patchExported(dest, patchFound, ptSpritesheet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private static Bitmap getPatchFolderSprite(String patchFile, String folder, Object originalFile) {
        return BitmapFactory.decodeFile(patchFile.concat(folder).concat(String.valueOf(originalFile)));
    }

    private static boolean patchExported(File dest, boolean patchFound, Bitmap ptSpritesheet) throws IOException {
        if (patchFound) {
            dest.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(dest);
            ptSpritesheet.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return true;
        }
        return false;
    }

    private static boolean isSpritePatched(JsonObject frames, Bitmap ogSpritesheet, Bitmap ptSpritesheet, Bitmap ogSprite, Bitmap ptSprite, int ogSpriteWidth, int ogSpriteHeight) {
        Iterator<String> framesKeys = frames.keySet().iterator();
        while (framesKeys.hasNext()) {
            JsonObject sprite = frames.getAsJsonObject(framesKeys.next());

            JsonObject frame = sprite.getAsJsonObject("frame");
            int frameX = frame.get("x").getAsInt();
            int frameY = frame.get("y").getAsInt();

            JsonObject sourceSize = sprite.getAsJsonObject("sourceSize");
            int w = sourceSize.get("w").getAsInt();
            int h = sourceSize.get("h").getAsInt();

            int[] spritesheetPixels = new int[w * h];
            Bitmap spritesheetSprite = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            int[] ptPixels = new int[w * h];

            ogSpritesheet.getPixels(spritesheetPixels, 0, w, frameX, frameY, w, h);
            spritesheetSprite.setPixels(spritesheetPixels, 0, w, 0, 0, w, h);

            if (w == ogSpriteWidth && h == ogSpriteHeight && KcEnUtils.bitmapEqual(ogSprite, spritesheetSprite)) {
                return patchSprite(ptSpritesheet, ptSprite, w, h, frameX, frameY, ptPixels);
            }
        }
        return false;
    }

    private static boolean patchSprite(Bitmap ptSpritesheet, Bitmap ptSprite, int w, int h, int frameX, int frameY, int[] ptPixels) {
        ptSprite.getPixels(ptPixels, 0, w, 0, 0, w, h);
        ptSpritesheet.setPixels(ptPixels, 0, w, frameX, frameY, w, h);
        return true;
    }
}
