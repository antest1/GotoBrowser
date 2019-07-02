package com.antest1.gotobrowser.Activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.antest1.gotobrowser.Browser.BrowserSoundPlayer;
import com.antest1.gotobrowser.Browser.WebViewL;
import com.antest1.gotobrowser.Browser.WebViewManager;
import com.antest1.gotobrowser.Helpers.BackPressCloseHandler;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.KcSubtitleUtils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import static com.antest1.gotobrowser.Browser.WebViewManager.OPEN_KANCOLLE;
import static com.antest1.gotobrowser.Constants.ACTION_SHOWKEYBOARD;
import static com.antest1.gotobrowser.Constants.ACTION_SHOWPANEL;
import static com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT;
import static com.antest1.gotobrowser.Constants.PREF_KEEPMODE;
import static com.antest1.gotobrowser.Constants.PREF_LANDSCAPE;
import static com.antest1.gotobrowser.Constants.PREF_LOCKMODE;
import static com.antest1.gotobrowser.Constants.PREF_MUTEMODE;
import static com.antest1.gotobrowser.Constants.PREF_PADDING;
import static com.antest1.gotobrowser.Constants.PREF_SHOWCC;
import static com.antest1.gotobrowser.Constants.PREF_SILENT;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.PREF_VPADDING;
import static com.antest1.gotobrowser.Constants.RESIZE_CALL;

public class BrowserActivity extends AppCompatActivity {
    private int uiOption;

    private SharedPreferences sharedPref;
    private WebViewL mContentView;
    private ProgressDialog downloadDialog;
    private View mHorizontalControlView, mVerticalControlView;
    private SeekBar mSeekBarH, mSeekBarV;
    private BrowserSoundPlayer browserPlayer;

    private boolean isKcBrowserMode = false;
    private boolean isStartedFlag = false;
    private List<String> connector_info;
    private boolean pause_flag = false;
    private boolean isMuteMode, isLockMode, isKeepMode, isCaptionMode;
    private boolean isSubtitleLoaded = false;
    private TextView subtitleText;
    ScheduledExecutorService executor;
    private final Handler clearSubHandler = new Handler();

    private BackPressCloseHandler backPressCloseHandler;

    @SuppressLint({"SetJavaScriptEnabled", "ApplySharedPref", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiOption = getWindow().getDecorView().getSystemUiVisibility();
        WebViewManager.setHardwardAcceleratedFlag(this);
        WebViewManager.setDataDirectorySuffix(this);
        try {
            setContentView(R.layout.activity_fullscreen);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) actionBar.hide();

            mContentView = findViewById(R.id.main_browser);
            WebViewManager.setGestureDetector(this, mContentView);

            // panel, keyboard settings
            Intent intent = getIntent();
            if (intent != null) {
                String action = intent.getAction();
                isKcBrowserMode = OPEN_KANCOLLE.equals(action);

                String options = intent.getStringExtra("options");
                if (options != null) {
                    View browserPanel = findViewById(R.id.browser_panel);
                    browserPanel.setVisibility(options.contains(ACTION_SHOWPANEL)?  View.VISIBLE : View.GONE);
                    if (!options.contains(ACTION_SHOWKEYBOARD)) {
                        mContentView.setFocusableInTouchMode(false);
                        mContentView.setFocusable(false);
                        mContentView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                    }
                }
            }

            backPressCloseHandler = new BackPressCloseHandler(this);
            sharedPref = getSharedPreferences(
                    getString(R.string.preference_key), Context.MODE_PRIVATE);

            boolean isLandscapeMode = sharedPref.getBoolean(PREF_LANDSCAPE, false);
            boolean isSilentMode = sharedPref.getBoolean(PREF_SILENT, false);
            isMuteMode = sharedPref.getBoolean(PREF_MUTEMODE, false);
            isLockMode = sharedPref.getBoolean(PREF_LOCKMODE, false);
            isKeepMode = sharedPref.getBoolean(PREF_KEEPMODE, false);
            isCaptionMode = sharedPref.getBoolean(PREF_SHOWCC, false);

            executor = Executors.newScheduledThreadPool(1);

            if (isLandscapeMode) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            if (isSilentMode) WebViewManager.setSoundMuteCookie(mContentView);
            if (isKeepMode) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            BrowserSoundPlayer.setmute(isMuteMode);

            mHorizontalControlView = findViewById(R.id.control_component);
            mHorizontalControlView.setVisibility(View.GONE);
            mVerticalControlView = findViewById(R.id.vcontrol_component);
            mVerticalControlView.setVisibility(View.GONE);

            downloadDialog = new ProgressDialog(BrowserActivity.this);

            // Browser Panel Buttons
            View menuRefresh = findViewById(R.id.menu_refresh);
            menuRefresh.setOnClickListener(v -> showRefreshDialog());

            View menuAspect = findViewById(R.id.menu_aspect);
            menuAspect.setOnClickListener(v -> showLayoutAspectControls());
            setLayoutAspectController();

            View menuMute = findViewById(R.id.menu_mute);
            menuMute.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), isMuteMode ? R.color.panel_red : R.color.black));
            menuMute.setOnClickListener(this::setMuteMode);

            View menuLock = findViewById(R.id.menu_lock);
            menuLock.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), isLockMode ? R.color.panel_red : R.color.black));
            menuLock.setOnClickListener(this::setOrientationLockMode);

            View menuBrightOn = findViewById(R.id.menu_brighton);
            menuBrightOn.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), isKeepMode ? R.color.panel_red : R.color.black));
            menuBrightOn.setOnClickListener(this::setBrightOnMode);

            View menuCaption = findViewById(R.id.menu_cc);
            menuCaption.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), isCaptionMode ? R.color.panel_red : R.color.black));
            menuCaption.setOnClickListener(this::setCaptionMode);

            View menuClose = findViewById(R.id.menu_close);
            menuClose.setOnClickListener(v -> { findViewById(R.id.browser_panel).setVisibility(View.GONE); });

            subtitleText = findViewById(R.id.subtitle_view);
            subtitleText.setVisibility(isKcBrowserMode && isCaptionMode ? View.VISIBLE : View.GONE);
            subtitleText.setOnClickListener(v -> clearSubHandler.postDelayed(clearSubtitle, 250));

            // defaultSubtitleMargin = getDefaultSubtitleMargin();
            setSubtitleMargin(sharedPref.getInt(PREF_PADDING, 0));
            String subtitle_local = sharedPref.getString(PREF_SUBTITLE_LOCALE, "en");
            KcSubtitleUtils.loadQuoteAnnotation(getApplicationContext());
            isSubtitleLoaded = KcSubtitleUtils.loadQuoteData(getApplicationContext(), subtitle_local);
            connector_info = WebViewManager.getDefaultPage(BrowserActivity.this, isKcBrowserMode);

            if (connector_info != null && connector_info.size() == 2) {
                WebViewManager.setWebViewSettings(mContentView);
                WebViewManager.enableBrowserCookie(mContentView);
                WebViewManager.setWebViewClient(this, mContentView, connector_info);
                WebViewManager.setWebViewDownloader(this, mContentView);
                WebViewManager.setPopupView(this, mContentView);
                WebViewManager.openPage(BrowserActivity.this, mContentView, connector_info, isKcBrowserMode);
            } else {
                finish();
            }
        } catch (Exception e) {
            String exception_str = KcUtils.getStringFromException(e);
            setContentView(R.layout.activity_empty);
            TextView tv = findViewById(R.id.error_text);
            tv.setText(exception_str);
            KcUtils.reportException(e);
            if (exception_str.toLowerCase().contains("no webview")) {
                KcUtils.showToast(getApplicationContext(), "WebView not installed");
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isKcBrowserMode) {
            backPressCloseHandler.onBackPressed();
        } else {
            Intent intent = new Intent(BrowserActivity.this, EntranceActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                // | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        uiOption = mContentView.getSystemUiVisibility();
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
            if (browserPlayer != null) browserPlayer.pauseAll();
        } else {
            if (pause_flag) {
                mContentView.resumeTimers();
                pause_flag = false;
                if (browserPlayer != null) browserPlayer.startAll();
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
        mContentView.getSettings().setTextZoom(100);
        if (browserPlayer != null) browserPlayer.startAll();
    }

    @Override
    protected void onDestroy() {
        if (browserPlayer != null) {
            browserPlayer.stopAll();
            browserPlayer.releaseAll();
        }
        mContentView.removeAllViews();
        mContentView.destroy();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) mContentView.setSystemUiVisibility(uiOption);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mContentView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mContentView.restoreState(savedInstanceState);
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
        int adjust_padding = sharedPref.getInt(PREF_PADDING, WebViewManager.getDefaultPadding(width, height));
        int adjust_vpadding = sharedPref.getInt(PREF_VPADDING, 0);
        if (mSeekBarH != null) mSeekBarH.setProgress(adjust_padding);
        setSubtitleMargin(adjust_padding);
        if (isStartedFlag) {
            if (adjust_layout) mContentView.evaluateJavascript(
                    String.format(Locale.US, RESIZE_CALL, adjust_padding, adjust_vpadding), null);
        }
    }

    public ProgressDialog getDownloadDialog() { return downloadDialog; }
    public boolean isKcMode() { return isKcBrowserMode; }
    public boolean isCaptionAvailable() { return isCaptionMode; }
    public boolean isSubtitleAvailable() { return isSubtitleLoaded; }
    public boolean isBrowserPaused() { return pause_flag; }
    public void setStartedFlag() { isStartedFlag = true; }
    public void setBrowserPlayer(BrowserSoundPlayer player) { browserPlayer = player; }

    private int getHorizontalProgressFromPref(int value) { return value / 2; }
    private int convertHorizontalProgress(int progress) { return progress * 2; }
    private int getVerticalProgressFromPref(int value) {return value / 2; }
    private int convertVerticalProgress(int progress) { return progress * 2; }

    private void showLayoutAspectControls() {
        DisplayMetrics dimension = KcUtils.getActivityDimension(this);
        int width = dimension.widthPixels;
        int height = dimension.heightPixels;
        int adjust_padding = sharedPref.getInt(PREF_PADDING, WebViewManager.getDefaultPadding(width, height));
        int adjust_vpadding = sharedPref.getInt(PREF_VPADDING, 0);
        mHorizontalControlView.setVisibility(View.VISIBLE);
        ((TextView) mHorizontalControlView.findViewById(R.id.control_text))
                .setText(String.valueOf(adjust_padding));
        mVerticalControlView.setVisibility(View.VISIBLE);
        ((TextView) mVerticalControlView.findViewById(R.id.vcontrol_text))
                .setText(String.valueOf(adjust_vpadding));
    }

    private void setLayoutAspectController() {
        DisplayMetrics dimension = KcUtils.getActivityDimension(this);
        int width = dimension.widthPixels;
        int height = dimension.heightPixels;
        int adjust_padding = sharedPref.getInt(PREF_PADDING, WebViewManager.getDefaultPadding(width, height));
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


        mHorizontalControlView.findViewById(R.id.control_exit)
                .setOnClickListener(v -> mHorizontalControlView.setVisibility(View.GONE));

        mSeekBarV = mVerticalControlView.findViewById(R.id.vcontrol_main);
        mSeekBarV.setProgress(getVerticalProgressFromPref(adjust_vpadding));
        mSeekBarV.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isStartedFlag) {
                    int hprogress = sharedPref.getInt(PREF_PADDING, WebViewManager.getDefaultPadding(width, height));
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
    }

    private void setMuteMode(View v) {
        isMuteMode = !isMuteMode;
        if (browserPlayer != null) browserPlayer.setMuteAll(isMuteMode);
        if (isMuteMode) {
            v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.panel_red));
            sharedPref.edit().putBoolean(PREF_MUTEMODE, true).apply();
        } else {
            v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
            sharedPref.edit().putBoolean(PREF_MUTEMODE, false).apply();
        }
    }

    private void setOrientationLockMode(View v) {
        isLockMode = !isLockMode;
        if (isLockMode) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.panel_red));
            sharedPref.edit().putBoolean(PREF_LOCKMODE, true).apply();
        } else {
            if (sharedPref.getBoolean(PREF_LANDSCAPE, false)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            }
            v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
            sharedPref.edit().putBoolean(PREF_LOCKMODE, false).apply();
        }
    }

    private void setBrightOnMode(View v) {
        isKeepMode = !isKeepMode;
        if (isKeepMode) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.panel_red));
            sharedPref.edit().putBoolean(PREF_KEEPMODE, true).apply();
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
            sharedPref.edit().putBoolean(PREF_KEEPMODE, false).apply();
        }
    }

    private void setCaptionMode(View v) {
        isCaptionMode = !isCaptionMode;
        if (isCaptionMode) {
            subtitleText.setVisibility(View.VISIBLE);
            v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.panel_red));
            sharedPref.edit().putBoolean(PREF_SHOWCC, true).apply();
        } else {
            subtitleText.setVisibility(View.GONE);
            v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
            sharedPref.edit().putBoolean(PREF_SHOWCC, false).apply();
        }
    }

    public void showRefreshDialog() {
        mContentView.pauseTimers();
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                BrowserActivity.this);
        alertDialogBuilder.setTitle(getString(R.string.app_name));
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(getString(R.string.refresh_msg))
                .setPositiveButton(R.string.action_ok,
                        (dialog, id) -> {
                            browserPlayer.pauseAll();
                            connector_info = WebViewManager.getDefaultPage(BrowserActivity.this, isKcBrowserMode);
                            if (connector_info != null && connector_info.size() == 2) {
                                WebViewManager.openPage(BrowserActivity.this, mContentView, connector_info, isKcBrowserMode);
                            } else {
                                finish();
                            }
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, id) -> {
                            dialog.cancel();
                            browserPlayer.startAll();
                            mContentView.resumeTimers();
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void setSubtitleMargin(int value) {
        FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) subtitleText.getLayoutParams();
        param.setMargins(param.leftMargin + value, param.topMargin, param.rightMargin + value, param.bottomMargin);
        subtitleText.setLayoutParams(param);
    }

    private Runnable clearSubtitle = new Runnable() {
        @Override
        public void run() {
            subtitleText.setText("");
        }
    };
}
