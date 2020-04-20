package com.antest1.gotobrowser.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.antest1.gotobrowser.Browser.WebViewL;
import com.antest1.gotobrowser.Browser.WebViewManager;
import com.antest1.gotobrowser.BuildConfig;
import com.antest1.gotobrowser.Helpers.BackPressCloseHandler;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Notification.ScreenshotNotification;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.KcSubtitleUtils;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.antest1.gotobrowser.Browser.WebViewManager.OPEN_KANCOLLE;
import static com.antest1.gotobrowser.Constants.ACTION_SHOWKEYBOARD;
import static com.antest1.gotobrowser.Constants.ACTION_SHOWPANEL;
import static com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT;
import static com.antest1.gotobrowser.Constants.PREF_CAPTURE;
import static com.antest1.gotobrowser.Constants.PREF_KEEPMODE;
import static com.antest1.gotobrowser.Constants.PREF_LANDSCAPE;
import static com.antest1.gotobrowser.Constants.PREF_LOCKMODE;
import static com.antest1.gotobrowser.Constants.PREF_MULTIWIN_MARGIN;
import static com.antest1.gotobrowser.Constants.PREF_MUTEMODE;
import static com.antest1.gotobrowser.Constants.PREF_PIP_MODE;
import static com.antest1.gotobrowser.Constants.PREF_SHOWCC;
import static com.antest1.gotobrowser.Constants.PREF_SILENT;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.REQUEST_EXTERNAL_PERMISSION;

public class BrowserActivity extends AppCompatActivity {
    public static final String FOREGROUND_ACTION = BuildConfig.APPLICATION_ID + ".foreground";

    private int uiOption;
    private SharedPreferences sharedPref;
    private WebViewManager manager;
    private WebViewL mContentView;
    private ProgressDialog downloadDialog;
    private ScreenshotNotification screenshotNotification;
    GestureDetector mDetector;

    private boolean isKcBrowserMode = false;
    private boolean isPanelVisible = false;
    private boolean isStartedFlag = false;
    private boolean isAdjustChangedByUser = false;
    private List<String> connector_info;
    private boolean pause_flag = false;
    private boolean isMuteMode, isCaptureMode, isLockMode, isKeepMode, isCaptionMode;
    private boolean isSubtitleLoaded = false;
    private TextView subtitleText;
    private ImageView kcCameraButton;
    ScheduledExecutorService executor;
    private final Handler clearSubHandler = new Handler();

    private boolean isInPictureInPictureMode = false;

    private BackPressCloseHandler backPressCloseHandler;

    @SuppressLint({"SetJavaScriptEnabled", "ApplySharedPref", "ClickableViewAccessibility", "SourceLockedOrientationActivity"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("GOTO", "enter");
        super.onCreate(savedInstanceState);
        uiOption = getWindow().getDecorView().getSystemUiVisibility();
        setContentView(R.layout.activity_fullscreen);

        manager = new WebViewManager(BrowserActivity.this);
        manager.setDataDirectorySuffix();
        Log.e("GOTO", "manager init");

        backPressCloseHandler = new BackPressCloseHandler(this);
        sharedPref = getSharedPreferences(
                getString(R.string.preference_key), Context.MODE_PRIVATE);

        screenshotNotification = new ScreenshotNotification(this);
        sendIsFrontChanged(true);

        try {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) actionBar.hide();

            mContentView = findViewById(R.id.main_browser);
            manager.setHardwardAcceleratedFlag();

            // panel, keyboard settings
            Intent intent = getIntent();

            View browserPanel = findViewById(R.id.menu_list);
            browserPanel.setVisibility(isMultiWindowMode() ? View.GONE : View.VISIBLE);
            if (intent != null) {
                String action = intent.getAction();
                isKcBrowserMode = OPEN_KANCOLLE.equals(action);
                String options = intent.getStringExtra("options");
                if (options != null) {
                    isPanelVisible = options.contains(ACTION_SHOWPANEL);
                    browserPanel.setVisibility(isPanelVisible ? View.VISIBLE : View.GONE);
                    setPanelVisible(findViewById(R.id.menu_close));
                    if (!options.contains(ACTION_SHOWKEYBOARD)) {
                        mContentView.setFocusableInTouchMode(false);
                        mContentView.setFocusable(false);
                        mContentView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                    }
                }
            }

            boolean isLandscapeMode = sharedPref.getBoolean(PREF_LANDSCAPE, false);
            boolean isSilentMode = sharedPref.getBoolean(PREF_SILENT, false);
            isMuteMode = sharedPref.getBoolean(PREF_MUTEMODE, false);
            isLockMode = sharedPref.getBoolean(PREF_LOCKMODE, false);
            isKeepMode = sharedPref.getBoolean(PREF_KEEPMODE, false);
            isCaptionMode = sharedPref.getBoolean(PREF_SHOWCC, false);
            isCaptureMode = checkStoragePermissionGrated() && sharedPref.getBoolean(PREF_CAPTURE, false);

            executor = Executors.newScheduledThreadPool(1);

            if (isLandscapeMode) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            if (isSilentMode) WebViewManager.setSoundMuteCookie(mContentView);
            if (isKeepMode) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            kcCameraButton = findViewById(R.id.kc_camera);
            downloadDialog = new ProgressDialog(BrowserActivity.this);

            // Browser Panel Buttons
            View menuRefresh = findViewById(R.id.menu_refresh);
            menuRefresh.setOnClickListener(v -> showRefreshDialog());

            View menuLogout = findViewById(R.id.menu_logout);
            menuLogout.setOnClickListener(v -> showLogoutDialog());

            View menuMute = findViewById(R.id.menu_mute);
            menuMute.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), isMuteMode ? R.color.panel_red : R.color.black));
            menuMute.setOnClickListener(this::setMuteMode);

            View menuCamera = findViewById(R.id.menu_camera);
            menuCamera.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), isCaptureMode ? R.color.panel_red : R.color.black));
            menuCamera.setOnClickListener(this::setCaptureMode);
            setCaptureButton();

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
            menuClose.setOnClickListener(this::setPanelVisible);

            subtitleText = findViewById(R.id.subtitle_view);
            subtitleText.setVisibility(isKcBrowserMode && isCaptionMode ? View.VISIBLE : View.GONE);
            subtitleText.setOnClickListener(v -> clearSubHandler.postDelayed(clearSubtitle, 250));

            // defaultSubtitleMargin = getDefaultSubtitleMargin();
            //setSubtitleMargin(sharedPref.getInt(PREF_PADDING, 0));
            String subtitle_local = sharedPref.getString(PREF_SUBTITLE_LOCALE, "en");
            KcSubtitleUtils.loadQuoteAnnotation(getApplicationContext());
            isSubtitleLoaded = KcSubtitleUtils.loadQuoteData(getApplicationContext(), subtitle_local);
            connector_info = WebViewManager.getDefaultPage(BrowserActivity.this, isKcBrowserMode);

            if (connector_info != null && connector_info.size() == 2) {
                WebViewManager.setWebViewSettings(mContentView);
                WebViewManager.enableBrowserCookie(mContentView);
                manager.setWebViewClient(this, mContentView, connector_info);
                manager.setWebViewDownloader(mContentView);
                manager.setPopupView(mContentView);
                manager.openPage(mContentView, connector_info, isKcBrowserMode);
            } else {
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
    protected void onStart() {
        super.onStart();
        if (sharedPref.getBoolean(PREF_MULTIWIN_MARGIN, false)) {
            setMultiwindowMargin();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        pause_flag = true;
        manager.runMuteScript(mContentView, true, true);
        sendIsFrontChanged(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("GOTO", "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("GOTO", "onResume");
        Log.e("GOTO", isAdjustChangedByUser + " " + isInPictureInPictureMode + " " + isMultiWindowMode());
        pause_flag = false;
        mContentView.resumeTimers();
        sendIsFrontChanged(true);
        if (isAdjustChangedByUser || isInPictureInPictureMode || isMultiWindowMode()) {
            mContentView.getSettings().setTextZoom(100);
        }
        manager.runMuteScript(mContentView, isMuteMode);
    }

    @Override
    protected void onDestroy() {
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
        Log.e("GOTO", isAdjustChangedByUser + " " + isInPictureInPictureMode + " " + isMultiWindowMode());
        View browserPanel = findViewById(R.id.browser_panel);
        browserPanel.setVisibility(isMultiWindowMode() ? View.GONE : View.VISIBLE);

        if (sharedPref.getBoolean(PREF_MULTIWIN_MARGIN, false)) {
            setMultiwindowMargin();
        }

        if (isAdjustChangedByUser || isInPictureInPictureMode || isMultiWindowMode()) {
            mContentView.getSettings().setTextZoom(100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_PERMISSION: {
                boolean result = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                String message;
                if (result) {
                    message = getString(R.string.granted_true_storage_permission);
                } else {
                    message = getString(R.string.granted_false_storage_permission);
                }
                Snackbar.make(this.findViewById(R.id.main_container), message, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    public ProgressDialog getDownloadDialog() { return downloadDialog; }
    public boolean isKcMode() { return isKcBrowserMode; }
    public boolean isMuteMode() { return isMuteMode; }
    public boolean isCaptionAvailable() { return isCaptionMode; }
    public boolean isSubtitleAvailable() { return isSubtitleLoaded; }
    public boolean isBrowserPaused() { return pause_flag; }
    public void setStartedFlag() { isStartedFlag = true; }

    private void setMuteMode(View v) {
        isMuteMode = !isMuteMode;
        if (manager != null && mContentView != null) {
            manager.runMuteScript(mContentView, isMuteMode);
        }
        if (isMuteMode) {
            v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.panel_red));
            sharedPref.edit().putBoolean(PREF_MUTEMODE, true).apply();
        } else {
            v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
            sharedPref.edit().putBoolean(PREF_MUTEMODE, false).apply();
        }
    }

    private void setCaptureMode(View v) {
        if (!checkStoragePermissionGrated()) {
            showStoragePermissionDialog();
        } else {
            isCaptureMode = !isCaptureMode;
            if (isCaptureMode) {
                findViewById(R.id.kc_camera).setVisibility(View.VISIBLE);
                v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.panel_red));
                sharedPref.edit().putBoolean(PREF_CAPTURE, true).apply();
            } else {
                findViewById(R.id.kc_camera).setVisibility(View.GONE);
                v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
                sharedPref.edit().putBoolean(PREF_CAPTURE, false).apply();
            }
        }
    }

    private boolean checkStoragePermissionGrated() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void showStoragePermissionDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.app_name));
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(getString(R.string.noti_screenshot_permission_message))
                .setPositiveButton(R.string.action_ok,
                        (dialog, id) -> {
                            ActivityCompat.requestPermissions(this, new String[]{
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                            }, REQUEST_EXTERNAL_PERMISSION);
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @SuppressLint("SourceLockedOrientationActivity")
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

    private void setPanelVisible(View v) {
        isPanelVisible = !isPanelVisible;
        View browserPanel = findViewById(R.id.menu_list);
        browserPanel.setVisibility(isPanelVisible ? View.VISIBLE : View.GONE);
        ((ImageView) v).setImageResource(isPanelVisible ? R.mipmap.close : R.mipmap.menu);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setCaptureButton() {
        kcCameraButton.setVisibility(isCaptureMode ? View.VISIBLE : View.GONE);
        kcCameraButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setAlpha(0.9f);
                    manager.captureGameScreen(mContentView);
                    View screenshotLight = findViewById(R.id.screenshot_light);
                    screenshotLight.setVisibility(View.VISIBLE);
                    Animation fadeout = new AlphaAnimation(0.75f, 0.f);
                    fadeout.setDuration(250);
                    fadeout.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) { }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            screenshotLight.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) { }
                    });
                    screenshotLight.startAnimation(fadeout);
                    break;
                case MotionEvent.ACTION_UP:
                    v.setAlpha(0.75f);
                    break;
                default:
                    break;
            }
            return true;
        });
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
                            connector_info = WebViewManager.getDefaultPage(BrowserActivity.this, isKcBrowserMode);
                            if (manager != null && connector_info != null && connector_info.size() == 2) {
                                ((TextView) findViewById(R.id.kc_error_text)).setText("");
                                manager.openPage(mContentView, connector_info, isKcBrowserMode);
                            } else {
                                finish();
                            }
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, id) -> {
                            dialog.cancel();
                            mContentView.resumeTimers();
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void showLogoutDialog() {
        mContentView.pauseTimers();
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                BrowserActivity.this);
        alertDialogBuilder.setTitle(getString(R.string.app_name));
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(getString(R.string.logout_msg))
                .setPositiveButton(R.string.action_ok,
                        (dialog, id) -> {
                            if (manager != null) {
                                ((TextView) findViewById(R.id.kc_error_text)).setText("");
                                manager.logoutGame(mContentView);
                            } else {
                                finish();
                            }
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, id) -> {
                            dialog.cancel();
                            mContentView.resumeTimers();
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /*
    public void setSubtitleMargin(int value) {
        FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) subtitleText.getLayoutParams();
        param.setMargins(param.leftMargin + value, param.topMargin, param.rightMargin + value, param.bottomMargin);
        subtitleText.setLayoutParams(param);
    }*/

    public void setMultiwindowMargin() {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mContentView.getLayoutParams();
        if (isMultiWindowMode()) {
            Rect windowRect = new Rect();
            Rect screenRect = new Rect();
            View decorView = getWindow().getDecorView();
            decorView.getWindowVisibleDisplayFrame(windowRect);
            decorView.getGlobalVisibleRect(screenRect);
            Log.e("GOTO", "windowRect: " + windowRect.toString());
            Log.e("GOTO", "screenRect: " + screenRect.toString());

            int center = (screenRect.top + screenRect.bottom) / 2;
            if (windowRect.top > center) params.setMargins(0, 24, 0, 0);
            else if (windowRect.bottom < center) params.setMargins(0, 0, 0, 24);
            else params.setMargins(0, 0, 0, 0);
        } else {
            params.setMargins(0, 0, 0, 0);
        }
        mContentView.requestLayout();
    }

    public void showScreenshotNotification(Bitmap bitmap, Uri uri) {
        screenshotNotification.showNotification(bitmap, uri);
    }

    private Runnable clearSubtitle = new Runnable() {
        @Override
        public void run() {
            subtitleText.setText("");
        }
    };

    public boolean isMultiWindowMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode();
    }

    @Override
    public void onUserLeaveHint() {
        boolean pipEnabled = sharedPref.getBoolean(PREF_PIP_MODE, false);
        if (supportsPiPMode() && pipEnabled) {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(1200, 720)).build();
            enterPictureInPictureMode(params);
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean newMode, Configuration newConfig) {
        isInPictureInPictureMode = newMode;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || !isStartedFlag) {
            return;
        }
        boolean adjust_layout = sharedPref.getBoolean(PREF_ADJUSTMENT, false);
        if (isInPictureInPictureMode) {
            // Bring the webview to the top while in picture-in-picture mode
            // i.e. blocking all other controls
            mContentView.setZ(100);
            if (adjust_layout) {
                isAdjustChangedByUser = true;
            }
        } else {
            mContentView.setZ(0);
        }
    }
    public boolean supportsPiPMode () {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public void sendIsFrontChanged (boolean is_front) {
        Intent intent = new Intent(FOREGROUND_ACTION);
        intent.putExtra("is_front", is_front);
        sendBroadcast(intent);
    }
}