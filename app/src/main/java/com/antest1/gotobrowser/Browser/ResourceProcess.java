package com.antest1.gotobrowser.Browser;

import android.annotation.SuppressLint;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;

import static com.antest1.gotobrowser.Constants.CACHE_DIR;
import static com.antest1.gotobrowser.Constants.CAPTURE_LISTEN;
import static com.antest1.gotobrowser.Constants.DEFAULT_ALTER_GADGET_URL;
import static com.antest1.gotobrowser.Constants.GADGET_OSAPI_IFR;
import static com.antest1.gotobrowser.Constants.MUTE_LISTEN;
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
import static com.antest1.gotobrowser.Helpers.KcEnUtils.GetMD5HashOfString;
import static com.antest1.gotobrowser.Helpers.KcEnUtils.dirMD5;
import static com.antest1.gotobrowser.Helpers.KcUtils.downloadResource;
import static com.antest1.gotobrowser.Helpers.KcUtils.getEmptyStream;

public class ResourceProcess {
    private static final int RES_IMAGE  = 0b0000001;
    private static final int RES_AUDIO  = 0b0000010;
    private static final int RES_JSON   = 0b0000100;
    private static final int RES_JS     = 0b0001000;
    private static final int RES_FONT   = 0b0010000;
    private static final int RES_CSS    = 0b0100000;
    private static final int RES_KCSAPI = 0b1000000;
    private static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

    private static final Pattern INIT_VOLUME_PATTERN = Pattern.compile(
            String.format(Locale.US, "(%s,%s,%s,%s,%s);",
                    "this\\[\\w+\\(\\w+\\)\\]=(\\w+\\[\\w+\\(\\w+\\)\\]\\[\\w+\\(\\w+\\)\\]\\(\\w+,\\w+\\(\\w+\\),\\w+\\))",
                    "this\\[\\w+\\(\\w+\\)\\]=(\\w+\\[\\w+\\(\\w+\\)\\]\\[\\w+\\(\\w+\\)\\]\\(\\w+,\\w+\\(\\w+\\),\\w+\\))",
                    "this\\[\\w+\\(\\w+\\)\\]=(\\w+\\[\\w+\\(\\w+\\)\\]\\[\\w+\\(\\w+\\)\\]\\(\\w+,\\w+\\(\\w+\\),\\w+\\))",
                    "this\\[\\w+\\(\\w+\\)\\]=0x1===\\w+\\[\\w+\\(\\w+\\)\\]\\[\\w+\\(\\w+\\)\\]\\(\\w+,\\w+\\(\\w+\\),\\w+\\)",
                    "this\\[\\w+\\(\\w+\\)\\]=0x1===\\w+\\[\\w+\\(\\w+\\)\\]\\[\\w+\\(\\w+\\)\\]\\(\\w+,\\w+\\(\\w+\\),\\w+\\)")
    );
    private static final Pattern HOWL_PATTERN = Pattern.compile("(new \\w+\\[\\(\\w+\\(\\w+\\)\\)])(\\(\\w+\\)),this(?:\\[\\w+\\(\\w+\\)]){2}\\(\\w+,\\w+,\\w+\\)\\):");
    private static final Pattern TOUCH_EVENT_PATTERN = Pattern.compile("('(out|over|down|move|up)'?:[^,;=}]{20,150},?){5,}");

    private static final String TAG_D = "GOTO-D";
    private static final String TAG_P = "GOTO-P";
    private static final String TAG_E = "GOTO-E";
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

    private static class ResourceRequestInfo {
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

    private final TextView subtitleText;
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
        subtitleText = activity.findViewById(R.id.subtitle_view);
        subtitleText.setOnClickListener(v -> clearSubHandler.postDelayed(clearSubtitle, 250));
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
        if (url.contains("ooi_moe_")) return null; // Prevent OOI from caching the server name display

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

                // load game data
                if (is_kcsapi && path.contains("/api_start2")) {
                    // checkSpecialSubtitleMode();
                    return null;
                }

                if (!is_kcsapi) {
                    // JsonObject update_info = checkResourceUpdate(source);
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
            // HTTP dates are always in GMT
            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
            return formatter.parse(dateString);
        } catch (ParseException | NullPointerException e) {
            // If the format is wrong, return current date (expired)
            return null;
        }
    }

    private String getCacheExpiredAt(String cache_control) {
        Long maxAgeSeconds = KcUtils.extractMaxAge(cache_control);
        if (maxAgeSeconds != null) {
            // 3. Calculate expiration
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
        File file = getImageFile(out_file_path);

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
                    Log.e(TAG_D, update_key + " last_modified: " + last_modified);
                    Log.e(TAG_D, update_key + " cache_expired: " + cache_expired);
                } else if (response_code == 304) {
                    Log.e(TAG_D, update_key + " use cached resource (304)");
                } else {
                    Log.e(TAG_D, update_key + " response_code: " + response_code);
                }
            } else {
                Log.e(TAG_D, "download error: " + update_key);
                return promptForRetry(requestInfo, resource_type);
            }
        } else {
            Log.e(TAG_D, "using cache: " + update_key + " " + cacheExpiredDate);
        }

        if (KenPatcher.isPatcherEnabled()) {
            String patchedFilePath = KcUtils.getAppCacheFileDir(context, KcEnUtils.getPatchedCachePath().concat(path));
            String patchFilePath = KcUtils.getAppCacheFileDir(context,
                    ("/" + KcEnUtils.getAssetPath()).concat(path));
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
                String patchVersion = versionTable.getVersionValue(patchFilePath);
                if (!patchedFile.exists() || update_flag || patchVersion == null ||
                        !Objects.equals(versionTable.getVersionValue(patchFilePath), hash)) {
                    versionTable.putVersionValue(patchFilePath, hash);
                    Log.e(TAG_D, "needs repatch: " + patchedFilePath + " " + hash);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        usePatchedCache = patchImage(out_file_path, patchedFilePath, patchFilePath);
                    }
                } else {
                    Log.e(TAG_D, "using cached patched file: " + patchedFilePath + " " + hash);
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
            Log.e(TAG_G, log_path + " " + is.available());
            String type = ResourceProcess.isImage(resource_type) ? "image/png" : "application/json";
            return new WebResourceResponse(type, "utf-8", is);
        } catch (IOException e) {
            KcUtils.reportException(e);
            // Fail to load
            return promptForRetry(requestInfo, resource_type);
        }
    }

    private void showGadgetIpServerBlockedDialog() {
        activity.runOnUiThread(() -> {
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    Intent intent = new Intent(activity, EntranceActivity.class);
                    activity.startActivity(intent);
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
        if (!isRetryPromptEnabled) {
            return null;
        }

        final AtomicReference<Boolean> cancelled = new AtomicReference<>(false);

        final CountDownLatch retryReady = new CountDownLatch(1);
        activity.runOnUiThread(() -> {
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE: // yes
                        // User allow retry recovery
                        // We can proceed to next iteration
                        retryReady.countDown();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE: // no and never ask again
                        // User give up and it is ok to stop loading
                        // And change preference to never ask again
                        sharedPref.edit().putBoolean(PREF_DOWNLOAD_RETRY, false).apply();
                    case DialogInterface.BUTTON_NEUTRAL: // no
                    default:
                        // User give up and it is ok to stop loading
                        cancelled.set(true);
                }
                dialog.dismiss();
            };

            if (!activity.isFinishing()) {
                try {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
                    String path = requestInfo.path;
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
            if (ResourceProcess.isImage(resource_type)) {
                return processImageDataResource(requestInfo, resource_type);
            } else if (ResourceProcess.isAudio(resource_type)) {
                return processAudioFile(requestInfo, resource_type);
            }
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
            String main_js = patchMainScript(new String(byteArray, StandardCharsets.UTF_8), silent_mode);
            InputStream is = new ByteArrayInputStream(main_js.getBytes());
            return new WebResourceResponse("application/javascript", "utf-8", is);
        } else {
            return null;
        }
    }

    private WebResourceResponse processStylesheet(ResourceRequestInfo requestInfo) throws IOException {
        String url = requestInfo.url;
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

            if (url.contains("play.games.dmm.com/assets/index") & url.endsWith(".css")) {
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

    private WebResourceResponse processAudioFile(ResourceRequestInfo requestInfo, int resource_type) {
        String update_key = requestInfo.key;

        String path = requestInfo.path;
        String resource_url = requestInfo.fullUrl;
        String out_file_path = requestInfo.outputPath;

        String url = requestInfo.url;
        File file = new File(out_file_path);
        Log.e(TAG_E, "resource_url: " + resource_url);

        String cacheExpiredDate = versionTable.getCacheControlValue(update_key);
        String prevLastModified = versionTable.getVersionValue(update_key);

        int isCacheExpired = checkCacheExpired(cacheExpiredDate);
        boolean isDefaultValue = prevLastModified.equals(versionTable.getDefaultValue())
                        || cacheExpiredDate.equals(versionTable.getDefaultValue());
        if (!file.exists() || isDefaultValue || isCacheExpired == -1) prevLastModified = null;

        if (prevLastModified == null || isCacheExpired == 1) {
            JsonObject result = downloadResource(
                    resourceClient, resource_url, file, prevLastModified);

            if (result.has("response_code")) {
                int response_code = result.get("response_code").getAsInt();
                if (response_code == 200) {
                    String cache_expired = getCacheExpiredAt(result.get("cache_control").getAsString());
                    String last_modified = result.get("last_modified").getAsString();
                    versionTable.putCacheAndVersion(update_key, last_modified, cache_expired);
                    Log.e(TAG_D, update_key + " last_modified: " + last_modified);
                    Log.e(TAG_D, update_key + " cache_expired: " + cache_expired);
                } else if (response_code == 304) {
                    Log.e(TAG_D, update_key + " use cached resource (304)");
                } else {
                    Log.e(TAG_D, update_key + " response_code: " + response_code);
                }
            } else {
                Log.e(TAG_D, "download error: " + update_key);
                return promptForRetry(requestInfo, resource_type);
            }
        } else {
            Log.e(TAG_D, "using cache: " + update_key + " " + cacheExpiredDate);
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
            file = applyKenPatcherIfAvailable(path, file, false);
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            return new WebResourceResponse("audio/mpeg", "binary", is);
        } catch (IOException e) {
            KcUtils.reportException(e);
            // Fail to load
            return promptForRetry(requestInfo, resource_type);
        }
    }

    private WebResourceResponse processFontFile(ResourceRequestInfo requestInfo) throws IOException {
        String path = requestInfo.path;
        String update_key = requestInfo.key;
        String resource_url = requestInfo.fullUrl;
        String out_file_path = requestInfo.outputPath;

        File file = new File(out_file_path);
        Log.e(TAG_E, "resource_url: " + resource_url);

        String cacheExpiredDate = versionTable.getCacheControlValue(update_key);
        String prevLastModified = versionTable.getVersionValue(update_key);

        int isCacheExpired = checkCacheExpired(cacheExpiredDate);
        boolean isDefaultValue = prevLastModified.equals(versionTable.getDefaultValue())
                || cacheExpiredDate.equals(versionTable.getDefaultValue());
        if (!file.exists() || isDefaultValue || isCacheExpired == -1) prevLastModified = null;

        if (prevLastModified == null || isCacheExpired == 1) {
            JsonObject result = downloadResource(
                    resourceClient, resource_url, file, prevLastModified);

            if (result.has("response_code")) {
                int response_code = result.get("response_code").getAsInt();
                if (response_code == 200) {
                    String cache_expired = getCacheExpiredAt(result.get("cache_control").getAsString());
                    String last_modified = result.get("last_modified").getAsString();
                    versionTable.putCacheAndVersion(update_key, last_modified, cache_expired);
                    Log.e(TAG_D, update_key + " last_modified: " + last_modified);
                    Log.e(TAG_D, update_key + " cache_expired: " + cache_expired);
                } else if (response_code == 304) {
                    Log.e(TAG_D, update_key + " use cached resource (304)");
                } else {
                    Log.e(TAG_D, update_key + " response_code: " + response_code);
                }
            } else {
                Log.e(TAG_D, "download error: " + update_key);
            }
        } else {
            Log.e(TAG_D, "using cache: " + update_key + " " + cacheExpiredDate);
        }

        file = applyKenPatcherIfAvailable(path, file, false);
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
        Log.e(TAG_G, "playHourVoice after: " + data.getExtraDelay() + " msec");
        shipVoiceHandler.removeCallbacks(r);
        shipVoiceHandler.postDelayed(r, data.getExtraDelay());
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
        JsonObject result = downloadResource(resourceClient, url, null);
        Log.e(TAG_G, "IpBannedStatus: " + result);
        if (result.has("response_code")) {
            return result.get("response_code").getAsInt() == 403;
        } else {
            return false;
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

    private String getTouchEventPatchJs() {
        try {
            AssetManager as = context.getAssets();
            InputStream inputStream = as.open("touch_event_patch.js");

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = inputStream.read(buffer)) != -1;) {
                result.write(buffer, 0, length);
            }
            return result.toString("utf-8");
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

        // 2025.10 update: fix patch logic for silent mode (default value added)
        if (silent_mode) {
            Matcher invPatternMatcher = INIT_VOLUME_PATTERN.matcher(main_js);

            if (invPatternMatcher.find()) {
                String statement = invPatternMatcher.group(0);
                String varBgm = invPatternMatcher.group(2);
                String varSe = invPatternMatcher.group(3);
                String varVoice = invPatternMatcher.group(4);

                if (statement != null && varBgm != null && varSe != null && varVoice != null) {
                    String newStatement = statement.replace(varBgm, "0")
                            .replace(varSe, "0").replace(varVoice, "0");
                    main_js = main_js.replace(statement, newStatement);
                }
            }
        }

        Matcher howlPatternMatcher = HOWL_PATTERN.matcher(main_js);
        boolean howl_found = howlPatternMatcher.find();
        if (howl_found) {
            String _howl_fn = escapeMatchedGroup(howlPatternMatcher.group(1));
            if (_howl_fn != null) main_js = main_js.replaceAll(_howl_fn, "add_bgm");
        }

        // Rename the original "mouseout" and "mouseover" event name to custom names for objects to listen on
        // Reusing original names will cause a lot of conflict issues
        //main_js = main_js.replace("over:n.pointer?\"pointerover\":\"mouseover\"", "over:\"touchover\"");
        //main_js = main_js.replace("out:n.pointer?\"pointerout\":\"mouseout\"", "out:\"touchout\"");
        if (isCursorTouchMode) {
            main_js = TOUCH_EVENT_PATTERN.matcher(main_js).replaceFirst(
                    "down:void 0!==document.ontouchstart?'touchstart':'mousedown',\n" + "move:void 0!==document.ontouchstart?'touchmove':'mousemove',\n" + "up:void 0!==document.ontouchstart?'touchend':'mouseup',\n" + "over:'touchover',\n" + "out:'touchout'");
        }

        // manage bgm loading strategy with global mute variable for audio focus issue
        main_js = "var gb_h=null;\nfunction add_bgm(b){b.onend=function(){(global_mute||gb_h.volume()==0)&&(gb_h.unload(),console.log('unload'))};global_mute&&(b.autoplay=false);gb_h=new Howl(b);return gb_h;}\n"
                + (activity.isMuteMode() ? "var global_mute=1;Howler.mute(true);\n" : "var global_mute=0;Howler.mute(false);\n")
                + main_js;

        // Simulate mouse hover effects by dispatching new custom events "touchover" and "touchout"
        if (isCursorTouchMode) main_js += getTouchEventPatchJs();

        // add other script patches
        main_js = main_js + MUTE_LISTEN + CAPTURE_LISTEN + "\n"
                + KcsInterface.AXIOS_INTERCEPT_SCRIPT;

        return main_js;
    }

    // Reference: https://github.com/KC3Kai/KC3Kai/blob/master/src/library/modules/Translation.js
    private final Runnable clearSubtitle = new Runnable() {
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

    private File applyKenPatcherIfAvailable(String path, File originalFile, boolean updateFlag) {
        if (!KenPatcher.isPatcherEnabled()) return originalFile;

        String patchedFilePath = KcUtils.getAppCacheFileDir(context, KcEnUtils.getPatchedCachePath().concat(path));
        String patchFilePath = KcUtils.getAppCacheFileDir(context,
                ("/" + KcEnUtils.getAssetPath()).concat(path));

        File patchedFile = new File(patchedFilePath);
        File patchFile = new File(patchFilePath);

        if (!patchFile.exists()) return originalFile;

        boolean usePatchedCache = false;

        String patchStrings;
        if (patchFile.isDirectory()) {
            if (new File(patchFilePath + "/original").isDirectory()) {
                patchStrings = dirMD5(patchFilePath + "/original") + dirMD5(patchFilePath + "/patched");
            } else {
                patchStrings = dirMD5(patchFilePath);
            }
        } else {
            patchStrings = patchFilePath + "_" + patchFile.length() + "_" + patchFile.lastModified();
        }

        String hash = GetMD5HashOfString(patchStrings);
        String patchVersion = versionTable.getVersionValue(patchFilePath);

        if (!patchedFile.exists() || updateFlag || patchVersion == null ||
                !Objects.equals(patchVersion, hash)) {

            versionTable.putVersionValue(patchFilePath, hash);
            Log.e(TAG_P, "needs repatch: " + patchedFilePath + " " + hash);

            try {
                patchedFile.getParentFile().mkdirs();

                if (patchFile.isFile()) {
                    KcUtils.copyFileUsingStream(patchFile, patchedFile);
                    usePatchedCache = true;
                } else {
                    String ext = "";
                    String name = originalFile.getName();
                    int dot = name.lastIndexOf('.');
                    if (dot != -1) ext = name.substring(dot);

                    File originalPatch = new File(patchFilePath + "/original" + ext);
                    File patchedPatch = new File(patchFilePath + "/patched" + ext);

                    if (originalPatch.exists() && patchedPatch.exists()) {

                        byte[] originalBytes = KcUtils.getBytesFromInputStream(new FileInputStream(originalFile));
                        byte[] patchOriginalBytes = KcUtils.getBytesFromInputStream(new FileInputStream(originalPatch));

                        if (Arrays.equals(originalBytes, patchOriginalBytes)) {
                            KcUtils.copyFileUsingStream(patchedPatch, patchedFile);
                            Log.e("GOTO-P", "patched via original/patched match: " + path);
                            usePatchedCache = true;
                        } else {
                            Log.e("GOTO-P", "original mismatch, skipping patch: " + path);
                        }
                    }
                }

            } catch (IOException e) {
                Log.e("GOTO-P", KcUtils.getStringFromException(e));
                return originalFile;
            }

        } else {
            Log.e(TAG_P, "using cached patched file: " + patchedFilePath + " " + hash);
            usePatchedCache = true;
        }

        return usePatchedCache ? patchedFile : originalFile;
    }

    public static boolean patchImage(String ogDestination, String ptDestination, String patchFile) {
        try {
            if (ResourceProcess.isImage(ResourceProcess.getCurrentState(Uri.fromFile(new File(ptDestination))))) {
                Bitmap ogSpritesheet = BitmapFactory.decodeFile(ogDestination);
                File metadataFile = new File(getSpriteMetadataPath(ogDestination));
                Log.e(TAG_P, "patchImage-src: " + metadataFile.getAbsolutePath());
                File dest = new File(ptDestination);
                Log.e(TAG_P, "patchImage-desc: " + metadataFile.getAbsolutePath());
                if (!metadataFile.exists()) {
                    Bitmap ogImage = BitmapFactory.decodeFile(patchFile.concat("/original.png"));
                    Log.e(TAG_P, patchFile.concat("/original.png"));
                    if (ogSpritesheet != null && ogImage != null && KcEnUtils.bitmapEqual(ogSpritesheet, ogImage, 0.01f)) {
                        File source = new File(patchFile.concat("/patched.png"));
                        dest.getParentFile().mkdirs();
                        dest.createNewFile();
                        KcUtils.copyFileUsingStream(source, dest);
                        Log.e(TAG_P, "image patched: " + ptDestination);
                        return true;
                    } else {
                        Log.e(TAG_P, "image not patched: "
                                + patchFile.concat("/original.png") + " "
                                + (ogSpritesheet != null) + " "
                                + (ogImage != null) + " "
                                + KcEnUtils.bitmapEqual(ogSpritesheet, ogImage, 0.01f)
                        );
                    }
                } else {
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
                }
            }
        } catch (IOException e) {
            Log.e("GOTO-P", KcUtils.getStringFromException(e));
        }
        return false;
    }

    private static String getSpriteMetadataPath(String path) {
        if (path.endsWith(".jpg")) {
            return path.replace(".jpg", ".json");
        }
        if (path.endsWith(".png")) {
            return path.replace(".png", ".json");
        }
        return path + ".json";
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

            if (w == ogSpriteWidth && h == ogSpriteHeight && KcEnUtils.bitmapEqual(ogSprite, spritesheetSprite, 0.01f)) {
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
