package com.antest1.gotobrowser;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
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
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.antest1.gotobrowser.Helpers.KcUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import com.antest1.gotobrowser.Helpers.MediaPlayerPool;

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
import static com.antest1.gotobrowser.Constants.PREF_KEEPMODE;
import static com.antest1.gotobrowser.Constants.PREF_LANDSCAPE;
import static com.antest1.gotobrowser.Constants.PREF_LATEST_URL;
import static com.antest1.gotobrowser.Constants.PREF_LOCKMODE;
import static com.antest1.gotobrowser.Constants.PREF_MUTEMODE;
import static com.antest1.gotobrowser.Constants.PREF_PADDING;
import static com.antest1.gotobrowser.Constants.PREF_SHOWCC;
import static com.antest1.gotobrowser.Constants.PREF_SILENT;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.PREF_VPADDING;
import static com.antest1.gotobrowser.Constants.REFRESH_CALL;
import static com.antest1.gotobrowser.Constants.REQUEST_BLOCK_RULES;
import static com.antest1.gotobrowser.Constants.RESIZE_CALL;
import static com.antest1.gotobrowser.Constants.RESIZE_DMM;
import static com.antest1.gotobrowser.Constants.RESIZE_OOI_3;
import static com.antest1.gotobrowser.Constants.RESIZE_OSAPI;
import static com.antest1.gotobrowser.Constants.URL_DMM;
import static com.antest1.gotobrowser.Constants.URL_DMM_FOREIGN;
import static com.antest1.gotobrowser.Constants.URL_DMM_LOGIN;
import static com.antest1.gotobrowser.Constants.URL_KANSU;
import static com.antest1.gotobrowser.Constants.URL_NITRABBIT;
import static com.antest1.gotobrowser.Constants.URL_OOI;
import static com.antest1.gotobrowser.Constants.URL_OOI_3;
import static com.antest1.gotobrowser.Constants.URL_OSAPI;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.getStringFromException;

public class FullscreenActivity extends AppCompatActivity {
    public static final String OPEN_KANCOLLE = "open_kancolle";
    public static final String OPEN_RES_DOWN = "open_res_down";
    private static final int UI_ANIMATION_DELAY = 300;
    private static final int AUDIO_POOL_LIMIT = 10;
    private int uiOption;
    private String action;

    private VersionDatabase versionTable;
    private AudioManager audioManager;
    private final Handler mHideHandler = new Handler();
    private WebViewL mContentView;
    private View mHorizontalControlView, mVerticalControlView;
    private View broswerPanel;
    private View menuRefresh, menuAspect, menuMute, menuLock, menuCaption, menuBrightOn, menuClose;
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
    private boolean isMuteMode, isLockMode, isKeepMode, isCaptionMode;
    private MediaPlayer bgmPlayer;
    private boolean isBgmPlaying = false;
    private boolean isOnPractice = false;
    private float bgmVolume = 1.0f;
    private float fadeOutBgmVolume = 1.0f;
    private int currentMapId = 0;
    private int currentBattleBgmId = 0;
    private boolean isFadeoutRunning = false;
    private Map<String, Integer> seMap = new HashMap<>();
    private SoundPool sePlayer;
    private float seVolume = 1.0f;
    private MediaPlayerPool voicePlayers;
    private boolean isVoicePlaying = false;
    private float voiceVolume = 1.0f;
    private MediaPlayer titleVoicePlayer;
    private final Handler shipVoiceHandler = new Handler();
    private boolean isBattleMode = false;
    private Map<String, String> filenameToShipId = new HashMap<>();
    private String currentCookieHost = "";
    private boolean isSubtitleLoaded = false;
    private TextView subtitleText;
    private final Handler clearSubHandler = new Handler();
    private List<String> titlePath = new ArrayList<>();
    private List<String> titleFiles = new ArrayList<>();
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
            uiOption = mContentView.getSystemUiVisibility();
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
        uiOption = getWindow().getDecorView().getSystemUiVisibility();
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_fullscreen);
        Intent intent = getIntent();
        if (intent != null) action = intent.getAction();

        final SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_key), Context.MODE_PRIVATE);

        if (sharedPref.getBoolean(PREF_LANDSCAPE, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        }

        if (sharedPref.getBoolean(PREF_KEEPMODE, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        menuRefresh.setOnClickListener(v -> {
            mContentView.pauseTimers();
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    FullscreenActivity.this);
            alertDialogBuilder.setTitle(getString(R.string.app_name));
            alertDialogBuilder
                    .setCancelable(false)
                    .setMessage(getString(R.string.refresh_msg))
                    .setPositiveButton(R.string.action_ok,
                            (dialog, id) -> {
                                if (KcUtils.checkIsPlaying(bgmPlayer)) bgmPlayer.stop();
                                setDefaultPage();
                            })
                    .setNegativeButton(R.string.action_cancel,
                            (dialog, id) -> {
                                dialog.cancel();
                                mContentView.resumeTimers();
                            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        });

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

        isKeepMode = sharedPref.getBoolean(PREF_KEEPMODE, false);
        menuBrightOn = findViewById(R.id.menu_brighton);
        menuBrightOn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), isKeepMode ? R.color.panel_red : R.color.black));
        menuBrightOn.setOnClickListener(v -> {
            isKeepMode = !isKeepMode;
            if (isKeepMode) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                menuBrightOn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.panel_red));
                sharedPref.edit().putBoolean(PREF_KEEPMODE, true).commit();
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                menuBrightOn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
                sharedPref.edit().putBoolean(PREF_KEEPMODE, false).commit();
            }
        });

        isCaptionMode = sharedPref.getBoolean(PREF_SHOWCC, false);

        subtitleText = findViewById(R.id.subtitle_view);
        subtitleText.setOnClickListener(v -> clearSubHandler.postDelayed(clearSubtitle, 250));
        subtitleText.setVisibility(OPEN_KANCOLLE.equals(action) && isCaptionMode ? View.VISIBLE : View.GONE);
        menuCaption = findViewById(R.id.menu_cc);
        menuCaption.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), isCaptionMode ? R.color.panel_red : R.color.black));
        menuCaption.setOnClickListener(v -> {
            isCaptionMode = !isCaptionMode;
            if (isCaptionMode) {
                subtitleText.setVisibility(View.VISIBLE);
                menuCaption.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.panel_red));
                sharedPref.edit().putBoolean(PREF_SHOWCC, true).commit();
            } else {
                subtitleText.setVisibility(View.GONE);
                menuCaption.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
                sharedPref.edit().putBoolean(PREF_SHOWCC, false).commit();
            }
        });

        menuClose = findViewById(R.id.menu_close);
        menuClose.setOnClickListener(v -> {
            broswerPanel.setVisibility(View.GONE);
        });

        backPressCloseHandler = new BackPressCloseHandler(this);
        bgmPlayer = new MediaPlayer();
        voicePlayers = new MediaPlayerPool(1);
        voicePlayers.setOnAllCompletedListener((pool, lastPlayer) -> {
            isVoicePlaying = false;
        });
        titleVoicePlayer = new MediaPlayer();
        sePlayer = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);



        // defaultSubtitleMargin = getDefaultSubtitleMargin();
        setSubtitleMargin(sharedPref.getInt(PREF_PADDING, 0));
        String subtitle_local = sharedPref.getString(PREF_SUBTITLE_LOCALE, "en");
        KcSoundUtils.loadQuoteAnnotation(getApplicationContext());
        isSubtitleLoaded = KcSoundUtils.loadQuoteData(getApplicationContext(), subtitle_local);

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
                if (url.contains(Constants.URL_OSAPI) || url.contains(Constants.URL_OOI_3) || url.contains(URL_DMM)) {
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
                        else if (url.contains(URL_OOI_3)) mContentView.evaluateJavascript(String.format(
                                Locale.US, RESIZE_OOI_3, adjust_padding, adjust_vpadding), null);
                        else if (url.contains(URL_DMM)) mContentView.evaluateJavascript(String.format(
                                Locale.US, RESIZE_DMM, adjust_padding, adjust_vpadding), null);
                    }
                    if (url.contains(URL_OSAPI) || url.contains(URL_OOI_3)) {
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
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                Uri source = Uri.parse(url);
                String host = source.getHost();
                if (OPEN_KANCOLLE.equals(action)) {
                    boolean is_image = url.contains("kcs2") && (url.contains(".png") || url.contains(".jpg"));
                    boolean is_audio = url.contains(".mp3");
                    boolean is_json = url.contains("json");
                    boolean is_js = url.contains("/js/") && url.contains(".js");
                    WebResourceResponse response = processWebRequest(source, is_image, is_audio, is_json, is_js);
                    if (response != null) return response;
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Uri source = request.getUrl();
                    Map<String, String> header = request.getRequestHeaders();
                    String host = source.getHost();
                    String accept = header.get("Accept");
                    if (OPEN_KANCOLLE.equals(action)) {
                        boolean is_image = accept != null && accept.contains("image") && source.toString().contains("kcs2");
                        boolean is_audio = accept != null && source.toString().contains(".mp3");
                        boolean is_json = accept != null && accept.contains("json") && source.toString().contains(".json");
                        boolean is_js = source.toString().contains("/js/") && source.toString().contains(".js");
                        WebResourceResponse response = processWebRequest(source, is_image, is_audio, is_json, is_js);
                        if (response != null) return response;
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
                final WebViewL newWebView = new WebViewL(FullscreenActivity.this);
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
                    setSubtitleMargin(adjust_padding);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        mContentView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url)
                    .addHeader("User-Agent", userAgent)
                    .addHeader("Content-Type", mimetype)
                    .addHeader("Cache-Control", "no-cache")
                    .build();
            Log.e("GOTO", url);
            Uri uri = Uri.parse(url);
            String version = uri.getQueryParameter("v");
            if (version == null) version = "";

            String filename = uri.getLastPathSegment();
            String outputpath = "";
            if (uri.getPath().contains(getString(R.string.resource_download_prefix))) {
                outputpath = uri.getPath().replace(getString(R.string.resource_download_prefix), "")
                        .replace(".zip", "/");
            } else if (uri.getPath().contains("kcs2-all")) {
                outputpath = "/";
            }

            Log.e("GOTO", outputpath);
            Log.e("GOTO", "version: " + version);

            ProgressDialog progress = new ProgressDialog(FullscreenActivity.this);
            progress.setMessage(String.format(Locale.US, "Downloading %s...", filename));
            progress.setCancelable(false);
            progress.setProgressNumberFormat(null);
            progress.setProgressPercentFormat(null);
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setIndeterminate(true);
            progress.show();

            final String outputpath_f = outputpath;
            final String version_f = version;
            new Thread() {
                public void run() {
                    try {
                        Response response = client.newCall(request).execute();
                        InputStream in = response.body().byteStream();
                        runOnUiThread(() -> progress.setMessage(String.format(Locale.US, "Extracting %s...", filename)));
                        KcUtils.unzipResource(getApplicationContext(), in, outputpath_f, versionTable, version_f);
                        runOnUiThread(() -> {
                            if (progress.isShowing()) progress.dismiss();
                            Toast.makeText(FullscreenActivity.this, String.format(Locale.US, "Process finished: %s", filename), Toast.LENGTH_LONG).show();
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        });
                    } catch (NullPointerException | IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(FullscreenActivity.this, getStringFromException(e), Toast.LENGTH_LONG).show();
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        });
                    }
                }
            }.start();
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

        String pref_connector = sharedPref.getString(PREF_CONNECTOR, null);

        mContentView.setInitialScale(1);
        mContentView.getSettings().setLoadWithOverviewMode(true);
        mContentView.getSettings().setSaveFormData(true);
        mContentView.getSettings().setDatabaseEnabled(true);
        mContentView.getSettings().setDomStorageEnabled(true);
        mContentView.getSettings().setUseWideViewPort(true);
        mContentView.getSettings().setJavaScriptEnabled(true);
        mContentView.getSettings().setTextZoom(100);
        mContentView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mContentView.getSettings().setSupportMultipleWindows(true);
        // mContentView.getSettings().setBuiltInZoomControls(true);
        mContentView.getSettings().setSupportZoom(false);
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
        if (OPEN_RES_DOWN.equals(action)) {
            Intent intent = new Intent(FullscreenActivity.this, EntranceActivity.class);
            startActivity(intent);
            finish();
        } else {
            backPressCloseHandler.onBackPressed();
        }
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
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            mContentView.setSystemUiVisibility( uiOption );
        }
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
            if (isBgmPlaying && KcUtils.checkIsPlaying(bgmPlayer)) bgmPlayer.pause();
            if (isVoicePlaying && voicePlayers.isAnyPlaying()) voicePlayers.pauseAll();
            if (isBgmPlaying && KcUtils.checkIsPlaying(titleVoicePlayer)) titleVoicePlayer.pause();
            sePlayer.autoPause();
        } else {
            if (pause_flag) {
                mContentView.resumeTimers();
                pause_flag = false;
                if (isBgmPlaying && !KcUtils.checkIsPlaying(bgmPlayer)) bgmPlayer.start();
                if (isVoicePlaying && !voicePlayers.isAnyPlaying()) voicePlayers.startAll();
                if (isBgmPlaying && !KcUtils.checkIsPlaying(titleVoicePlayer)) titleVoicePlayer.start();
                sePlayer.autoResume();
            }
            Log.e("GOTO", "is_multi");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("GOTO", "onResume");
        pause_flag = false;
        mContentView.resumeTimers();
        if (isBgmPlaying && !KcUtils.checkIsPlaying(bgmPlayer)) bgmPlayer.start();
        if (isVoicePlaying && !voicePlayers.isAnyPlaying()) voicePlayers.startAll();
        sePlayer.autoResume();
        //setVolumeMute(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //setVolumeMute(false);
        bgmPlayer.stop();
        bgmPlayer.release();
        sePlayer.release();
        voicePlayers.release();
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
        setSubtitleMargin(adjust_padding);
        if (isStartedFlag) {
            if (adjust_layout) mContentView.evaluateJavascript(
                    String.format(Locale.US, RESIZE_CALL, adjust_padding, adjust_vpadding), null);
        }
    }

    public WebResourceResponse processWebRequest(Uri source, boolean is_image, boolean is_audio, boolean is_json, boolean is_js) {
        String url = source.toString();
        String host = source.getHost();
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

        String path = source.getPath();
        String filename = source.getLastPathSegment();

        try {
            if (path != null && filename != null) {
                Log.e("GOTO", source.getPath());
                //Log.e("GOTO", header.toString());
                if (filename.equals("version.json") || filename.contains("index.php")) {
                    titlePath.clear();
                    titleFiles.clear();
                    return null;
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
                    JsonArray api_mst_ship = api_data.getAsJsonArray("api_mst_ship");
                    JsonArray api_mst_mapbgm = api_data.getAsJsonArray("api_mst_mapbgm");
                    KcSoundUtils.buildShipGraph(api_mst_ship);
                    KcSoundUtils.buildMapBgmGraph(api_mst_mapbgm);
                    for (JsonElement item: api_mst_shipgraph) {
                        JsonObject ship = item.getAsJsonObject();
                        String shipId = ship.get("api_id").getAsString();
                        String shipFn = ship.get("api_filename").getAsString();
                        filenameToShipId.put(shipFn, shipId);
                    }
                    Log.e("GOTO", "ship_filename: " + filenameToShipId.size());
                    return null;
                }

                if (path.contains("/api_port/port")) {
                    currentMapId = 0;
                    Log.e("GOTO","run executor");
                    executor = Executors.newScheduledThreadPool(1);
                    executor.scheduleAtFixedRate(portVolumeCheckRunnable, 0, 1, TimeUnit.SECONDS);

                } else if (path.contains("/api")) {
                    if (!executor.isShutdown()) executor.shutdown();
                }

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

                isBattleMode = isBattleMode || path.contains("api_req_battle")
                        || path.contains("api_req_map") || path.contains("api_req_practice");
                isBattleMode = isBattleMode && !path.contains("api_port");
                isOnPractice = isOnPractice || path.contains("/kcs2/img/prac/prac_main");
                voicePlayers.setStreamsLimit(isBattleMode ? AUDIO_POOL_LIMIT : 1);
                Log.e("GOTO ", "isBattleMode: " + isBattleMode);
                Log.e("GOTO", "voicePlayers: streams_limit " + (isBattleMode ? AUDIO_POOL_LIMIT : 1));

                String fullpath = String.format(Locale.US, "http://%s%s", host, path);
                String outputpath = getApplicationContext().getFilesDir().getAbsolutePath()
                        .concat("/cache/").concat(path.replace(filename, "").substring(1));
                String filepath = outputpath.concat(filename);

                boolean update_flag = false;
                String source_path = source.getPath();
                stopMp3(bgmPlayer, filepath);

                if (is_image || is_audio || is_json || is_js) {
                    String version = "";
                    if (source.getQueryParameterNames().contains("version")) {
                        version = source.getQueryParameter("version");
                    }

                    if (!versionTable.getValue(source_path).equals(version)) {
                        update_flag = true;
                        versionTable.putValue(source_path, version);
                        Log.e("GOTO", "cache resource " + source_path +  ": " + version);
                    } else {
                        Log.e("GOTO", "resource " + source_path +  " found: " + version);
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
                            return null;
                        }
                    }

                    Log.e("GOTO", "load from disk: " + path);
                    InputStream is = new BufferedInputStream(new FileInputStream(file));
                    return new WebResourceResponse("image/png", "utf-8", is);
                }

                if (is_json || is_audio || is_js) {
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
                    if (is_js) {
                        if (url.contains("kcs2/js/")) {
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            int nRead;
                            byte[] data = new byte[1024];
                            while ((nRead = is.read(data, 0, data.length)) != -1) {
                                buffer.write(data, 0, nRead);
                            }
                            buffer.flush();
                            is.close();

                            byte[] byteArray = buffer.toByteArray();
                            String main_js = new String(byteArray, StandardCharsets.UTF_8);
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

                            is = new ByteArrayInputStream(main_js.getBytes());
                            // Log.e("GOTO", main_js);
                        }
                        return new WebResourceResponse("application/javascript", "utf-8", is);
                    }
                    if (is_audio) {
                        if (url.contains("resources/se")) playSe(sePlayer, file, seVolume);
                        else if (url.contains("/kcs/sound/kc")) {
                            String info = source_path.replace("/kcs/sound/kc", "").replace(".mp3", "");
                            String[] fn_code = info.split("/");
                            String voiceline = "";
                            String voice_filename = fn_code[0];
                            String voice_code = fn_code[1];
                            String ship_id = voice_filename;
                            if (filenameToShipId.containsKey(voice_filename)) {
                                ship_id = filenameToShipId.get(voice_filename);
                                voiceline = KcSoundUtils.getVoiceLineByFilename(ship_id, voice_code);

                            } else {
                                voiceline = KcSoundUtils.getVoiceLineByFilename(voice_filename, voice_code);
                            }
                            Log.e("GOTO", "file info: " + info);
                            Log.e("GOTO", "voiceline: " + String.valueOf(voiceline));
                            int voiceline_value = Integer.parseInt(voiceline);
                            if (voiceline_value >= 30 && voiceline_value <= 53) { // hourly voiceline
                                Date now = new Date();
                                String voiceline_time = String.format(Locale.US, "%02d:00:00", voiceline_value - 30);
                                SimpleDateFormat time_fmt = new SimpleDateFormat("HH:mm:ss");
                                Date time_src = time_fmt.parse(time_fmt.format(now));
                                Date time_tgt = time_fmt.parse(voiceline_time);
                                long diff_msec = time_tgt.getTime() - time_src.getTime();
                                if (voiceline_value == 30) diff_msec += 86400000;
                                //KcSoundUtils.setHourlyVoiceInfo(file.getAbsolutePath(), ship_id, voiceline);
                                Runnable r = new VoiceSubtitleRunnable(file.getAbsolutePath(), ship_id, voiceline);
                                shipVoiceHandler.removeCallbacks(r);
                                shipVoiceHandler.postDelayed(r, diff_msec);
                                Log.e("GOTO", "playHourVoice after: " + diff_msec + " msec");
                            } else {
                                setSubtitle(ship_id, voiceline);
                                if (ship_id.equals("9998") && false) { // temp code: play abyssal sound from browser
                                    return null;
                                } else {
                                    playVoice(file, voiceVolume);
                                }
                            }
                        }
                        else if (url.contains("/voice/titlecall_")) {
                            String info = source_path.replace("/kcs2/resources/voice/", "").replace(".mp3", "");
                            titlePath.add(info);
                            titleFiles.add(file.getAbsolutePath());
                            if (titleFiles.size() == 2) {
                                playTitleCall(titlePath, titleFiles, 0, voiceVolume);
                            }
                        }
                        else if (url.contains("resources/bgm")) {
                            if (isBgmPlaying) {
                                bgmPlayer.stop();
                            }
                            bgmPlayer.release();
                            bgmPlayer = new MediaPlayer();
                            playMp3("bgm", bgmPlayer, file, bgmVolume);
                        }
                        return new WebResourceResponse("audio/mpeg", "binary", getEmptyStream());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        if (OPEN_RES_DOWN.equals(action)) {
            mContentView.loadUrl(getString(R.string.resource_download_link));
        } else if (OPEN_KANCOLLE.equals(action)) {
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
                    connector_url = URL_KANSU; sharedPref.getString(PREF_LATEST_URL, URL_KANSU);
                } else {
                    connector_url_default = URL_OOI;
                    connector_url = URL_OOI; // sharedPref.getString(PREF_LATEST_URL, URL_OOI);
                }

                if (connector_url_default.equals(connector_url)) {
                    String postdata = "";
                    try {
                        int connect_mode = connector_url_default.equals(URL_OOI) ? 3 : 4;
                        postdata = String.format(Locale.US, "login_id=%s&password=%s&mode=%d",
                                URLEncoder.encode(login_id, "utf-8"),
                                URLEncoder.encode(login_password, "utf-8"),
                                connect_mode);
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
            "battle_result/battle_result_main.json",
            "battle/battle_main.json",
            "/kcs2/resources/se/217.mp3"
        };

        for (String pattern: STOP_FLAG) {
            if (url.contains(pattern)) {
                boolean fadeout_flag = true;
                boolean shutter_call = url.contains("/se/217.mp3");
                if (shutter_call) {
                    if (isOnPractice) {
                        isOnPractice = false;
                        continue;
                    }
                    JsonObject bgmData = KcSoundUtils.getMapBgmGraph(currentMapId);
                    if (bgmData != null) {
                        JsonArray normal_bgm = bgmData.getAsJsonArray("api_map_bgm");
                        boolean normal_diff = normal_bgm.get(0).getAsInt() != normal_bgm.get(1).getAsInt();
                        boolean normal_flag = currentBattleBgmId == normal_bgm.get(0).getAsInt() && normal_diff;

                        JsonArray boss_bgm = bgmData.getAsJsonArray("api_boss_bgm");
                        boolean boss_diff = boss_bgm.get(0).getAsInt() != boss_bgm.get(1).getAsInt();
                        boolean boss_flag = currentBattleBgmId == boss_bgm.get(0).getAsInt() && boss_diff;

                        fadeout_flag = normal_flag || boss_flag;
                    }
                }
                if (fadeout_flag) {
                    fadeOut(bgmPlayer, 1000);
                    break;
                }
            }
        }

        String[] STOP_V_FLAG = {
            "api_req_map",
            "api_get_member/slot_item"
        };
        for (String pattern: STOP_V_FLAG) {
            if (url.contains(pattern)) {
                voicePlayers.stopAll();
                voicePlayers.resetAll();
            }
        }
    }

    public void setCurrentVolume(boolean mute_mode) {
        Log.e("GOTO", "setCurrentVolume: " + mute_mode);
        boolean bgm_playing = bgmPlayer.isPlaying();
        boolean voice_playing = voicePlayers.isAnyPlaying();
        boolean title_playing = titleVoicePlayer.isPlaying();
        if (bgm_playing) bgmPlayer.pause();
        if (voice_playing) voicePlayers.pauseAll();
        if (title_playing) titleVoicePlayer.pause();

        if (mute_mode) {
            bgmPlayer.setVolume(0.0f, 0.0f);
            voicePlayers.setVolumeAll(0.0f, 0.0f);
            titleVoicePlayer.setVolume(0.0f, 0.0f);
            for (Integer key: seMap.values()) {
                sePlayer.setVolume(key, 0.0f, 0.0f);
            }
        } else {
            bgmPlayer.setVolume(bgmVolume, bgmVolume);
            voicePlayers.setVolumeAll(voiceVolume, voiceVolume);
            titleVoicePlayer.setVolume(voiceVolume, voiceVolume);
            for (Map.Entry<String, Integer> item: seMap.entrySet()) {
                String url = item.getKey();
                Integer sid = item.getValue();
                sePlayer.setVolume(sid, seVolume, seVolume);
            }
        }
        if (bgm_playing) bgmPlayer.start();
        if (voice_playing) voicePlayers.startAll();
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
            });
            player.reset();
            player.setVolume(volume, volume);
            player.setLooping(shouldAudioLoop(path));
            player.setDataSource(path);
            player.prepare();
            player.start();
            if (tag.equals("bgm")) isBgmPlaying = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playVoice(File file, float volume) {
        MediaPlayer player = new MediaPlayer();

        playMp3("voice", player, file, volume);

        voicePlayers.addToPool(player);
        isVoicePlaying = true;
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
        if (isFadeoutRunning || !isBgmPlaying) return;
        isFadeoutRunning = true;
        fadeOutBgmVolume = isMuteMode ? 0.0f : bgmVolume;
        final int FADE_DURATION = duration;
        final int FADE_INTERVAL = 100;
        final float MAX_VOLUME = bgmVolume;
        int numberOfSteps = FADE_DURATION / FADE_INTERVAL;
        final float deltaVolume = MAX_VOLUME / (float) numberOfSteps;

        final Timer timer = new Timer(true);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    _player.setVolume(fadeOutBgmVolume, fadeOutBgmVolume);
                    fadeOutBgmVolume -= deltaVolume;
                    if(fadeOutBgmVolume < 0.0f){
                        _player.stop();
                        _player.reset();
                        timer.cancel();
                        timer.purge();
                        isFadeoutRunning = false;
                        isBgmPlaying = false;
                        _player.setVolume(bgmVolume, bgmVolume);
                    }
                } catch (IllegalStateException e) {
                    _player.reset();
                    timer.cancel();
                    timer.purge();
                    isFadeoutRunning = false;
                    isBgmPlaying = false;
                    _player.setVolume(bgmVolume, bgmVolume);
                }
            }
        };
        timer.schedule(timerTask, 0, FADE_INTERVAL);
    }

    public void playTitleCall(List<String> info, List<String> list, int idx, float volume) {
        if (isMuteMode) volume = 0.0f;
        try {
            String[] fn_code = info.get(idx).split("/");
            String voice_filename = fn_code[0];
            String voice_code = fn_code[1];
            setSubtitle(voice_filename, voice_code);
            titleVoicePlayer.reset();
            float vol_f = volume;
            titleVoicePlayer.setOnCompletionListener(mp -> {
                isBgmPlaying = false;
                if (idx == 0) playTitleCall(info, list, 1, vol_f);
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
                    if (isMuteMode) voicePlayers.setVolumeAll(0.0f, 0.0f);
                    else voicePlayers.setVolumeAll(voiceVolume, voiceVolume);
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

    // Reference: https://github.com/KC3Kai/KC3Kai/blob/master/src/library/modules/Translation.js
    public Runnable clearSubtitle = () -> subtitleText.setText("");
    public void setSubtitle(String id, String code) {
        if (isCaptionMode) {
            shipVoiceHandler.removeCallbacksAndMessages(null);
            JsonObject subtitle = KcSoundUtils.getQuoteString(id, code);
            Log.e("GOTO", subtitle.toString());
            for (String key: subtitle.keySet()) {
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
        SubtitleRunnable(String text) { subtitle_text = text; }

        @Override
        public void run() {
            runOnUiThread(() -> {
                clearSubHandler.removeCallbacks(clearSubtitle);
                if (isSubtitleLoaded) {
                    subtitle_text = subtitle_text.replace("<br>", "\n");
                    subtitle_text = subtitle_text.replace("<br />", "\n");
                    subtitle_text = subtitle_text.replace("<br>", "\n");
                } else {
                    subtitle_text = getString(R.string.no_subtitle_file);
                }
                subtitleText.setText(subtitle_text);
                int delay = KcSoundUtils.getDefaultTiming(subtitle_text);
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
                if (!pause_flag) {
                    File file = new File(path);
                    playVoice(file, voiceVolume);
                }
            }
            setSubtitle(ship_id, voiceline);
        }
    }

    public int getDefaultSubtitleMargin() {
        FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) subtitleText.getLayoutParams();
        return param.leftMargin;
    }

    public void setSubtitleMargin(int value) {
        FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) subtitleText.getLayoutParams();
        param.setMargins(param.leftMargin + value, param.topMargin, param.rightMargin + value, param.bottomMargin);
        subtitleText.setLayoutParams(param);
    }
}
