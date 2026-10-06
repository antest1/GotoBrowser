package com.antest1.gotobrowser.Browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebResourceResponse;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.KenPatcher;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.SubtitleData;
import com.antest1.gotobrowser.Subtitle.SubtitleProviderUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;

import static com.antest1.gotobrowser.Constants.CACHE_DIR;
import static com.antest1.gotobrowser.Constants.DEFAULT_ALTER_GADGET_URL;
import static com.antest1.gotobrowser.Constants.GADGET_OSAPI_IFR;
import static com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_ENDPOINT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD_URL;
import static com.antest1.gotobrowser.Constants.PREF_CURSOR_MODE;
import static com.antest1.gotobrowser.Constants.PREF_CURSOR_MODE_TOUCH;
import static com.antest1.gotobrowser.Constants.PREF_DOWNLOAD_RETRY;
import static com.antest1.gotobrowser.Constants.PREF_FONT_PREFETCH;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH;
import static com.antest1.gotobrowser.Constants.PREF_SILENT;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.REQUEST_BLOCK_RULES;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.downloadResource;
import static com.antest1.gotobrowser.Helpers.KcUtils.getEmptyStream;

public class ResourceProcess {
    public static final int RES_IMAGE  = 0b0000001;
    public static final int RES_AUDIO  = 0b0000010;
    public static final int RES_JSON   = 0b0000100;
    public static final int RES_JS     = 0b0001000;
    public static final int RES_FONT   = 0b0010000;
    public static final int RES_CSS    = 0b0100000;
    public static final int RES_KCSAPI = 0b1000000;
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

    private static final String TAG_D = "GOTO-D";
    private static final String TAG_G = "GOTO";

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

    public static class ResourceRequestInfo {
        String key = "";
        String url = "";
        String host = "";
        String path = "";
        String version = "";
        String filename = "";
        String fullUrl = "";
        String outputDir = "";
        String outputPath = "";
    }

    private final BrowserActivity activity;
    private final Context context;
    private final VersionDatabase versionTable;
    private final OkHttpClient resourceClient = new OkHttpClient();
    SharedPreferences sharedPref;

    private final Handler shipVoiceHandler = new Handler();
    private final Handler clearSubHandler = new Handler();

    boolean prefAlterGadget, prefModKantaiEn, isGadgetUrlReplaceMode, isCursorTouchMode;
    String alterEndpoint;

    ResourceProcess(BrowserActivity activity) {
        this.activity = activity;
        context = activity.getApplicationContext();
        versionTable = new VersionDatabase(context, null, VERSION_TABLE_VERSION);
        sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);
        prefAlterGadget = sharedPref.getBoolean(PREF_ALTER_GADGET, false);
        isGadgetUrlReplaceMode = sharedPref.getString(PREF_ALTER_METHOD, PREF_ALTER_METHOD_URL)
                .equals(PREF_ALTER_METHOD_URL);
        isCursorTouchMode = sharedPref.getString(PREF_CURSOR_MODE, PREF_CURSOR_MODE_TOUCH)
                .equals(PREF_CURSOR_MODE_TOUCH);
        alterEndpoint = sharedPref.getString(PREF_ALTER_ENDPOINT, DEFAULT_ALTER_GADGET_URL);
        prefModKantaiEn = sharedPref.getBoolean(PREF_MOD_KCCP_LANG_PATCH, false);
        clearSubtitle = () -> activity.setSubtitleText("");
    }

    public static String getUserAgent() {
        if (userAgent == null) return WebViewManager.USER_AGENT;
        return userAgent;
    }

    public static void setUserAgent(String agent) { userAgent = agent; }

    public static int getCurrentState(Uri source) {
        String path = source.getPath();
        if (path == null) return 0;
        int state = 0;
        String url = source.toString();
        if (path.contains("kcs2") && (path.endsWith(".png") || path.endsWith(".jpg"))) {
            state |= RES_IMAGE;
        }
        if (path.endsWith(".mp3")) {
            state |= RES_AUDIO;
        }
        if (path.endsWith(".json")) {
            state |= RES_JSON;
        }
        if ((path.contains("/js/") || path.contains("/script/")) && path.endsWith(".js")) {
            state |= RES_JS;
        }
        if (path.endsWith(".woff2")) {
            state |= RES_FONT;
        }
        if (path.endsWith(".css")) {
            state |= RES_CSS;
        }
        if (path.contains("kcsapi") && !url.contains("osapi.dmm.com")) {
            state |= RES_KCSAPI;
        }
        return state;
    }

    @SuppressLint("ApplySharedPref")
    public WebResourceResponse processWebRequest(Uri source) {
        int resource_type = getCurrentState(source);
        String url = source.toString();
        if (resource_type > 0) Log.e(TAG_G, url + " - " + resource_type);
        boolean is_image = ResourceProcess.isImage(resource_type);
        boolean is_audio = ResourceProcess.isAudio(resource_type);
        boolean is_json = ResourceProcess.isJson(resource_type);
        boolean is_js = ResourceProcess.isScript(resource_type);
        boolean is_font = ResourceProcess.isFont(resource_type);
        boolean is_css = ResourceProcess.isStylesheet(resource_type);
        boolean is_kcsapi = ResourceProcess.isKcsApi(resource_type);

        if (checkBlockedContent(url)) return getEmptyResponse();
        if (url.contains(GADGET_OSAPI_IFR)) return getGadgetIfrPage(url);
        if (url.contains("ooi.css")) return getOoiSheetFromAsset();
        if (url.contains("tweenjs.min.js")) return getTweenJs();
        if (url.contains("gadget_html5/script/rollover.js")) return getMuteInjectedRolloverJs();
        if (url.contains("html/maintenance.png")) return getMaintenanceFiles(true);
        if (resource_type == 0) return null;
        if (url.contains("ooi_moe_")) return null;

        if (url.contains("gadget_html5/js/kcs_cda.js")) {
            boolean ip_banned = getIpBannedStatus(url);
            if (prefAlterGadget && !ip_banned) {
                sharedPref.edit().putBoolean(PREF_ALTER_GADGET, false).commit();
            } else if (!prefAlterGadget && ip_banned) {
                showGadgetIpServerBlockedDialog();
            }
            return getInjectedKcaCdaJs();
        }

        ResourceRequestInfo requestInfo = getPathAndFileInfo(source);
        String path = requestInfo.path;
        String filename = requestInfo.filename;

        try {
            if (!path.isEmpty() && !filename.isEmpty()) {
                if (filename.equals("version.json") || filename.contains("index.php")) {
                    return null;
                }

                if (is_kcsapi && path.contains("/api_start2")) {
                    return null;
                }

                if (!is_kcsapi) {
                    if (is_image || is_json) return processImageDataResource(requestInfo, resource_type);
                    if (is_js) return processScriptFile(requestInfo);
                    if (is_audio) return processAudioFile(requestInfo, resource_type);
                    if (is_css) return processStylesheet(requestInfo);
                    if (is_font) {
                        if (sharedPref.getBoolean(PREF_FONT_PREFETCH, true) && !KenPatcher.isPatcherEnabled()) {
                            return getFontFile(filename);
                        } else {
                            return processFontFile(requestInfo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG_G, KcUtils.getStringFromException(e));
            KcUtils.reportException(e);
        }
        return null;
    }

    private ResourceRequestInfo getPathAndFileInfo(Uri source) {
        ResourceRequestInfo info = new ResourceRequestInfo();
        String cacheDir = KcUtils.getAppCacheFileDir(context, CACHE_DIR);
        info.url = source.toString();
        if (source.getPath() != null) {
            String scheme = source.getScheme();
            info.host = source.getHost();
            info.path = source.getPath();
            List<String> segments = source.getPathSegments();
            if (segments.size() > 1) {
                StringBuilder outputPathBuilder = new StringBuilder(cacheDir);
                for (String segment : segments.subList(0, segments.size() - 1)) {
                    outputPathBuilder.append(segment).append("/");
                }
                info.outputDir = outputPathBuilder.toString();
            } else {
                info.outputDir = cacheDir;
            }
            info.filename = source.getLastPathSegment();
            if (info.filename != null) {
                info.outputPath = info.outputDir.concat(info.filename);
            }
            info.fullUrl = String.format(Locale.US, "%s://%s%s", scheme, info.host, info.path);
            String version = source.getQueryParameter("version");
            if (version != null) {
                info.version = version;
            }
            if (!info.version.isEmpty()) {
                info.fullUrl = info.fullUrl + "?version=" + info.version;
            }
            info.key = String.format(Locale.US, "|%s|%s", info.path, info.version);
        }
        return info;
    }

    private boolean checkBlockedContent(String url) {
        for (String rule : REQUEST_BLOCK_RULES) {
            if (url.contains(rule)) {
                Log.e(TAG_G, "blocked: ".concat(url));
                return true;
            }
        }
        return false;
    }

    private WebResourceResponse getEmptyResponse() {
        return new WebResourceResponse("text/css", "utf-8", getEmptyStream());
    }

    private WebResourceResponse getGadgetIfrPage(String url) {
        try {
            byte[] byteArray = KcUtils.downloadDataFromURL(url);
            String gadget_page = new String(byteArray, StandardCharsets.UTF_8);
            gadget_page = gadget_page.replace("background-color:white;", "background-color:black;");
            gadget_page = gadget_page.replace("</style>", "#globalNavi, #contentsWrap {display:none;}</style>");
            InputStream is = new ByteArrayInputStream(gadget_page.getBytes());
            return new WebResourceResponse("text/html", "utf-8", is);
        } catch (IOException e) {
            return null;
        }
    }

    private int checkCacheExpired(String expiryDate) {
        Date parsedExpiryDate = parseHttpDate(expiryDate);
        if (parsedExpiryDate == null) return -1;
        return parsedExpiryDate.before(new Date()) ? 1 : 0;
    }

    public static Date parseHttpDate(String dateString) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
            return formatter.parse(dateString);
        } catch (ParseException | NullPointerException e) {
            return null;
        }
    }

    private String getCacheExpiredAt(String cache_control) {
        Long maxAgeSeconds = KcUtils.extractMaxAge(cache_control);
        if (maxAgeSeconds != null) {
            Date now = new Date();
            long expiryMillis = now.getTime() + (maxAgeSeconds * 1000);
            Date expiryDate = new Date(expiryMillis);
            SimpleDateFormat sdf = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            return sdf.format(expiryDate);
        } else {
            return versionTable.getDefaultValue();
        }
    }

    private WebResourceResponse processImageDataResource(ResourceRequestInfo requestInfo, int resource_type) {
        String update_key = requestInfo.key;
        String path = requestInfo.path;
        String resource_url = requestInfo.fullUrl;
        String out_file_path = requestInfo.outputPath;
        String log_path = out_file_path;
        File file = new File(out_file_path);

        Log.e(TAG_D, "resource_url: " + resource_url);
        String cacheExpiredDate = versionTable.getCacheControlValue(update_key);
        String prevLastModified = versionTable.getVersionValue(update_key);

        int isCacheExpired = checkCacheExpired(cacheExpiredDate);
        boolean isDefaultValue = prevLastModified.equals(versionTable.getDefaultValue())
                || cacheExpiredDate.equals(versionTable.getDefaultValue());
        if (!file.exists() || isDefaultValue || isCacheExpired == -1) prevLastModified = null;

        boolean update_flag = false;
        if (prevLastModified == null || isCacheExpired == 1) {
            JsonObject result = downloadResource(
                    resourceClient, resource_url, file, prevLastModified);

            if (result.has("response_code")) {
                int response_code = result.get("response_code").getAsInt();
                if (response_code == 200) {
                    update_flag = true;
                    String cache_expired = getCacheExpiredAt(result.get("cache_control").getAsString());
                    String last_modified = result.get("last_modified").getAsString();
                    versionTable.putCacheAndVersion(update_key, last_modified, cache_expired);
                }
            } else {
                return promptForRetry(requestInfo, resource_type);
            }
        }

        file = ResourcePatcher.INSTANCE.patchImageIfAvailable(context, versionTable, path, file, update_flag);

        try {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            String type = ResourceProcess.isImage(resource_type) ? "image/png" : "application/json";
            return new WebResourceResponse(type, "utf-8", is);
        } catch (IOException e) {
            KcUtils.reportException(e);
            return promptForRetry(requestInfo, resource_type);
        }
    }

    private void showGadgetIpServerBlockedDialog() {
        activity.runOnUiThread(() -> {
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    activity.finish();
                }
                dialog.dismiss();
            };

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
            builder.setTitle(activity.getString(R.string.dialog_ipblock_title))
                    .setMessage(String.format(activity.getString(R.string.dialog_ipblock_message),
                            activity.getString(R.string.connection_use_alter)))
                    .setPositiveButton(activity.getString(R.string.action_ok), dialogClickListener)
                    .setCancelable(false).show();
        });
    }

    private WebResourceResponse promptForRetry(ResourceRequestInfo requestInfo, int resource_type) {
        boolean isRetryPromptEnabled = sharedPref.getBoolean(PREF_DOWNLOAD_RETRY, true);
        if (!isRetryPromptEnabled) return null;
        final AtomicReference<Boolean> cancelled = new AtomicReference<>(false);
        final CountDownLatch retryReady = new CountDownLatch(1);
        activity.runOnUiThread(() -> {
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        retryReady.countDown();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        sharedPref.edit().putBoolean(PREF_DOWNLOAD_RETRY, false).apply();
                    case DialogInterface.BUTTON_NEUTRAL:
                    default:
                        cancelled.set(true);
                }
                dialog.dismiss();
            };

            if (!activity.isFinishing()) {
                try {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
                    builder.setTitle(activity.getString(R.string.dialog_retry_title))
                            .setMessage(String.format(activity.getString(R.string.dialog_retry_message), requestInfo.path))
                            .setPositiveButton(activity.getString(R.string.dialog_retry_yes), dialogClickListener)
                            .setNeutralButton(activity.getString(R.string.dialog_retry_no), dialogClickListener)
                            .setNegativeButton(activity.getString(R.string.dialog_retry_never), dialogClickListener)
                            .setCancelable(false).show();
                } catch (WindowManager.BadTokenException ignored) {}
            }
        });

        try {
            retryReady.await();
        } catch (InterruptedException e) {
            return null;
        }

        if (cancelled.get()) return null;
        if (ResourceProcess.isImage(resource_type)) {
            return processImageDataResource(requestInfo, resource_type);
        } else if (ResourceProcess.isAudio(resource_type)) {
            return processAudioFile(requestInfo, resource_type);
        }
        return null;
    }

    private WebResourceResponse processScriptFile(ResourceRequestInfo requestInfo) throws IOException {
        boolean silent_mode = sharedPref.getBoolean(PREF_SILENT, false);
        String url = requestInfo.url;
        if (prefAlterGadget && isGadgetUrlReplaceMode && url.contains("gadget_html5")) {
            url = WebViewManager.replaceEndpoint(url, alterEndpoint);
            byte[] byteArray = KcUtils.downloadDataFromURL(url);
            InputStream is = new ByteArrayInputStream(byteArray);
            return new WebResourceResponse("application/javascript", "utf-8", is);
        }

        if (url.contains("kcs2/js/main.js")) {
            byte[] byteArray = KcUtils.downloadDataFromURL(url);
            String main_js = ResourcePatcher.INSTANCE.patchMainScript(context, activity, new String(byteArray, StandardCharsets.UTF_8), silent_mode, isCursorTouchMode, getTouchEventPatchJs());
            InputStream is = new ByteArrayInputStream(main_js.getBytes());
            return new WebResourceResponse("application/javascript", "utf-8", is);
        }
        return null;
    }

    private WebResourceResponse processStylesheet(ResourceRequestInfo requestInfo) throws IOException {
        String url = requestInfo.url;
        if (sharedPref.getBoolean(PREF_ADJUSTMENT, true)) {
            AssetManager as = context.getAssets();
            if (url.contains("kcscontents/css/import.css")) {
                InputStream game_in = as.open("game_custom.css");
                byte[] game_css = KcUtils.getBytesFromInputStream(game_in);
                return new WebResourceResponse("text/css", "utf-8", new ByteArrayInputStream(game_css));
            }
            if (url.contains("kcscontents/css/default.css") || url.contains("kcscontents/css/style.css")) {
                return getEmptyResponse();
            }
            if (url.contains("play.games.dmm.com/assets/index") && url.endsWith(".css")) {
                byte[] byteArray = KcUtils.downloadDataFromURL(url);
                String css = new String(byteArray, StandardCharsets.UTF_8);
                InputStream dmm_in = as.open("dmm_custom.css");
                String dmm_css = KcUtils.getStringFromInputStream(dmm_in);
                css = css.concat("\n\n").concat(dmm_css);
                return new WebResourceResponse("text/css", "utf-8", new ByteArrayInputStream(css.getBytes()));
            }
        }
        return null;
    }

    private WebResourceResponse processAudioFile(ResourceRequestInfo requestInfo, int resource_type) {
        String update_key = requestInfo.key;
        String resource_url = requestInfo.fullUrl;
        String out_file_path = requestInfo.outputPath;
        File file = new File(out_file_path);

        String cacheExpiredDate = versionTable.getCacheControlValue(update_key);
        String prevLastModified = versionTable.getVersionValue(update_key);

        int isCacheExpired = checkCacheExpired(cacheExpiredDate);
        if (!file.exists() || prevLastModified.equals(versionTable.getDefaultValue()) || isCacheExpired == -1) {
            prevLastModified = null;
        }

        if (prevLastModified == null || isCacheExpired == 1) {
            JsonObject result = downloadResource(resourceClient, resource_url, file, prevLastModified);
            if (result.has("response_code")) {
                int response_code = result.get("response_code").getAsInt();
                if (response_code == 200) {
                    versionTable.putCacheAndVersion(update_key, result.get("last_modified").getAsString(),
                            getCacheExpiredAt(result.get("cache_control").getAsString()));
                }
            } else {
                return promptForRetry(requestInfo, resource_type);
            }
        }

        String voiceSize = String.valueOf(file.length());
        String subtitle_local = sharedPref.getString(PREF_SUBTITLE_LOCALE, "en");
        SubtitleData data = SubtitleProviderUtils.getSubtitleProvider(subtitle_local).getSubtitleData(requestInfo.url, requestInfo.path, voiceSize);

        if (data != null) {
            if (data.getExtraDelay() != null) setSubtitleAfter(data);
            else setSubtitle(data);
        }

        try {
            file = ResourcePatcher.INSTANCE.applyKenPatcherIfAvailable(context, versionTable, requestInfo.path, file, false);
            return new WebResourceResponse("audio/mpeg", "binary", new BufferedInputStream(new FileInputStream(file)));
        } catch (IOException e) {
            KcUtils.reportException(e);
            return promptForRetry(requestInfo, resource_type);
        }
    }

    private WebResourceResponse processFontFile(ResourceRequestInfo requestInfo) throws IOException {
        String update_key = requestInfo.key;
        String resource_url = requestInfo.fullUrl;
        File file = new File(requestInfo.outputPath);

        String cacheExpiredDate = versionTable.getCacheControlValue(update_key);
        String prevLastModified = versionTable.getVersionValue(update_key);

        int isCacheExpired = checkCacheExpired(cacheExpiredDate);
        if (!file.exists() || prevLastModified.equals(versionTable.getDefaultValue()) || isCacheExpired == -1) {
            prevLastModified = null;
        }

        if (prevLastModified == null || isCacheExpired == 1) {
            JsonObject result = downloadResource(resourceClient, resource_url, file, prevLastModified);
            if (result.has("response_code") && result.get("response_code").getAsInt() == 200) {
                versionTable.putCacheAndVersion(update_key, result.get("last_modified").getAsString(),
                        getCacheExpiredAt(result.get("cache_control").getAsString()));
            }
        }

        file = ResourcePatcher.INSTANCE.applyKenPatcherIfAvailable(context, versionTable, requestInfo.path, file, false);
        return new WebResourceResponse("application/font-woff2", "binary", new BufferedInputStream(new FileInputStream(file)));
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
    }

    private WebResourceResponse getOoiSheetFromAsset() {
        try {
            return new WebResourceResponse("text/css", "utf-8", context.getAssets().open("ooi.css"));
        } catch (IOException e) { return null; }
    }

    private WebResourceResponse getMuteInjectedRolloverJs() {
        try {
            return new WebResourceResponse("application/x-javascript", "utf-8", context.getAssets().open("rollover.js"));
        } catch (IOException e) { return null; }
    }

    private boolean getIpBannedStatus(String url) {
        JsonObject result = downloadResource(resourceClient, url, null);
        return result.has("response_code") && result.get("response_code").getAsInt() == 403;
    }

    private WebResourceResponse getInjectedKcaCdaJs() {
        try {
            return new WebResourceResponse("application/x-javascript", "utf-8", context.getAssets().open("kcs_cda.js"));
        } catch (IOException e) { return null; }
    }

    private WebResourceResponse getTweenJs() {
        try {
            return new WebResourceResponse("application/x-javascript", "utf-8", context.getAssets().open("tweenjs-0.6.2.min.js"));
        } catch (IOException e) { return null; }
    }

    private WebResourceResponse getFontFile(String filename) {
        try {
            return new WebResourceResponse("application/octet-stream", "utf-8", context.getAssets().open(filename));
        } catch (IOException e) { return null; }
    }

    private String getTouchEventPatchJs() {
        try {
            InputStream inputStream = context.getAssets().open("touch_event_patch.js");
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = inputStream.read(buffer)) != -1;) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8);
        } catch (IOException e) { return null; }
    }

    private WebResourceResponse getMaintenanceFiles(boolean is_image) {
        try {
            if (is_image) return new WebResourceResponse("image/png", "utf-8", context.getAssets().open("maintenance.png"));
            else return new WebResourceResponse("text/html", "utf-8", context.getAssets().open("maintenance.html"));
        } catch (IOException e) { return null; }
    }

    class SubtitleRunnable implements Runnable {
        String subtitle_text;
        int duration;
        SubtitleRunnable(String text, int duration) { this.subtitle_text = text; this.duration = duration; }

        @Override
        public void run() {
            activity.runOnUiThread(() -> {
                clearSubHandler.removeCallbacks(clearSubtitle);
                if (activity.isSubtitleAvailable()) {
                    subtitle_text = subtitle_text.replace("<br>", "\n").replace("<br />", "\n");
                } else {
                    subtitle_text = context.getString(R.string.no_subtitle_file);
                }
                if (activity.isCaptionAvailable()) activity.setSubtitleText(subtitle_text);
                clearSubHandler.postDelayed(clearSubtitle, duration);
            });
        }
    }

    private final Runnable clearSubtitle;

    class VoiceSubtitleRunnable implements Runnable {
        SubtitleData data;
        VoiceSubtitleRunnable(SubtitleData data) { this.data = data; }
        @Override
        public void run() { setSubtitle(data); }
    }
}
