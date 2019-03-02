package com.antest1.gotobrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import static com.antest1.gotobrowser.Constants.ACTION_WITHLC;
import static com.antest1.gotobrowser.Constants.AUTOCOMPLETE_NIT;
import static com.antest1.gotobrowser.Constants.AUTOCOMPLETE_OOI;
import static com.antest1.gotobrowser.Constants.CONNECT_NITRABBIT;
import static com.antest1.gotobrowser.Constants.CONN_KANSU;
import static com.antest1.gotobrowser.Constants.CONN_NITRABBIT;
import static com.antest1.gotobrowser.Constants.CONN_OOI;
import static com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT;
import static com.antest1.gotobrowser.Constants.PREF_CONNECTOR;
import static com.antest1.gotobrowser.Constants.PREF_LANDSCAPE;
import static com.antest1.gotobrowser.Constants.PREF_LATEST_URL;
import static com.antest1.gotobrowser.Constants.PREF_PADDING;
import static com.antest1.gotobrowser.Constants.PREF_SILENT;
import static com.antest1.gotobrowser.Constants.REFRESH_CALL;
import static com.antest1.gotobrowser.Constants.RESIZE_OSAPI;
import static com.antest1.gotobrowser.Constants.RESIZE_OSAPI_CALL;
import static com.antest1.gotobrowser.Constants.SERVER_LIST;
import static com.antest1.gotobrowser.Constants.URL_KANSU;
import static com.antest1.gotobrowser.Constants.URL_NITRABBIT;
import static com.antest1.gotobrowser.Constants.URL_OOI;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;

public class FullscreenActivity extends AppCompatActivity {
    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;

    private VersionDatabase versionTable;
    private AudioManager audioManager;
    private final Handler mHideHandler = new Handler();
    private WebView mContentView;
    private View mControllerView;
    private SeekBar mSeekbar;
    private boolean isControllerActive = false;
    private boolean isStartedFlag = false;
    private boolean savedStreamMuted = false;
    private String connector_url = "";
    private String connector_url_default = "";
    private String login_id = "";
    private String login_password = "";

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
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private BackPressCloseHandler backPressCloseHandler;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        Intent intent = getIntent();
        final SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_key), Context.MODE_PRIVATE);

        if (sharedPref.getBoolean(PREF_LANDSCAPE, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        login_id = intent.getStringExtra("login_id");
        login_password = intent.getStringExtra("login_pw");
        if (login_id == null) login_id = "";
        if (login_password == null) login_password = "";
        versionTable = new VersionDatabase(getApplicationContext(), null, VERSION_TABLE_VERSION);

        String pref_connector = sharedPref.getString(PREF_CONNECTOR, null);
        if (CONN_KANSU.equals(pref_connector)) {
            connector_url_default = URL_KANSU;
            connector_url = sharedPref.getString(PREF_LATEST_URL, URL_KANSU);
        } else if (CONN_OOI.equals(pref_connector)) {
            connector_url_default = URL_OOI;
            connector_url = sharedPref.getString(PREF_LATEST_URL, URL_OOI);
        } else if (CONN_NITRABBIT.equals(pref_connector)) {
            connector_url_default = URL_NITRABBIT;
            connector_url = sharedPref.getString(PREF_LATEST_URL, URL_NITRABBIT);
        } else {
            finish();
        }

        mVisible = true;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mContentView = findViewById(R.id.main_browser);
        mControllerView = findViewById(R.id.control_component);
        mControllerView.setVisibility(View.GONE);
        isControllerActive = intent != null && ACTION_WITHLC.equals(intent.getAction());

        // window-level acceleration
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        backPressCloseHandler = new BackPressCloseHandler(this);

        mContentView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                sharedPref.edit().putString(PREF_LATEST_URL, url).apply();
                if (url.equals(URL_KANSU) || url.equals(URL_OOI)) {
                    mContentView.evaluateJavascript(
                            String.format(Locale.US, AUTOCOMPLETE_OOI,
                                    login_id, login_password), null);
                }
                if (url.equals(Constants.URL_NITRABBIT)) {
                    mContentView.evaluateJavascript(CONNECT_NITRABBIT, null);
                    mContentView.evaluateJavascript(
                            String.format(Locale.US, AUTOCOMPLETE_NIT,
                                    login_id, login_password), null);
                }
                if (url.contains(Constants.URL_OSAPI)) {
                    isStartedFlag = true;
                    DisplayMetrics dimension= new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(dimension);
                    int width = dimension.widthPixels;
                    int height = dimension.heightPixels;
                    int adjust_padding = sharedPref.getInt(PREF_PADDING, getDefaultPadding(width, height));
                    boolean adjust_layout = sharedPref.getBoolean(PREF_ADJUSTMENT, false);
                    if (adjust_layout) mContentView.evaluateJavascript(String.format(
                            Locale.US, RESIZE_OSAPI, adjust_padding), null);
                    mContentView.evaluateJavascript(String.format(Locale.US,
                            REFRESH_CALL, connector_url_default), null);
                    if (isControllerActive) {
                        mControllerView.setVisibility(View.VISIBLE);
                        ((TextView) mControllerView.findViewById(R.id.control_text))
                                .setText(String.valueOf(adjust_padding));
                    }
                }
                if(url.contains("test")){ // temp code
                    CookieSyncManager syncManager = CookieSyncManager.createInstance(mContentView.getContext());
                    CookieManager cookieManager = CookieManager.getInstance();
                    String cookie = cookieManager.getCookie(url);
                    syncManager.sync();
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    Map<String, String> header = request.getRequestHeaders();
                    Uri source = request.getUrl();
                    String version = "";
                    if (source.getQueryParameterNames().contains("version")) {
                        version = source.getQueryParameter("version");
                    }
                    String host = source.getHost();
                    String accept = header.get("Accept");
                    boolean is_image = accept != null && accept.contains("image") && source.toString().contains("kcs2");
                    boolean is_audio = accept != null && source.toString().contains(".mp3");
                    boolean is_json = accept != null && accept.contains("json") && source.toString().contains(".json");

                    try {
                        if (source.getPath() != null && source.getLastPathSegment() != null) {
                            //Log.e("GOTO", source.getPath());
                            //Log.e("GOTO", header.toString());

                            String filename = source.getLastPathSegment();
                            if (filename.equals("version.json") || filename.contains("index.php")) {
                                return super.shouldInterceptRequest(view, request);
                            }
                            if (source.getPath().contains("ooi.css")) { // block ooi.moe background
                                AssetManager as = getAssets();
                                InputStream is = as.open("ooi.css");
                                return new WebResourceResponse("text/css", "utf-8", is);
                            }

                            String fullpath = String.format(Locale.US, "http://%s%s", host, source.getPath());
                            String outputpath = getApplicationContext().getFilesDir().getAbsolutePath()
                                    .concat("/cache/").concat(source.getPath().replace(filename, "").substring(1));
                            String filepath = outputpath.concat(filename);

                            boolean update_flag = false;
                            String version_key = source.getPath();

                            if (is_image || is_audio || is_json) {
                                if (!versionTable.getValue(version_key).equals(version)) {
                                    update_flag = true;
                                    versionTable.putValue(version_key, version);
                                    Log.e("GOTO", "cache resource " + version_key +  ": " + version);
                                } else {
                                    Log.e("GOTO", "resource " + version_key +  " found: " + version);
                                }
                            }

                            if (is_image) {
                                File dir = new File(outputpath);
                                if (!dir.exists()) dir.mkdirs();

                                File file = new File(filepath);
                                if (!file.exists() || update_flag) {
                                    InputStream in = new URL(fullpath).openStream();
                                    Bitmap image = BitmapFactory.decodeStream(in);
                                    FileOutputStream fos = new FileOutputStream(new File(filepath));
                                    image.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                } else {
                                    Log.e("GOTO", "load from disk: " + filepath);
                                }
                                FileInputStream fis = new FileInputStream(filepath);
                                return new WebResourceResponse("image/png", "utf-8", fis);
                            }

                            if (is_json || is_audio) {
                                File dir = new File(outputpath);
                                if (!dir.exists()) dir.mkdirs();

                                File file = new File(filepath);
                                if (!file.exists() || update_flag) {
                                    InputStream in = new URL(fullpath).openStream();
                                    byte[] buffer = new byte[8 * 1024];
                                    int bytes;
                                    FileOutputStream fos = new FileOutputStream(new File(filepath));
                                    while ((bytes = in.read(buffer)) != -1) {
                                        fos.write(buffer, 0, bytes);
                                    }
                                } else {
                                    Log.e("GOTO", "load from disk: " + filepath);
                                }
                                FileInputStream fis = new FileInputStream(filepath);
                                if (is_json) return new WebResourceResponse("application/json", "utf-8", fis);
                                if (is_audio) return new WebResourceResponse("audio/mpeg", "binary", fis);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

        });

        if (android.os.Build.VERSION.SDK_INT >= 21) {
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
        mSeekbar = mControllerView.findViewById(R.id.control_main);
        mSeekbar.setProgress(getProgressFromPref(adjust_padding));
        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isStartedFlag && fromUser) {
                    mContentView.evaluateJavascript(String.format(Locale.US, RESIZE_OSAPI_CALL, convertProgress(progress)), null);
                    sharedPref.edit().putInt(PREF_PADDING, convertProgress(progress)).apply();
                    ((TextView) mControllerView.findViewById(R.id.control_text))
                            .setText(String.valueOf(convertProgress(progress)));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        mControllerView.findViewById(R.id.control_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mControllerView.setVisibility(View.GONE);
            }
        });

        // mContentView.setInitialScale(1);
        mContentView.getSettings().setLoadWithOverviewMode(true);
        mContentView.getSettings().setSaveFormData(true);
        mContentView.getSettings().setDomStorageEnabled(true);
        mContentView.getSettings().setUseWideViewPort(true);
        mContentView.getSettings().setJavaScriptEnabled(true);
        mContentView.getSettings().setSupportZoom(false);
        // mContentView.getSettings().setBuiltInZoomControls(true);
        mContentView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0");
        mContentView.setScrollBarStyle (View.SCROLLBARS_OUTSIDE_OVERLAY);
        mContentView.setScrollbarFadingEnabled(false);
        mContentView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mContentView.getSettings().setAppCacheEnabled(false);

        WebView.setWebContentsDebuggingEnabled(true);
        mContentView.loadUrl(connector_url);
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
        boolean is_multi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode();
        if (!is_multi) mContentView.onPause();
        //setVolumeMute(true);
        super.onPause();
    }

    @Override
    protected void onResume() {
        boolean is_multi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode();
        if (!is_multi) mContentView.onResume();
        //setVolumeMute(false);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //setVolumeMute(false);
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
        if (mSeekbar != null) mSeekbar.setProgress(adjust_padding);

        if (isStartedFlag) {
            if (adjust_layout) mContentView.evaluateJavascript(String.format(Locale.US, RESIZE_OSAPI, adjust_padding), null);
        }
    }
    private int getProgressFromPref(int value) {return value / 2; }
    private int convertProgress(int progress) { return progress * 2; }

    private int getDefaultPadding(int width, int height) {
        if (width < height) {
            int temp = width; width = height; height = temp;
        }
        int ratio_val = width  * 18 / height;
        return (ratio_val - 30) * 20;
    }

    public void setSoundMuteCookie(){
        CookieSyncManager syncManager = CookieSyncManager.createInstance(mContentView.getContext());
        syncManager.sync();

        CookieManager cookieManager = CookieManager.getInstance();
        for (String server: SERVER_LIST) {
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
}
