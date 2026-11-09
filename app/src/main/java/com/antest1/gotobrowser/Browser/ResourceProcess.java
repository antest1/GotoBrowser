package com.antest1.gotobrowser.Browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebResourceResponse;

import androidx.lifecycle.Lifecycle;

import com.antest1.gotobrowser.Activity.BrowserActivity;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.KenPatcher;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.SubtitleData;
import com.antest1.gotobrowser.Subtitle.SubtitleProviderUtils;
import com.antest1.gotobrowser.ui.component.DialogButton;
import com.antest1.gotobrowser.ui.component.MessageDialog;

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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import static com.antest1.gotobrowser.Constants.CACHE_DIR;
import static com.antest1.gotobrowser.Constants.DEFAULT_ALTER_GADGET_URL;
import static com.antest1.gotobrowser.Constants.GADGET_OSAPI_IFR;
import static com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_AUTO_DISABLE;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_ENDPOINT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD_URL;
import static com.antest1.gotobrowser.Constants.PREF_CURSOR_MODE;
import static com.antest1.gotobrowser.Constants.PREF_CURSOR_MODE_TOUCH;
import static com.antest1.gotobrowser.Constants.PREF_DOWNLOAD_RETRY;
import static com.antest1.gotobrowser.Constants.PREF_FONT_PREFETCH;
import static com.antest1.gotobrowser.Constants.PREF_SILENT;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.REQUEST_BLOCK_RULES;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.DownloadResult;
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

    private static final String TAG_G = "GOTO";

    // Freshness window for cached resources, overriding the server's unrealistic max-age.
    private static final long CACHE_TTL_MS = 24L * 60 * 60 * 1000;

    // downloadResourceIfMissing results.
    private static final int RESOURCE_CACHED = 0;
    private static final int RESOURCE_DOWNLOADED = 1;
    private static final int RESOURCE_FAILED = -1;
    // The server answered (e.g. 403/404) but there is no local copy to serve.
    private static final int RESOURCE_UNAVAILABLE = -2;

    private static final ExecutorService REVALIDATION_EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Set<String> revalidationInFlight = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
    // Reuses the shared client's TLS setup, which is slow to build on the main thread, but
    // keeps a connection pool of its own so game resource loads keep their idle connections.
    private final OkHttpClient resourceClient = KcUtils.getSharedHttpClient().newBuilder()
            .connectionPool(new ConnectionPool())
            .build();
    SharedPreferences sharedPref;

    private final Handler shipVoiceHandler = new Handler();
    private final Handler clearSubHandler = new Handler();

    ResourceProcess(BrowserActivity activity) {
        this.activity = activity;
        context = activity.getApplicationContext();
        versionTable = new VersionDatabase(context, null, VERSION_TABLE_VERSION);
        sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);
        clearSubtitle = () -> activity.setSubtitleText("");
    }

    // Preferences are read on every use (not cached) so that changes apply on the next page load.
    private boolean isAlterGadgetEnabled() {
        return sharedPref.getBoolean(PREF_ALTER_GADGET, false);
    }

    /** Whether the bypass turns itself off once the gadget server is reachable without it. */
    private boolean isAlterAutoDisableEnabled() {
        return sharedPref.getBoolean(PREF_ALTER_AUTO_DISABLE, true);
    }

    private boolean isGadgetUrlReplaceMode() {
        return PREF_ALTER_METHOD_URL.equals(sharedPref.getString(PREF_ALTER_METHOD, PREF_ALTER_METHOD_URL));
    }

    private boolean isCursorTouchMode() {
        return PREF_CURSOR_MODE_TOUCH.equals(sharedPref.getString(PREF_CURSOR_MODE, PREF_CURSOR_MODE_TOUCH));
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
        if (url.contains("tweenjs.min.js")) return getTweenJs();
        if (url.contains("gadget_html5/script/rollover.js")) return getMuteInjectedRolloverJs();
        if (url.contains("html/maintenance.png")) return getMaintenanceFiles(true);
        if (resource_type == 0) return null;
        if (url.contains("ooi_moe_")) return null;

        if (url.contains("gadget_html5/js/kcs_cda.js")) {
            boolean ip_banned = getIpBannedStatus(url);
            boolean alterGadget = isAlterGadgetEnabled();
            if (alterGadget && !ip_banned) {
                if (isAlterAutoDisableEnabled()) {
                    sharedPref.edit().putBoolean(PREF_ALTER_GADGET, false).commit();
                }
            } else if (!alterGadget && ip_banned) {
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
            KcUtils.reportDownloadException(e);
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
            info.fullUrl = scheme + "://" + info.host + info.path;
            String version = source.getQueryParameter("version");
            if (version != null) {
                info.version = version;
            }
            if (!info.version.isEmpty()) {
                info.fullUrl = info.fullUrl + "?version=" + info.version;
            }
            info.key = "|" + info.path + "|" + info.version;
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

    private static final ThreadLocal<SimpleDateFormat> HTTP_DATE_FORMATTER = new ThreadLocal<>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat formatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
            return formatter;
        }
    };

    public static Date parseHttpDate(String dateString) {
        try {
            return HTTP_DATE_FORMATTER.get().parse(dateString);
        } catch (ParseException | NullPointerException e) {
            return null;
        }
    }

    private WebResourceResponse processImageDataResource(ResourceRequestInfo requestInfo, int resource_type) {
        File file = new File(requestInfo.outputPath);
        int status = downloadResourceIfMissing(requestInfo, file);
        if (status == RESOURCE_FAILED) return promptForRetry(requestInfo, resource_type);
        if (status == RESOURCE_UNAVAILABLE) return null;
        return serveLocalCachedImage(file, requestInfo, resource_type, status == RESOURCE_DOWNLOADED);
    }

    // Serves the cached copy if present (refreshing it in the background), else downloads once.
    private int downloadResourceIfMissing(ResourceRequestInfo requestInfo, File file) {
        String update_key = requestInfo.key;
        // canRead: a file left by an earlier install/restore can exist but be unreadable (EACCES).
        boolean cached = file.exists() && file.canRead() && file.length() > 0
                && !Objects.equals(versionTable.getVersionValue(update_key), versionTable.getDefaultValue());
        if (cached) {
            revalidateCacheInBackground(update_key, requestInfo.fullUrl, file);
            return RESOURCE_CACHED;
        }
        DownloadResult result = downloadResource(resourceClient, requestInfo.fullUrl, file, null);
        if (result.responseCode == 200) {
            versionTable.putCacheAndVersion(update_key, result.lastModified, getRevalidationExpiry());
            return RESOURCE_DOWNLOADED;
        }
        // Only a connectivity failure is worth a retry prompt; a local file error (e.g. an
        // unwritable cache folder) would fail again, so let the WebView fetch it instead.
        if (result.responseCode == -1) return result.networkError ? RESOURCE_FAILED : RESOURCE_UNAVAILABLE;
        // Non-200 answers write nothing, so only an existing file counts as cached.
        return file.exists() && file.length() > 0 ? RESOURCE_CACHED : RESOURCE_UNAVAILABLE;
    }

    // Stale-while-revalidate (off the UI thread): the conditional GET result is used on a future
    // load, so an asset updated without a version bump self-heals within CACHE_TTL_MS.
    private void revalidateCacheInBackground(String update_key, String resource_url, File file) {
        Date expiry = parseHttpDate(versionTable.getCacheControlValue(update_key));
        if (expiry != null && System.currentTimeMillis() < expiry.getTime()) return;
        if (!revalidationInFlight.add(update_key)) return;

        String prevLastModified = versionTable.getVersionValue(update_key);
        final String ifModifiedSince = Objects.equals(versionTable.getDefaultValue(), prevLastModified) ? null : prevLastModified;

        REVALIDATION_EXECUTOR.execute(() -> {
            try {
                DownloadResult result = downloadResource(resourceClient, resource_url, file, ifModifiedSince);
                if (result.responseCode == 200) {
                    versionTable.putCacheAndVersion(update_key, result.lastModified, getRevalidationExpiry());
                } else if (result.responseCode == 304) {
                    versionTable.putCacheControlValue(update_key, getRevalidationExpiry());
                }
            } catch (Exception e) {
                KcUtils.reportDownloadException(e);
            } finally {
                revalidationInFlight.remove(update_key);
            }
        });
    }

    private String getRevalidationExpiry() {
        return HTTP_DATE_FORMATTER.get().format(new Date(System.currentTimeMillis() + CACHE_TTL_MS));
    }

    // Patches the image only if a changed source is detected, then returns a response backed by
    // the local file. updateFlag=true forces re-patch after a fresh download.
    private WebResourceResponse serveLocalCachedImage(File originalFile, ResourceRequestInfo requestInfo, int resource_type, boolean updateFlag) {
        File file = ResourcePatcher.INSTANCE.patchImageIfAvailable(context, versionTable, requestInfo.path, originalFile, updateFlag);
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            String type = ResourceProcess.isImage(resource_type) ? "image/png" : "application/json";
            return new WebResourceResponse(type, "utf-8", is);
        } catch (IOException e) {
            // A local file error; retrying the same file would fail again, so let the WebView fetch it.
            KcUtils.reportException(e);
            return null;
        }
    }

    private void showGadgetIpServerBlockedDialog() {
        activity.showMessageDialog(new MessageDialog(
                activity.getString(R.string.dialog_ipblock_title),
                String.format(activity.getString(R.string.dialog_ipblock_message),
                        activity.getString(R.string.connection_use_alter)),
                new DialogButton(activity.getString(R.string.action_ok), activity::finish)));
    }

    private WebResourceResponse promptForRetry(ResourceRequestInfo requestInfo, int resource_type) {
        boolean isRetryPromptEnabled = sharedPref.getBoolean(PREF_DOWNLOAD_RETRY, true);
        if (!isRetryPromptEnabled) return null;
        // In the background nobody can answer the dialog, so the request thread would stay
        // blocked until the user returns. Let the WebView fetch the resource itself instead.
        if (!activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return null;
        final AtomicReference<Boolean> cancelled = new AtomicReference<>(false);
        final CountDownLatch retryReady = new CountDownLatch(1);
        // Every answer releases the waiting request thread; only "retry" goes on to download.
        Runnable skip = () -> {
            cancelled.set(true);
            retryReady.countDown();
        };
        boolean shown = activity.showMessageDialog(new MessageDialog(
                activity.getString(R.string.dialog_retry_title),
                String.format(activity.getString(R.string.dialog_retry_message), requestInfo.path),
                new DialogButton(activity.getString(R.string.dialog_retry_yes), retryReady::countDown),
                new DialogButton(activity.getString(R.string.dialog_retry_no), skip),
                new DialogButton(activity.getString(R.string.dialog_retry_never), () -> {
                    sharedPref.edit().putBoolean(PREF_DOWNLOAD_RETRY, false).apply();
                    skip.run();
                })));
        if (!shown) return null;

        try {
            // The dialog goes away with the activity without an answer, so stop
            // waiting once the activity is gone instead of blocking this thread forever.
            while (!retryReady.await(1, TimeUnit.SECONDS)) {
                if (activity.isDestroyed()) return null;
            }
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
        if (isAlterGadgetEnabled() && isGadgetUrlReplaceMode() && url.contains("gadget_html5")) {
            String alterEndpoint = sharedPref.getString(PREF_ALTER_ENDPOINT, DEFAULT_ALTER_GADGET_URL);
            url = WebViewManager.replaceEndpoint(url, alterEndpoint);
            byte[] byteArray = KcUtils.downloadDataFromURL(url);
            InputStream is = new ByteArrayInputStream(byteArray);
            return new WebResourceResponse("application/javascript", "utf-8", is);
        }

        if (url.contains("kcs2/js/main.js")) {
            String update_key = requestInfo.key;
            String resource_url = requestInfo.fullUrl;
            String out_file_path = requestInfo.outputPath;
            File file = new File(out_file_path);

            String prevLastModified = versionTable.getVersionValue(update_key);
            // prevLastModified is null when the last response had no Last-Modified header.
            if (!file.exists() || file.length() == 0 || prevLastModified == null
                    || prevLastModified.equals(versionTable.getDefaultValue())) {
                prevLastModified = null;
            }

            // main.js is core game code; bypass max-age caching and issue If-Modified-Since revalidation.
            DownloadResult result = downloadResource(resourceClient, resource_url, file, prevLastModified);
            if (result.responseCode == 200) {
                versionTable.putCacheAndVersion(update_key, result.lastModified, versionTable.getDefaultValue());
            }

            byte[] byteArray;
            if (file.exists() && file.length() > 0) {
                byteArray = KcUtils.getBytesFromInputStream(new FileInputStream(file));
            } else {
                byteArray = KcUtils.downloadDataFromURL(url);
            }

            String main_js = ResourcePatcher.INSTANCE.patchMainScript(context, activity, new String(byteArray, StandardCharsets.UTF_8), silent_mode, isCursorTouchMode(), getTouchEventPatchJs());
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
        File file = new File(requestInfo.outputPath);
        int status = downloadResourceIfMissing(requestInfo, file);
        if (status == RESOURCE_FAILED) return promptForRetry(requestInfo, resource_type);
        // Let the WebView fetch it itself and see the server's real answer.
        if (status == RESOURCE_UNAVAILABLE) return null;

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
            // A local file error; retrying the same file would fail again, so let the WebView fetch it.
            KcUtils.reportException(e);
            return null;
        }
    }

    private WebResourceResponse processFontFile(ResourceRequestInfo requestInfo) throws IOException {
        File file = new File(requestInfo.outputPath);
        downloadResourceIfMissing(requestInfo, file);
        if (!file.exists()) return null; // let the WebView fetch it itself
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

    private WebResourceResponse getMuteInjectedRolloverJs() {
        try {
            return new WebResourceResponse("application/x-javascript", "utf-8", context.getAssets().open("rollover.js"));
        } catch (IOException e) { return null; }
    }

    private boolean getIpBannedStatus(String url) {
        DownloadResult result = downloadResource(resourceClient, url, null);
        return result.responseCode == 403;
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
            byte[] buffer = new byte[16384];
            for (int length; (length = inputStream.read(buffer)) != -1;) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
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
