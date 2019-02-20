package com.antest1.gotobrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import static com.antest1.gotobrowser.Constants.ACTION_WITHLC;
import static com.antest1.gotobrowser.Constants.CONNECT_NITRABBIT;
import static com.antest1.gotobrowser.Constants.CONN_NITRABBIT;
import static com.antest1.gotobrowser.Constants.CONN_OOI;
import static com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT;
import static com.antest1.gotobrowser.Constants.PREF_CONNECTOR;
import static com.antest1.gotobrowser.Constants.PREF_LANDSCAPE;
import static com.antest1.gotobrowser.Constants.PREF_PADDING;
import static com.antest1.gotobrowser.Constants.PREF_SILENT;
import static com.antest1.gotobrowser.Constants.RESIZE_OSAPI;
import static com.antest1.gotobrowser.Constants.RESIZE_OSAPI_CALL;
import static com.antest1.gotobrowser.Constants.SERVER_LIST;
import static com.antest1.gotobrowser.Constants.URL_NITRABBIT;
import static com.antest1.gotobrowser.Constants.URL_OOI;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    private final Handler mHideHandler = new Handler();
    private WebView mContentView;
    private View mControllerView;
    private SeekBar mSeekbar;
    private boolean isControllerActive = false;
    private boolean isStartedFlag = false;
    private String connector_url = "";

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
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
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
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
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

        String pref_connector = sharedPref.getString(PREF_CONNECTOR, null);
        if (CONN_OOI.equals(pref_connector)) {
            connector_url = URL_OOI;
        } else if (CONN_NITRABBIT.equals(pref_connector)) {
            connector_url = URL_NITRABBIT;
        } else {
            finish();
        }

        mVisible = true;
        mContentView = findViewById(R.id.main_browser);
        mControllerView = findViewById(R.id.control_component);
        mControllerView.setVisibility(View.GONE);
        isControllerActive = intent != null && ACTION_WITHLC.equals(intent.getAction());

        // window-level acceleration
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        // Set up the user interaction to manually show or hide the system UI.
        /*
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });*/
        backPressCloseHandler = new BackPressCloseHandler(this);

        mContentView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                //I save the cookies only when the user goes on login page
                if (url.equals(Constants.URL_NITRABBIT)) {
                    mContentView.evaluateJavascript(CONNECT_NITRABBIT, null);
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
                    if (isControllerActive) {
                        mControllerView.setVisibility(View.VISIBLE);
                        ((TextView) mControllerView.findViewById(R.id.control_text))
                                .setText(String.valueOf(adjust_padding));
                    }
                }
                if(url.contains("125.6")){ // temp code
                    CookieSyncManager syncManager = CookieSyncManager.createInstance(mContentView.getContext());
                    CookieManager cookieManager = CookieManager.getInstance();
                    String cookie = cookieManager.getCookie(url);
                    Log.e("GOTO", url + " " + cookie);
                    syncManager.sync();
                }
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
        mContentView.getSettings().setAppCacheEnabled(true);

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
    protected void onDestroy() {
        super.onDestroy();
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
            String url = "http://".concat(server);
            cookieManager.setCookie(url, "vol_bgm=0; expires=Tue Jan 19 2038 12:14:07 GMT+0900; path=/kcs2/");
            cookieManager.setCookie(url, "vol_se=0; expires=Tue Jan 19 2038 12:14:07 GMT+0900; path=/kcs2/");
            cookieManager.setCookie(url, "vol_voice=0; expires=Tue Jan 19 2038 12:14:07 GMT+0900; path=/kcs2/");
        }
    }


}
