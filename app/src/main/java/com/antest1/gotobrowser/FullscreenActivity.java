package com.antest1.gotobrowser;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.antest1.gotobrowser.Constants.ACTION_SHOWPANEL;
import static com.antest1.gotobrowser.Constants.AUTOCOMPLETE_NIT;
import static com.antest1.gotobrowser.Constants.AUTOCOMPLETE_OOI;
import static com.antest1.gotobrowser.Constants.CONNECT_NITRABBIT;
import static com.antest1.gotobrowser.Constants.CONN_DMM;
import static com.antest1.gotobrowser.Constants.CONN_KANSU;
import static com.antest1.gotobrowser.Constants.CONN_NITRABBIT;
import static com.antest1.gotobrowser.Constants.CONN_OOI;
import static com.antest1.gotobrowser.Constants.DMM_COOKIE;
import static com.antest1.gotobrowser.Constants.KANCOLLE_SERVER_LIST;
import static com.antest1.gotobrowser.Constants.OOI_SERVER_LIST;
import static com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT;
import static com.antest1.gotobrowser.Constants.PREF_CONNECTOR;
import static com.antest1.gotobrowser.Constants.PREF_DMM_ID;
import static com.antest1.gotobrowser.Constants.PREF_DMM_PASS;
import static com.antest1.gotobrowser.Constants.PREF_LANDSCAPE;
import static com.antest1.gotobrowser.Constants.PREF_LATEST_URL;
import static com.antest1.gotobrowser.Constants.PREF_LOCKMODE;
import static com.antest1.gotobrowser.Constants.PREF_MUTEMODE;
import static com.antest1.gotobrowser.Constants.PREF_PADDING;
import static com.antest1.gotobrowser.Constants.PREF_SILENT;
import static com.antest1.gotobrowser.Constants.PREF_VPADDING;
import static com.antest1.gotobrowser.Constants.REFRESH_CALL;
import static com.antest1.gotobrowser.Constants.REQUEST_BLOCK_RULES;
import static com.antest1.gotobrowser.Constants.RESIZE_CALL;
import static com.antest1.gotobrowser.Constants.RESIZE_DMM;
import static com.antest1.gotobrowser.Constants.RESIZE_OSAPI;
import static com.antest1.gotobrowser.Constants.URL_DMM;
import static com.antest1.gotobrowser.Constants.URL_DMM_FOREIGN;
import static com.antest1.gotobrowser.Constants.URL_DMM_LOGIN;
import static com.antest1.gotobrowser.Constants.URL_KANSU;
import static com.antest1.gotobrowser.Constants.URL_NITRABBIT;
import static com.antest1.gotobrowser.Constants.URL_OOI;
import static com.antest1.gotobrowser.Constants.URL_OSAPI;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;

public class FullscreenActivity extends AppCompatActivity {
    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;

    private VersionDatabase versionTable;
    private AudioManager audioManager;
    private final Handler mHideHandler = new Handler();
    private WebView mContentView;
    private View mHorizontalControlView, mVerticalControlView;
    private View broswerPanel;
    private View menuRefresh, menuAspect, menuMute, menuLock, menuCaption, menuClose;
    private GestureDetector mDetector;

    private SeekBar mSeekBarH, mSeekBarV;
    private boolean isPancelActive = false;
    private boolean isStartedFlag = false;
    private boolean savedStreamMuted = false;
    private String connector_url = "";
    private String connector_url_default = "";
    private String login_id = "";
    private String login_password = "";
    private boolean pause_flag = false;
    private final OkHttpClient resourceClient = new OkHttpClient();
    private boolean isMuteMode, isLockMode;
    private MediaPlayer bgmPlayer;
    private boolean isBgmPlaying = false;
    private float bgmVolume = 1.0f;
    private Map<String, Integer> seMap = new HashMap<>();
    private SoundPool sePlayer;
    private float seVolume = 1.0f;
    private MediaPlayer voicePlayer;
    private boolean isVoicePlaying = false;
    private float voiceVolume = 1.0f;
    private MediaPlayer titleVoicePlayer;
    private boolean isBattleMode = false;
    private Map<String, String> filenameToShipId = new HashMap<>();
    private String currentCookieHost = "";
    private List<String> currentTitleCall = new ArrayList<>();
    ScheduledExecutorService executor;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    // | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            // mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private boolean checkImageCached(String url) {
        if (url.contains("img/common")) return true;
        if (url.contains("img/remodel")) return true;
        if (url.contains("img/arsenal")) return true;
        if (url.contains("img/repair")) return true;
        if (url.contains("img/supply")) return true;
        if (url.contains("img/sally")) return true;
        if (url.contains("img/duty")) return true;
        if (url.contains("img/map")) return true;
        if (url.contains("img/battle")) return true;
        if (url.contains("resources/map")) return true;
        return false;
    }

    private BackPressCloseHandler backPressCloseHandler;

    @SuppressLint({"SetJavaScriptEnabled", "ApplySharedPref"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_fullscreen);
        Intent intent = getIntent();
        final SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_key), Context.MODE_PRIVATE);

        if (sharedPref.getBoolean(PREF_LANDSCAPE, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        }

        executor = Executors.newScheduledThreadPool(1);

        login_id = sharedPref.getString(PREF_DMM_ID, ""); // intent.getStringExtra("login_id");
        login_password = sharedPref.getString(PREF_DMM_PASS, "");
        versionTable = new VersionDatabase(getApplicationContext(), null, VERSION_TABLE_VERSION);

        mVisible = true;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mDetector = new GestureDetector(this, new SettingButtonGestureListener());

        mContentView = findViewById(R.id.main_browser);
        mContentView.setOnTouchListener((v, event) -> mDetector.onTouchEvent(event));
        mHorizontalControlView = findViewById(R.id.control_component);
        mHorizontalControlView.setVisibility(View.GONE);
        mVerticalControlView = findViewById(R.id.vcontrol_component);
        mVerticalControlView.setVisibility(View.GONE);

        isPancelActive = intent != null && ACTION_SHOWPANEL.equals(intent.getAction());
        // setMemoryCache(getFilesDir().getAbsolutePath().concat("/cache/"));
        // Log.e("GOTO", "memory cache: " + image_cache.size());

        broswerPanel = findViewById(R.id.browser_panel);
        broswerPanel.setVisibility(isPancelActive ? View.VISIBLE : View.GONE);

        menuRefresh = findViewById(R.id.menu_refresh);
        menuRefresh.setOnClickListener(v -> setDefaultPage());

        menuAspect = findViewById(R.id.menu_aspect);
        menuAspect.setOnClickListener(v -> {
            DisplayMetrics dimension= new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dimension);
            int width = dimension.widthPixels;
            int height = dimension.heightPixels;

            int adjust_padding = sharedPref.getInt(PREF_PADDING, getDefaultPadding(width, height));
            int adjust_vpadding = sharedPref.getInt(PREF_VPADDING, 0);

            mHorizontalControlView.setVisibility(View.VISIBLE);
            ((TextView) mHorizontalControlView.findViewById(R.id.control_text))
                    .setText(String.valueOf(adjust_padding));
            mVerticalControlView.setVisibility(View.VISIBLE);
            ((TextView) mVerticalControlView.findViewById(R.id.vcontrol_text))
                    .setText(String.valueOf(adjust_vpadding));
        });
        menuCaption = findViewById(R.id.menu_cc);

        isMuteMode = sharedPref.getBoolean(PREF_MUTEMODE, false);
        menuMute = findViewById(R.id.menu_mute);
        menuMute.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), isMuteMode ? R.color.panel_red : R.color.black));
        menuMute.setOnClickListener(v -> {
            isMuteMode = !isMuteMode;
            setCurrentVolume(isMuteMode);
            if (isMuteMode) {
                menuMute.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.panel_red));
                sharedPref.edit().putBoolean(PREF_MUTEMODE, true).commit();
            } else {
                menuMute.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
                sharedPref.edit().putBoolean(PREF_MUTEMODE, false).commit();
            }
        });

        isLockMode = sharedPref.getBoolean(PREF_LOCKMODE, false);
        menuLock = findViewById(R.id.menu_lock);
        menuLock.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), isLockMode ? R.color.panel_red : R.color.black));
        menuLock.setOnClickListener(v -> {
            isLockMode = !isLockMode;
            if (isLockMode) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                menuLock.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.panel_red));
                sharedPref.edit().putBoolean(PREF_LOCKMODE, true).commit();
            } else {
                if (sharedPref.getBoolean(PREF_LANDSCAPE, false)) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                }
                menuLock.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
                sharedPref.edit().putBoolean(PREF_LOCKMODE, false).commit();
            }
        });

        menuClose = findViewById(R.id.menu_close);
        menuClose.setOnClickListener(v -> {
            broswerPanel.setVisibility(View.GONE);
        });

        backPressCloseHandler = new BackPressCloseHandler(this);
        bgmPlayer = new MediaPlayer();
        voicePlayer = new MediaPlayer();
        titleVoicePlayer = new MediaPlayer();
        sePlayer = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        mContentView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                sharedPref.edit().putString(PREF_LATEST_URL, url).apply();
                if (url.contains(URL_DMM_FOREIGN)) {
                    mContentView.evaluateJavascript(DMM_COOKIE, null);
                    mContentView.evaluateJavascript("location.href='".concat(URL_DMM).concat("';"), null);
                }
                if (url.contains(URL_DMM_LOGIN) || url.equals(URL_KANSU) || url.equals(URL_OOI)) {
                    mContentView.evaluateJavascript(
                            String.format(Locale.US, AUTOCOMPLETE_OOI,
                                    login_id, login_password), null);
                    if (url.contains(URL_DMM_LOGIN)) {
                        mContentView.evaluateJavascript(DMM_COOKIE, null);
                    }
                }
                if (url.equals(Constants.URL_NITRABBIT)) {
                    mContentView.evaluateJavascript(CONNECT_NITRABBIT, null);
                    mContentView.evaluateJavascript(
                            String.format(Locale.US, AUTOCOMPLETE_NIT,
                                    login_id, login_password), null);
                }
                if (url.contains(Constants.URL_OSAPI) || url.contains(URL_DMM)) {
                    isStartedFlag = true;
                    DisplayMetrics dimension= new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(dimension);
                    int width = dimension.widthPixels;
                    int height = dimension.heightPixels;
                    int adjust_padding = sharedPref.getInt(PREF_PADDING, getDefaultPadding(width, height));
                    int adjust_vpadding = sharedPref.getInt(PREF_VPADDING, 0);
                    boolean adjust_layout = sharedPref.getBoolean(PREF_ADJUSTMENT, false);
                    if (adjust_layout) {
                        if (url.contains(URL_OSAPI)) mContentView.evaluateJavascript(String.format(
                                Locale.US, RESIZE_OSAPI, adjust_padding, adjust_vpadding), null);
                        else if (url.contains(URL_DMM)) mContentView.evaluateJavascript(String.format(
                                Locale.US, RESIZE_DMM, adjust_padding, adjust_vpadding), null);
                    }
                    if (url.contains(URL_OSAPI)) {
                        mContentView.evaluateJavascript(String.format(Locale.US,
                                REFRESH_CALL, connector_url_default), value -> {
                                    Log.e("GOTO", "invalid: " + value);
                                    if (value.equals("true")) {
                                        sharedPref.edit().putString(PREF_LATEST_URL, connector_url_default).apply();
                                        setDefaultPage();
                                    }
                                });
                    }
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Map<String, String> header = request.getRequestHeaders();
                    Uri source = request.getUrl();
                    String host = source.getHost();
                    String accept = header.get("Accept");
                    boolean is_image = accept != null && accept.contains("image") && source.toString().contains("kcs2");
                    boolean is_audio = accept != null && source.toString().contains(".mp3");
                    boolean is_json = accept != null && accept.contains("json") && source.toString().contains(".json");

                    String url = source.toString();
                    for (String rule : REQUEST_BLOCK_RULES) {
                        if (url.contains(rule)) {
                            Log.e("GOTO", "blocked: ".concat(url));
                            return new WebResourceResponse("text/css", "utf-8", getEmptyStream());
                        }
                    }

                    if(url.contains("/kcs2/img/") && host != null) {
                        currentCookieHost = host.concat("/kcs2/");
                        String cookie = CookieManager.getInstance().getCookie(currentCookieHost);
                        if (cookie != null) {
                            String[] data = cookie.split(";");
                            for (String item: data) {
                                String[] row = item.trim().split("=");
                                if (!row[1].matches("[0-9]+")) continue;
                                String key = row[0];
                                float value = (float)Integer.parseInt(row[1]) / 100;
                                if (key.equals("vol_bgm")) bgmVolume = value;
                                if (key.equals("vol_voice")) voiceVolume = value;
                                if (key.equals("vol_se")) seVolume = value;
                            }
                        }
                        // Log.e("GOTO", cookie);
                    }

                    if (url.contains("kcscontents/css/common.css")) {
                        String replace_css = "#globalNavi, #contentsWrap {display:none;} body {background-color: black;}";
                        InputStream is = new ByteArrayInputStream(replace_css.getBytes());
                        return new WebResourceResponse("text/css", "utf-8", is);
                    }

                    try {
                        if (source.getPath() != null && source.getLastPathSegment() != null) {
                            //Log.e("GOTO", source.getPath());
                            //Log.e("GOTO", header.toString());
                            String path = source.getPath();
                            String filename = source.getLastPathSegment();

                            if (filename.equals("version.json") || filename.contains("index.php")) {
                                currentTitleCall.clear();
                                return super.shouldInterceptRequest(view, request);
                            }
                            if (path.contains("ooi.css")) { // block ooi.moe background
                                AssetManager as = getAssets();
                                InputStream is = as.open("ooi.css");
                                return new WebResourceResponse("text/css", "utf-8", is);
                            }

                            if (path.contains("/api_start2/")) {
                                boolean update_flag = false;
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
                                Log.e("GOTO", versionTable.getValue("api_start2"));
                                String apipath = getApplicationContext().getFilesDir().getAbsolutePath()
                                        .concat("/cache/").concat("api_start2");
                                File file = new File(apipath);
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

                                InputStream buf = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)));
                                Gson gson = new Gson();
                                Reader reader = new InputStreamReader(buf);
                                JsonObject api_data = gson.fromJson(reader, JsonObject.class).getAsJsonObject("api_data");
                                JsonArray api_mst_shipgraph = api_data.getAsJsonArray("api_mst_shipgraph");
                                for (JsonElement item: api_mst_shipgraph) {
                                    JsonObject ship = item.getAsJsonObject();
                                    String shipId = ship.get("api_id").getAsString();
                                    String shipFn = ship.get("api_filename").getAsString();
                                    filenameToShipId.put(shipFn, shipId);
                                }
                                Log.e("GOTO", "shipgraph: " + String.valueOf(filenameToShipId.size()));
                                return super.shouldInterceptRequest(view, request);
                            }

                            if (path.contains("/api_port/port")) {
                                Log.e("GOTO","run executor");
                                executor = Executors.newScheduledThreadPool(1);
                                executor.scheduleAtFixedRate(portVolumeCheckRunnable, 0, 1, TimeUnit.SECONDS);

                            } else if (path.contains("/api")) {
                                if (!executor.isShutdown()) executor.shutdown();
                            }

                            String fullpath = String.format(Locale.US, "http://%s%s", host, path);
                            String outputpath = getApplicationContext().getFilesDir().getAbsolutePath()
                                    .concat("/cache/").concat(path.replace(filename, "").substring(1));
                            String filepath = outputpath.concat(filename);

                            boolean update_flag = false;
                            String version_key = source.getPath();
                            stopMp3(bgmPlayer, filepath);

                            if (is_image || is_audio || is_json) {
                                String version = "";
                                if (source.getQueryParameterNames().contains("version")) {
                                    version = source.getQueryParameter("version");
                                }

                                if (!versionTable.getValue(version_key).equals(version)) {
                                    update_flag = true;
                                    versionTable.putValue(version_key, version);
                                    Log.e("GOTO", "cache resource " + version_key +  ": " + version);
                                } else {
                                    Log.e("GOTO", "resource " + version_key +  " found: " + version);
                                }
                            }

                            if (is_image) {
                                File file = new File(filepath);
                                if (!file.exists() || update_flag) {
                                    File dir = new File(outputpath);
                                    if (!dir.exists()) dir.mkdirs();
                                    Request imageRequest = new Request.Builder().url(fullpath).build();
                                    Response response = resourceClient.newCall(imageRequest).execute();
                                    ResponseBody body = response.body();
                                    // InputStream in = new BufferedInputStream(new URL(fullpath).openStream());
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
                                    } else {
                                        return super.shouldInterceptRequest(view, request);
                                    }
                                }

                                Log.e("GOTO", "load from disk: " + path);
                                InputStream is = new BufferedInputStream(new FileInputStream(file));
                                return new WebResourceResponse("image/png", "utf-8", is);
                            }

                            if (is_json || is_audio) {
                                File dir = new File(outputpath);
                                if (!dir.exists()) dir.mkdirs();

                                File file = new File(filepath);
                                if (!file.exists() || update_flag) {
                                    InputStream in = new BufferedInputStream(new URL(fullpath).openStream());
                                    byte[] buffer = new byte[8 * 1024];
                                    int bytes;
                                    FileOutputStream fos = new FileOutputStream(file);
                                    while ((bytes = in.read(buffer)) != -1) {
                                        fos.write(buffer, 0, bytes);
                                    }
                                    fos.close();
                                } else {
                                    Log.e("GOTO", "load from disk: " + filepath);
                                }
                                InputStream is = new BufferedInputStream(new FileInputStream(file));
                                if (is_json) return new WebResourceResponse("application/json", "utf-8", is);
                                if (is_audio) {
                                    if (url.contains("resources/se")) playSe(sePlayer, file, seVolume);
                                    else if (url.contains("/kcs/sound/kc")) {
                                        String info = version_key.replace("/kcs/sound/kc", "").replace(".mp3", "");
                                        String[] fn_code = info.split("/");
                                        int voiceline = -1;
                                        String voice_filename = fn_code[0];
                                        if (filenameToShipId.containsKey(voice_filename)) {
                                            voiceline = KcVoiceUtils.getVoiceLineByFilename(filenameToShipId.get(fn_code[0]), fn_code[1]);
                                        } else {
                                            voiceline = KcVoiceUtils.getVoiceLineByFilename(fn_code[0], fn_code[1]);
                                        }
                                        Log.e("GOTO", "file info: " + info);
                                        Log.e("GOTO", "voiceline: " + String.valueOf(voiceline));
                                        if (voiceline >= 30 && voiceline <= 53) { // hourly voiceline
                                            Calendar now = Calendar.getInstance();
                                            int minute = now.get(Calendar.MINUTE);
                                            int second = now.get(Calendar.SECOND);
                                            if (minute == 0 && second <= 2) {
                                                playMp3("voice", voicePlayer, file, voiceVolume);
                                            }
                                        } else {
                                            if (isBattleMode && (file.length() / 1024) < 100) playSe(sePlayer, file, voiceVolume);
                                            else playMp3("voice", voicePlayer, file, voiceVolume);
                                        }
                                    }
                                    else if (url.contains("/voice/titlecall")) {
                                        currentTitleCall.add(file.getAbsolutePath());
                                        if (currentTitleCall.size() == 2) {
                                            playTitleCall(currentTitleCall, 0, voiceVolume);
                                        }
                                    }
                                    else if (url.contains("resources/bgm")) playMp3("bgm", bgmPlayer, file, bgmVolume);
                                    return new WebResourceResponse("audio/mpeg", "binary", getEmptyStream());
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

        });
        mContentView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                return super.onJsAlert(view, url, message, result);
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                final WebView newWebView = new WebView(FullscreenActivity.this);
                ImageView closeButton = findViewById(R.id.dmm_browser_close);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(newWebView, true);
                }
                closeButton.setVisibility(View.VISIBLE);

                newWebView.getSettings().setJavaScriptEnabled(true);
                newWebView.getSettings().setLoadWithOverviewMode(true);
                newWebView.getSettings().setSaveFormData(true);
                newWebView.getSettings().setDomStorageEnabled(true);
                newWebView.getSettings().setUseWideViewPort(true);
                newWebView.getSettings().setSupportZoom(true);
                newWebView.setWebViewClient(new WebViewClient() {
                    @TargetApi(Build.VERSION_CODES.N)
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String url = request.getUrl().toString();
                        view.loadUrl(url);
                        return true;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }

                });

                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mContentView.removeView(newWebView);
                        v.setVisibility(View.GONE);
                    }
                });
                mContentView.addView(newWebView);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                super.onCloseWindow(window);
            }
        });

        if (Build.VERSION.SDK_INT >= 21) {
            mContentView.getSettings().setMixedContentMode(WebSettings
                    .MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(mContentView, true);
        } else {
            CookieManager.getInstance().setAcceptCookie(true);
        }

        boolean isSilentMode = sharedPref.getBoolean(PREF_SILENT, false);
        if (isSilentMode) setSoundMuteCookie();

        findViewById(R.id.cookietest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cookie = CookieManager.getInstance().getCookie("http://125.6.189.215/kcs2/");
                Toast.makeText(getApplicationContext(), cookie, Toast.LENGTH_LONG).show();
            }
        });

        DisplayMetrics dimension= new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimension);
        int width = dimension.widthPixels;
        int height = dimension.heightPixels;
        int adjust_padding = sharedPref.getInt(PREF_PADDING, getDefaultPadding(width, height));
        int adjust_vpadding = sharedPref.getInt(PREF_VPADDING, 0);
        mSeekBarH = mHorizontalControlView.findViewById(R.id.control_main);
        mSeekBarH.setProgress(getHorizontalProgressFromPref(adjust_padding));
        mSeekBarH.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isStartedFlag && fromUser) {
                    int vprogress = sharedPref.getInt(PREF_VPADDING, 0);
                    mContentView.evaluateJavascript(String.format(Locale.US,
                            RESIZE_CALL, convertHorizontalProgress(progress), vprogress), null);
                    sharedPref.edit().putInt(PREF_PADDING, convertHorizontalProgress(progress)).apply();
                    ((TextView) mHorizontalControlView.findViewById(R.id.control_text))
                            .setText(String.valueOf(convertHorizontalProgress(progress)));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        mHorizontalControlView.findViewById(R.id.control_exit)
                .setOnClickListener(v -> mHorizontalControlView.setVisibility(View.GONE));

        mSeekBarV = mVerticalControlView.findViewById(R.id.vcontrol_main);
        mSeekBarV.setProgress(getVerticalProgressFromPref(adjust_vpadding));
        mSeekBarV.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isStartedFlag) {
                    int hprogress = sharedPref.getInt(PREF_PADDING, getDefaultPadding(width, height));
                    mContentView.evaluateJavascript(String.format(Locale.US,
                            RESIZE_CALL, hprogress, convertVerticalProgress(progress)), null);
                    sharedPref.edit().putInt(PREF_VPADDING, convertVerticalProgress(progress)).apply();
                    ((TextView) mVerticalControlView.findViewById(R.id.vcontrol_text))
                            .setText(String.valueOf(convertVerticalProgress(progress)));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        mVerticalControlView.findViewById(R.id.vcontrol_exit)
                .setOnClickListener(v -> mVerticalControlView.setVisibility(View.GONE));

        // mContentView.setInitialScale(1);
        mContentView.getSettings().setLoadWithOverviewMode(true);
        mContentView.getSettings().setSaveFormData(true);
        mContentView.getSettings().setDatabaseEnabled(true);
        mContentView.getSettings().setDomStorageEnabled(true);
        mContentView.getSettings().setUseWideViewPort(true);
        mContentView.getSettings().setJavaScriptEnabled(true);
        mContentView.getSettings().setSupportZoom(false);
        mContentView.getSettings().setTextZoom(100);
        mContentView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mContentView.getSettings().setSupportMultipleWindows(true);
        // mContentView.getSettings().setBuiltInZoomControls(true);
        mContentView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0");
        mContentView.setScrollbarFadingEnabled(true);
        mContentView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mContentView.getSettings().setAppCacheEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mContentView.getSettings().setOffscreenPreRaster(true);
        }
        mContentView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebView.setWebContentsDebuggingEnabled(true);
        setDefaultPage();
    }

    @Override
    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        // mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("GOTO", "onPause");
        boolean is_multi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode();
        if (!is_multi) {
            Log.e("GOTO", "is_not_multi");
            pause_flag = true;
            mContentView.pauseTimers();
            if (bgmPlayer.isPlaying()) bgmPlayer.pause();
            if (voicePlayer.isPlaying()) voicePlayer.pause();
            if (titleVoicePlayer.isPlaying()) titleVoicePlayer.pause();
            sePlayer.autoPause();
        } else {
            if (pause_flag) {
                mContentView.resumeTimers();
                pause_flag = false;
                if (!bgmPlayer.isPlaying() && isBgmPlaying) bgmPlayer.start();
                if (!voicePlayer.isPlaying() && isVoicePlaying) voicePlayer.start();
                if (!titleVoicePlayer.isPlaying() && isBgmPlaying) titleVoicePlayer.start();
                sePlayer.autoResume();
            }
            Log.e("GOTO", "is_multi");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("GOTO", "onResume");
        boolean is_multi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode();
        if (pause_flag) {
            mContentView.resumeTimers();
            pause_flag = false;
            if (!bgmPlayer.isPlaying() && isBgmPlaying) bgmPlayer.start();
            if (!voicePlayer.isPlaying() && isVoicePlaying) voicePlayer.start();
            sePlayer.autoResume();
        }
        //setVolumeMute(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //setVolumeMute(false);
        bgmPlayer.stop();
        bgmPlayer.release();
        sePlayer.release();
        voicePlayer.release();
        titleVoicePlayer.release();
        mContentView.removeAllViews();
        mContentView.destroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_key), Context.MODE_PRIVATE);
        boolean adjust_layout = sharedPref.getBoolean(PREF_ADJUSTMENT, false);

        DisplayMetrics dimension = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimension);
        int width = dimension.widthPixels;
        int height = dimension.heightPixels;
        int adjust_padding = sharedPref.getInt(PREF_PADDING, getDefaultPadding(width, height));
        int adjust_vpadding = sharedPref.getInt(PREF_VPADDING, 0);
        if (mSeekBarH != null) mSeekBarH.setProgress(adjust_padding);

        if (isStartedFlag) {
            if (adjust_layout) mContentView.evaluateJavascript(
                    String.format(Locale.US, RESIZE_CALL, adjust_padding, adjust_vpadding), null);
        }
    }
    private int getHorizontalProgressFromPref(int value) {return value / 2; }
    private int convertHorizontalProgress(int progress) { return progress * 2; }
    private int getVerticalProgressFromPref(int value) {return value / 2; }
    private int convertVerticalProgress(int progress) { return progress * 2; }

    private int getDefaultPadding(int width, int height) {
        if (width < height) {
            int temp = width; width = height; height = temp;
        }
        int ratio_val = width  * 18 / height;
        return (ratio_val - 30) * 20;
    }

    public void setDefaultPage() {
        mContentView.resumeTimers();
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_key), Context.MODE_PRIVATE);
        String pref_connector = sharedPref.getString(PREF_CONNECTOR, null);
        if (CONN_DMM.equals(pref_connector)) {
            connector_url_default = URL_DMM;
            connector_url = sharedPref.getString(PREF_LATEST_URL, URL_DMM);
            mContentView.loadUrl(connector_url);
        } else if (CONN_KANSU.equals(pref_connector) || CONN_OOI.equals(pref_connector)) {
            if (CONN_KANSU.equals(pref_connector)) {
                connector_url_default = URL_KANSU;
                connector_url = sharedPref.getString(PREF_LATEST_URL, URL_KANSU);
            } else {
                connector_url_default = URL_OOI;
                connector_url = sharedPref.getString(PREF_LATEST_URL, URL_OOI);
            }

            if (connector_url_default.equals(connector_url)) {
                String postdata = "";
                try {
                    postdata = String.format(Locale.US, "login_id=%s&password=%s&mode=4",
                            URLEncoder.encode(login_id, "utf-8"),
                            URLEncoder.encode(login_password, "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.e("GOTO", postdata);
                mContentView.postUrl(connector_url, postdata.getBytes());
            } else {
                mContentView.loadUrl(connector_url);
            }
        } else if (CONN_NITRABBIT.equals(pref_connector)) {
            connector_url_default = URL_NITRABBIT;
            connector_url = sharedPref.getString(PREF_LATEST_URL, URL_NITRABBIT);
            mContentView.loadUrl(connector_url);
        } else {
            finish();
        }
    }

    public void setSoundMuteCookie(){
        CookieSyncManager syncManager = CookieSyncManager.createInstance(mContentView.getContext());
        syncManager.sync();

        CookieManager cookieManager = CookieManager.getInstance();
        for (String server: KANCOLLE_SERVER_LIST) {
            String url = "http://".concat(server).concat("/kcs2/");
            cookieManager.setCookie(url, "vol_bgm=0;");
            cookieManager.setCookie(url, "vol_se=0;");
            cookieManager.setCookie(url, "vol_voice=0;");
        }
        for (String server: OOI_SERVER_LIST) {
            String url = "http://".concat(server).concat("/kcs2/");
            cookieManager.setCookie(url, "vol_bgm=0;");
            cookieManager.setCookie(url, "vol_se=0;");
            cookieManager.setCookie(url, "vol_voice=0;");
        }
    }

    public void setVolumeMute(boolean is_mute) {
        if (is_mute) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) {
                    savedStreamMuted = true;
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                }
            } else {
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (savedStreamMuted) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
                    savedStreamMuted = false;
                }
            } else {
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            }
        }
    }
    public InputStream getEmptyStream() {
        InputStream empty = new InputStream() {
            @Override
            public int read() {
                return -1;
            }
        };
        return empty;
    }

    public boolean shouldAudioLoop(String url) {
        if (url.contains("resources/bgm") && !url.contains("fanfare")) {
            return true;
        } else {
            return false;
        }
    }

    public void stopMp3(MediaPlayer player, String url) {
        String[] STOP_FLAG = {
            "api_req_sortie",
            "api_req_battle_midnight",
            "api_req_combined_battle",
            "api_req_practice",
            "battle_result/battle_result_main"
        };

        isBattleMode = false;
        for (String pattern: STOP_FLAG) {
            if (url.contains(pattern)) {
                fadeOut(bgmPlayer, 1000);
                isBattleMode = true;
            }
        }

        if (url.contains("api_req_map")) {
            voicePlayer.stop();
            voicePlayer.reset();
        }
    }

    public void setCurrentVolume(boolean mute_mode) {
        Log.e("GOTO", "setCurrentVolume: " + mute_mode);
        boolean bgm_playing = bgmPlayer.isPlaying();
        boolean voice_playing = voicePlayer.isPlaying();
        boolean title_playing = titleVoicePlayer.isPlaying();
        if (bgm_playing) bgmPlayer.pause();
        if (voice_playing) voicePlayer.pause();
        if (title_playing) titleVoicePlayer.pause();

        if (mute_mode) {
            bgmPlayer.setVolume(0.0f, 0.0f);
            voicePlayer.setVolume(0.0f, 0.0f);
            titleVoicePlayer.setVolume(0.0f, 0.0f);
            for (Integer key: seMap.values()) {
                sePlayer.setVolume(key, 0.0f, 0.0f);
            }
        } else {
            bgmPlayer.setVolume(bgmVolume, bgmVolume);
            voicePlayer.setVolume(voiceVolume, voiceVolume);
            titleVoicePlayer.setVolume(voiceVolume, voiceVolume);
            for (Map.Entry<String, Integer> item: seMap.entrySet()) {
                String url = item.getKey();
                Integer sid = item.getValue();
                if (url.contains("/kcs/sound/kc")) {
                    sePlayer.setVolume(sid, voiceVolume, voiceVolume);
                } else {
                    sePlayer.setVolume(sid, seVolume, seVolume);
                }
            }
        }
        if (bgm_playing) bgmPlayer.start();
        if (voice_playing) voicePlayer.start();
        if (title_playing) titleVoicePlayer.start();
    }

    public void playMp3(String tag, MediaPlayer player, File file, float volume) {
        if (isMuteMode) volume = 0.0f;
        try {
            String path = file.getAbsolutePath();
            if (player.isPlaying()) {
                player.stop();
            }
            player.setOnCompletionListener(mp -> {
                if (tag.equals("bgm")) isBgmPlaying = false;
                if (tag.equals("voice")) isVoicePlaying = false;
            });
            player.reset();
            player.setVolume(volume, volume);
            player.setLooping(shouldAudioLoop(path));
            player.setDataSource(path);
            player.prepare();
            player.start();
            if (tag.equals("bgm")) isBgmPlaying = true;
            if (tag.equals("voice")) isVoicePlaying = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playSe(SoundPool pool, File file, float volume) {
        if (isMuteMode) volume = 0.0f;
        String path = file.getAbsolutePath();
        int soundId = -1;
        if (seMap != null) {
            if (!seMap.containsKey(path)) {
                soundId = pool.load(path, 1);
                float vol_f = volume;
                pool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
                    pool.play(sampleId, vol_f, vol_f,  1,  0,  1.0f);
                });
                seMap.put(path, soundId);
            } else {
                soundId = seMap.get(path);
                pool.play(soundId, volume, volume,  1,  0,  1.0f);
            }
        }
    }

    public void fadeOut(final MediaPlayer _player, final int duration) {
        final float deviceVolume = bgmVolume;
        final Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            private float time = duration;
            private float volume = 0.0f;
            @Override
            public void run() {
                if (!_player.isPlaying())
                    _player.start();
                // can call h again after work!
                time -= 100;
                volume = (deviceVolume * time) / duration;
                _player.setVolume(volume, volume);
                if (time > 0)
                    h.postDelayed(this, 100);
                else {
                    _player.stop();
                    _player.reset();
                    _player.setVolume(deviceVolume, deviceVolume);
                }
            }
        }, 100); // 1 second delay (takes millis)
    }

    public void playTitleCall(List<String> list, int idx, float volume) {
        if (isMuteMode) volume = 0.0f;
        try {
            titleVoicePlayer.reset();
            float vol_f = volume;
            titleVoicePlayer.setOnCompletionListener(mp -> {
                isBgmPlaying = false;
                if (idx == 0) playTitleCall(list, 1, vol_f);
            });
            titleVoicePlayer.setLooping(false);
            titleVoicePlayer.setVolume(volume, volume);
            titleVoicePlayer.setDataSource(list.get(idx));
            titleVoicePlayer.prepare();
            titleVoicePlayer.start();
            isBgmPlaying = true;
        } catch (IOException e) {
            Log.e("GOTO", "playTitleCall Error:");
            e.printStackTrace();
        }
    }

    Runnable portVolumeCheckRunnable = new Runnable() {
        public void run() {
            String cookie = CookieManager.getInstance().getCookie(currentCookieHost);
            String[] data = cookie.split(";");
            for (String item: data) {
                String[] row = item.trim().split("=");
                if (!row[1].matches("[0-9]+")) continue;
                String key = row[0];
                float value = (float)Integer.parseInt(row[1]) / 100;
                if (key.equals("vol_bgm")) {
                    bgmVolume = value;
                    if (isMuteMode) bgmPlayer.setVolume(0.0f, 0.0f);
                    else bgmPlayer.setVolume(bgmVolume, bgmVolume);
                }
                if (key.equals("vol_voice")) {
                    voiceVolume = value;
                    if (isMuteMode) voicePlayer.setVolume(0.0f, 0.0f);
                    else voicePlayer.setVolume(voiceVolume, voiceVolume);
                }
                if (key.equals("vol_se")) {
                    seVolume = value;
                }
            }
            // Log.e("GOTO", cookie);
        }
    };

    class SettingButtonGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.e("TAG", "onFling: ");
            Log.e("TAG", event1.toString());
            Log.e("TAG", event2.toString());
            Log.e("TAG", velocityX + " " + velocityY);
            if (event1.getX() < 200 && velocityX > 2000) {
                broswerPanel.setVisibility(View.VISIBLE);
            } else if (event1.getX() < 1500 && velocityX < -2000) {
                broswerPanel.setVisibility(View.GONE);
            }
            return super.onFling(event1, event2, velocityX, velocityY);
        }
    }
}
