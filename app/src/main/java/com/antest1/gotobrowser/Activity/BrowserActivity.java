package com.antest1.gotobrowser.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.SslErrorHandler;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import com.antest1.gotobrowser.Browser.BrowserGestureListener;
import com.antest1.gotobrowser.Browser.BrowserScaleGestureListener;
import com.antest1.gotobrowser.Browser.CustomDrawerLayout;
import com.antest1.gotobrowser.Browser.WebViewL;
import com.antest1.gotobrowser.Browser.WebViewManager;
import com.antest1.gotobrowser.BuildConfig;
import com.antest1.gotobrowser.Helpers.BackPressCloseHandler;
import com.antest1.gotobrowser.Helpers.CritPatcher;
import com.antest1.gotobrowser.Helpers.FpsPatcher;
import com.antest1.gotobrowser.Helpers.KenPatcher;
import com.antest1.gotobrowser.Helpers.K3dPatcher;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Notification.ScreenshotNotification;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.SubtitleProviderUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.antest1.gotobrowser.Browser.WebViewManager.OPEN_KANCOLLE;
import static com.antest1.gotobrowser.Constants.ACTION_SHOWKEYBOARD;
import static com.antest1.gotobrowser.Constants.ACTION_SHOWPANEL;
import static com.antest1.gotobrowser.Constants.APP_UI_HELP_VER;
import static com.antest1.gotobrowser.Constants.DEFAULT_SUBTITLE_FONT_SIZE;
import static com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT;
import static com.antest1.gotobrowser.Constants.PREF_CAPTURE;
import static com.antest1.gotobrowser.Constants.PREF_DEVTOOLS_DEBUG;
import static com.antest1.gotobrowser.Constants.PREF_KEEPMODE;
import static com.antest1.gotobrowser.Constants.PREF_LANDSCAPE;
import static com.antest1.gotobrowser.Constants.PREF_LOCKMODE;
import static com.antest1.gotobrowser.Constants.PREF_MULTIWIN_MARGIN;
import static com.antest1.gotobrowser.Constants.PREF_MUTEMODE;
import static com.antest1.gotobrowser.Constants.PREF_DISABLE_REFRESH_DIALOG;
import static com.antest1.gotobrowser.Constants.PREF_PIP_MODE;
import static com.antest1.gotobrowser.Constants.PREF_SHOWCC;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_FONTSIZE;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.PREF_UI_HELP_CHECKED;
import static com.antest1.gotobrowser.Constants.REQUEST_NOTIFICATION_PERMISSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.getSslErrorCodeDescription;
import static com.antest1.gotobrowser.Helpers.KcUtils.getSslErrorCodeTitle;
import static com.antest1.gotobrowser.Helpers.KcUtils.getWebkitErrorCodeText;

public class BrowserActivity extends AppCompatActivity {
    public static final String FOREGROUND_ACTION = BuildConfig.APPLICATION_ID + ".foreground";

    private int uiOption;
    private SharedPreferences sharedPref;
    private WebViewManager manager;
    private WebViewL mContentView;
    private ScreenshotNotification screenshotNotification;
    private final K3dPatcher k3dPatcher = new K3dPatcher();
    private final KenPatcher kenPatcher = new KenPatcher();
    private final CritPatcher critPatcher = new CritPatcher();
    private final FpsPatcher fpsPatcher = new FpsPatcher();

    private boolean isKcBrowserMode = false;
    private boolean isStartedFlag = false;
    private boolean isAdjustChangedByUser = false;
    private List<String> connector_info;
    private boolean isMuteMode, isCaptureMode, isLockMode, isKeepMode, isCaptionMode, isNoRefreshPopupMode;
    private boolean isSubtitleLoaded = false;
    private TextView subtitleText;
    private ImageView kcCameraButton;
    ScheduledExecutorService executor;
    private final Handler clearSubHandler = new Handler();

    private boolean isInPictureInPictureMode = false;

    private BackPressCloseHandler backPressCloseHandler;


    @SuppressLint({"SetJavaScriptEnabled", "ApplySharedPref",
            "ClickableViewAccessibility", "SourceLockedOrientationActivity"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        k3dPatcher.prepare(this);
        kenPatcher.prepare(this);
        fpsPatcher.prepare(this);
        critPatcher.prepare(this);

        Log.e("GOTO", "enter");
        super.onCreate(savedInstanceState);
        uiOption = getWindow().getDecorView().getSystemUiVisibility();
        setContentView(R.layout.activity_fullscreen);

        manager = new WebViewManager(BrowserActivity.this);
        manager.setDataDirectorySuffix();
        Log.e("GOTO", "manager init");

        backPressCloseHandler = new BackPressCloseHandler(this, true);
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { handleBackPress(); }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);

        sharedPref = getSharedPreferences(
                getString(R.string.preference_key), Context.MODE_PRIVATE);

        screenshotNotification = new ScreenshotNotification(this);
        sendIsFrontChanged(true);

        try {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) actionBar.hide();

            Intent intent = getIntent();
            isKcBrowserMode = OPEN_KANCOLLE.equals(intent.getAction());

            mContentView = findViewById(R.id.main_browser);
            mContentView.addJavascriptInterface(k3dPatcher,"kantai3dInterface");
            manager.setHardwareAcceleratedFlag();

            // panel, keyboard settings
            initPanelVisibility(intent);
            initPanelKeyboardFromIntent(intent);

            boolean isLandscapeMode = sharedPref.getBoolean(PREF_LANDSCAPE, true);
            isMuteMode = sharedPref.getBoolean(PREF_MUTEMODE, false);
            isLockMode = sharedPref.getBoolean(PREF_LOCKMODE, false);
            isKeepMode = sharedPref.getBoolean(PREF_KEEPMODE, false);
            isCaptionMode = sharedPref.getBoolean(PREF_SHOWCC, false);
            isCaptureMode = checkStoragePermissionGrated() && sharedPref.getBoolean(PREF_CAPTURE, false);
            isNoRefreshPopupMode = sharedPref.getBoolean(PREF_DISABLE_REFRESH_DIALOG, false);

            executor = Executors.newScheduledThreadPool(1);

            if (isLandscapeMode) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            if (isKeepMode) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            kcCameraButton = findViewById(R.id.kc_camera);

            // Browser Panel Buttons
            View menuRefresh = findViewById(R.id.menu_refresh);
            menuRefresh.setOnClickListener(v -> showRefreshDialog());

            View menuLogout = findViewById(R.id.menu_logout);
            menuLogout.setOnClickListener(v -> showLogoutDialog());

            MaterialButton menuMute = findViewById(R.id.menu_mute);
            menuMute.setIconTintResource(isMuteMode ? R.color.colorAccent : R.color.lightGray);
            menuMute.setOnClickListener(this::setMuteMode);

            MaterialButton menuCamera = findViewById(R.id.menu_camera);
            menuCamera.setIconTintResource(isCaptureMode ? R.color.colorAccent : R.color.lightGray);
            menuCamera.setOnClickListener(this::setCaptureMode);
            setCaptureButton();

            MaterialButton menuLock = findViewById(R.id.menu_lock);
            menuLock.setIconTintResource(isLockMode ? R.color.colorAccent : R.color.lightGray);
            menuLock.setOnClickListener(this::setOrientationLockMode);

            MaterialButton menuBrightOn = findViewById(R.id.menu_brighton);
            menuBrightOn.setIconTintResource(isKeepMode ? R.color.colorAccent : R.color.lightGray);
            menuBrightOn.setOnClickListener(this::setBrightOnMode);

            MaterialButton menuCaption = findViewById(R.id.menu_cc);
            menuCaption.setIconTintResource(isCaptionMode ? R.color.colorAccent : R.color.lightGray);
            menuCaption.setOnClickListener(this::setCaptionMode);

            MaterialButton menuKantai3d = findViewById(R.id.menu_kantai3d);
            if (k3dPatcher.isPatcherEnabled()) {
                menuKantai3d.setOnClickListener(this::setKantai3dMode);
            } else {
                menuKantai3d.setVisibility(View.GONE);
            }

            View menuClose = findViewById(R.id.menu_close);
            menuClose.setOnClickListener(this::togglePanelVisibility);

            View uiHintLayout = findViewById(R.id.ui_hint_layout);
            String uiHintCheckedVer = sharedPref.getString(PREF_UI_HELP_CHECKED, "");
            if (APP_UI_HELP_VER.equals(uiHintCheckedVer)) {
                uiHintLayout.setVisibility(View.GONE);
            }

            View uiHintClose = findViewById(R.id.ui_hint_close);
            uiHintClose.setOnClickListener(this::setUiHintInvisible);

            subtitleText = findViewById(R.id.subtitle_view);
            int subtitleSize = sharedPref.getInt(PREF_SUBTITLE_FONTSIZE, DEFAULT_SUBTITLE_FONT_SIZE);
            setSubtitleTextView(this, subtitleText, subtitleSize);
            subtitleText.setVisibility(isKcBrowserMode && isCaptionMode ? View.VISIBLE : View.GONE);
            subtitleText.setOnClickListener(v -> clearSubHandler.postDelayed(clearSubtitle, 250));

            String subtitle_local = sharedPref.getString(PREF_SUBTITLE_LOCALE, "");
            if (!subtitle_local.isEmpty()) {
                isSubtitleLoaded = SubtitleProviderUtils.getSubtitleProvider(subtitle_local)
                        .loadQuoteData(getApplicationContext(), subtitle_local);
            }

            connector_info = WebViewManager.getDefaultPage(BrowserActivity.this, isKcBrowserMode);

            boolean useDevTools = sharedPref.getBoolean(PREF_DEVTOOLS_DEBUG, false);
            WebViewManager.setWebViewDebugging(useDevTools);

            if (connector_info != null && connector_info.size() == 2) {
                manager.setWebViewSettings(mContentView);
                WebViewManager.enableBrowserCookie(mContentView);
                manager.setWebViewClient(this, mContentView);
                manager.setPopupView(mContentView);
                manager.openPage(mContentView, connector_info, isKcBrowserMode);
            } else {
                showWebkitErrorDialog(-1, "invalid connector info", "");
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

        setupSmoothPipAnimation();
        setScaleGestureDetector(mContentView);
    }

    private void setupSmoothPipAnimation(){
        // For Android 12+, PIP behaviour is changed
        // PIP params need to be set before calling onUserLeaveHint
        // In order to support smoother animation
        // Listener is called right after the user exits PiP but before animating.
        boolean pipEnabled = sharedPref.getBoolean(PREF_PIP_MODE, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && supportsPiPMode() && pipEnabled) {
            mContentView.addOnLayoutChangeListener((v, left, top, right, bottom,
                                                    oldLeft, oldTop, oldRight, oldBottom) -> {
                if (left != oldLeft || right != oldRight || top != oldTop
                        || bottom != oldBottom) {
                    final Rect sourceRectHint = new Rect();
                    mContentView.getGlobalVisibleRect(sourceRectHint);
                    setPictureInPictureParams(
                            new PictureInPictureParams.Builder()
                                    .setSeamlessResizeEnabled(false)
                                    .setSourceRectHint(sourceRectHint)
                                    .setAutoEnterEnabled(true)
                                    .setAspectRatio(new Rational(1200, 720))
                                    .build());
                }
            });
        }
    }

    public void handleBackPress() {
        if (isKcBrowserMode) {
            // On back pressed, always show the button panel
            // It is in case new users don't know tapping background shows the panel
            // Or if the screen is exactly 15:9 so there is no background to tap on
            ((CustomDrawerLayout)findViewById(R.id.main_container)).openDrawer(GravityCompat.START);
            backPressCloseHandler.handleOnBackPressed();
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
        manager.runMuteScript(mContentView, true, true);
        sendIsFrontChanged(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("GOTO", "onPause");
        k3dPatcher.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("GOTO", "onResume");
        Log.e("GOTO", isAdjustChangedByUser + " " +
                isInPictureInPictureMode + " " + isMultiWindowMode());
        mContentView.resumeTimers();
        sendIsFrontChanged(true);
        if (isAdjustChangedByUser || isInPictureInPictureMode || isMultiWindowMode()) {
            mContentView.getSettings().setTextZoom(100);
        }
        manager.runMuteScript(mContentView, isMuteMode);

        k3dPatcher.resume();
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mContentView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mContentView.restoreState(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e("GOTO", isAdjustChangedByUser + " "
                + isInPictureInPictureMode + " " + isMultiWindowMode());

        if (isMultiWindowMode()) {
            // Close drawer
            ((CustomDrawerLayout) findViewById(R.id.main_container))
                    .closeDrawer(GravityCompat.START);
        }

        if (sharedPref.getBoolean(PREF_MULTIWIN_MARGIN, false)) {
            setMultiwindowMargin();
        }

        if (isAdjustChangedByUser || isInPictureInPictureMode || isMultiWindowMode()) {
            mContentView.getSettings().setTextZoom(100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            boolean result = grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;
            String message;
            if (result) {
                message = getString(R.string.granted_true_notification_permission);
            } else {
                message = getString(R.string.granted_false_notification_permission);
            }
            Snackbar.make(this.findViewById(R.id.main_container),
                    message, Snackbar.LENGTH_SHORT).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean isKcMode() { return isKcBrowserMode; }
    public boolean isMuteMode() { return isMuteMode; }
    public boolean isCaptionAvailable() { return isCaptionMode; }
    public boolean isSubtitleAvailable() { return isSubtitleLoaded; }
    public void setStartedFlag() { isStartedFlag = true; }

    private void setMuteMode(View v) {
        isMuteMode = !isMuteMode;
        if (manager != null && mContentView != null) {
            manager.runMuteScript(mContentView, isMuteMode);
        }
        if (isMuteMode) {
            ((MaterialButton) v).setIconTintResource(R.color.colorAccent);
            sharedPref.edit().putBoolean(PREF_MUTEMODE, true).apply();
        } else {
            ((MaterialButton) v).setIconTintResource(R.color.lightGray);
            sharedPref.edit().putBoolean(PREF_MUTEMODE, false).apply();
        }
    }

    private void setCaptureMode(View v) {
        if (!checkStoragePermissionGrated()) showStoragePermissionDialog();
        isCaptureMode = !isCaptureMode;
        if (isCaptureMode) {
            findViewById(R.id.kc_camera).setVisibility(View.VISIBLE);
            ((MaterialButton) findViewById(R.id.menu_camera)).setIconTintResource(R.color.colorAccent);
            sharedPref.edit().putBoolean(PREF_CAPTURE, true).apply();
        } else {
            findViewById(R.id.kc_camera).setVisibility(View.GONE);
            ((MaterialButton) findViewById(R.id.menu_camera)).setIconTintResource(R.color.lightGray);
            sharedPref.edit().putBoolean(PREF_CAPTURE, false).apply();
        }
    }

    private boolean checkStoragePermissionGrated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void showStoragePermissionDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(getString(R.string.app_name));
            alertDialogBuilder
                    .setCancelable(false)
                    .setMessage(getString(R.string.noti_screenshot_permission_message))
                    .setPositiveButton(R.string.action_ok,
                            (dialog, id) -> {
                                ActivityCompat.requestPermissions(this, new String[]{
                                        Manifest.permission.POST_NOTIFICATIONS
                                }, REQUEST_NOTIFICATION_PERMISSION);
                            })
                    .setNegativeButton(R.string.action_cancel,
                            (dialog, id) -> dialog.cancel());
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    public void showWebkitErrorDialog(int errorCode, String description, String failingUrl) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getWebkitErrorCodeText(errorCode));
        alertDialogBuilder
                .setCancelable(false)
                .setMessage((description + "\n\n" + failingUrl).trim())
                .setPositiveButton("Reload",
                        (dialog, id) -> refreshPageOrFinish())
                .setNegativeButton("Close",
                        (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void showSslErrorDialog(SslErrorHandler handler, SslError error) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getSslErrorCodeTitle(error.getPrimaryError()));
        alertDialogBuilder
                .setCancelable(false)
                .setMessage((getSslErrorCodeDescription(error.getPrimaryError())
                        + "\n\nurl: " + error.getUrl()).trim())
                .setPositiveButton("Close",
                        (dialog, id) -> handler.cancel())
                .setNegativeButton("Proceed",
                        (dialog, id) -> handler.proceed());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void setOrientationLockMode(View v) {
        isLockMode = !isLockMode;
        if (isLockMode) {
            ((MaterialButton) v).setIconTintResource(R.color.colorAccent);
            sharedPref.edit().putBoolean(PREF_LOCKMODE, true).apply();
        } else {
            ((MaterialButton) v).setIconTintResource(R.color.lightGray);
            sharedPref.edit().putBoolean(PREF_LOCKMODE, false).apply();
        }

        if (sharedPref.getBoolean(PREF_LANDSCAPE, false)) {
            if (isLockMode) {
                int rot = getWindowManager().getDefaultDisplay().getRotation();
                if (rot == Surface.ROTATION_270) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
            else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        } else {
            if (isLockMode) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }
    }

    private void setBrightOnMode(View v) {
        isKeepMode = !isKeepMode;
        if (isKeepMode) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            ((MaterialButton) v).setIconTintResource(R.color.colorAccent);
            sharedPref.edit().putBoolean(PREF_KEEPMODE, true).apply();
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            ((MaterialButton) v).setIconTintResource(R.color.lightGray);
            sharedPref.edit().putBoolean(PREF_KEEPMODE, false).apply();
        }
    }

    private void setCaptionMode(View v) {
        isCaptionMode = !isCaptionMode;
        if (isCaptionMode) {
            subtitleText.setVisibility(View.VISIBLE);
            ((MaterialButton) v).setIconTintResource(R.color.colorAccent);
            sharedPref.edit().putBoolean(PREF_SHOWCC, true).apply();
        } else {
            subtitleText.setVisibility(View.GONE);
            ((MaterialButton) v).setIconTintResource(R.color.lightGray);
            sharedPref.edit().putBoolean(PREF_SHOWCC, false).apply();
        }
    }

    private void initPanelKeyboardFromIntent(Intent intent) {
        if (intent != null) {
            String options = intent.getStringExtra("options");
            if (options != null && !options.contains(ACTION_SHOWKEYBOARD)) {
                mContentView.setFocusableInTouchMode(false);
                mContentView.setFocusable(false);
                mContentView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            }
        }
        setGestureDetector(findViewById(R.id.background_area));
    }

    private void initPanelVisibility(Intent intent) {
        if (intent != null) {
            String options = intent.getStringExtra("options");
            if (options != null && options.contains(ACTION_SHOWPANEL)) {
                togglePanelVisibility(null);
            }
        }
    }

    private void setUiHintInvisible(View v) {
        findViewById(R.id.ui_hint_layout).setVisibility(View.GONE);
        sharedPref.edit().putString(PREF_UI_HELP_CHECKED, APP_UI_HELP_VER).apply();
    }

    private void togglePanelVisibility(View v) {
        CustomDrawerLayout drawerLayout = findViewById(R.id.main_container);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void onUserPinchIn(View v) {
        // User pinch in to enter pip mode
        // Same logic as UserLeaveHint (e.g. pressing home button)
        onUserLeaveHint();
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
                    Animation fadeout = getFadeoutAnimation(screenshotLight);
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

    @NonNull
    private static Animation getFadeoutAnimation(View screenshotLight) {
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
        return fadeout;
    }

    private void setKantai3dMode(View v) {
        k3dPatcher.showDialog();
    }

    private void refreshPageOrFinish() {
        connector_info = WebViewManager.getDefaultPage(BrowserActivity.this, isKcBrowserMode);
        if (manager != null && connector_info != null && connector_info.size() == 2) {
            ((TextView) findViewById(R.id.kc_error_text)).setText("");
            manager.refreshPage(mContentView);
        } else {
            finish();
        }
    }

    public void showRefreshDialog() {
        if (isNoRefreshPopupMode) {
            refreshPageOrFinish();
        } else {
            mContentView.pauseTimers();
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    BrowserActivity.this);
            alertDialogBuilder.setTitle(getString(R.string.app_name));
            alertDialogBuilder
                    .setCancelable(false)
                    .setMessage(getString(R.string.refresh_msg))
                    .setPositiveButton(R.string.action_ok,
                            (dialog, id) -> refreshPageOrFinish())
                    .setNegativeButton(R.string.action_cancel,
                            (dialog, id) -> {
                                dialog.cancel();
                                mContentView.resumeTimers();
                            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
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

    public void setMultiwindowMargin() {
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) mContentView.getLayoutParams();
        if (isMultiWindowMode() && !isInPictureInPictureMode) {
            Rect windowRect = new Rect();
            Rect screenRect = new Rect();
            View decorView = getWindow().getDecorView();
            decorView.getWindowVisibleDisplayFrame(windowRect);
            decorView.getGlobalVisibleRect(screenRect);

            // In split screen mode, the window and screen have 1-2 edges aligned
            if (windowRect.top != screenRect.top && windowRect.bottom != screenRect.bottom &&
                    windowRect.left != screenRect.left && windowRect.right != screenRect.right) {
                // it is in free-form mode and should not add black bar
                params.setMargins(0, 0, 0, 0);
            }  else {
                int center = (screenRect.top + screenRect.bottom) / 2;
                if (windowRect.top > center) params.setMargins(0, 24, 0, 0);
                else if (windowRect.bottom < center) params.setMargins(0, 0, 0, 24);
                else params.setMargins(0, 0, 0, 0);
            }
        } else {
            params.setMargins(0, 0, 0, 0);
        }
        mContentView.requestLayout();
    }

    public void showScreenshotNotification(Bitmap bitmap, Uri uri) {
        screenshotNotification.showNotification(bitmap, uri);
    }

    private final Runnable clearSubtitle = new Runnable() {
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
    public void onPictureInPictureModeChanged(boolean newMode, @NonNull Configuration newConfig) {
        super.onPictureInPictureModeChanged(newMode, newConfig);
        isInPictureInPictureMode = newMode;
        if (!isStartedFlag) {
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
            // Update margin again to undo multi-window black bar
            setMultiwindowMargin();
        } else {
            mContentView.setZ(0);
        }
    }

    private boolean supportsPiPMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static void setSubtitleTextView(Context context, TextView tv, int size) {
        int colorBlack = ContextCompat.getColor(context, R.color.black);
        tv.setTextSize(size);
        if (size >= 24) {
            tv.setShadowLayer(3, 3, 3, colorBlack);
        } else {
            tv.setShadowLayer(2, 2, 2, colorBlack);
        }
    }

    public void sendIsFrontChanged (boolean is_front) {
        Intent intent = new Intent(FOREGROUND_ACTION);
        intent.putExtra("is_front", is_front);
        sendBroadcast(intent);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setGestureDetector(View view) {
        GestureDetector mDetector = new GestureDetector(this,
                new BrowserGestureListener(this, this::togglePanelVisibility));
        view.setOnTouchListener((v, event) -> {
            mDetector.onTouchEvent(event);
            return event.getAction() != MotionEvent.ACTION_UP;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setScaleGestureDetector(View view) {
        ScaleGestureDetector mDetector = new ScaleGestureDetector(this,
                new BrowserScaleGestureListener(this, this::onUserPinchIn));
        view.setOnTouchListener((v, event) -> {
            mDetector.onTouchEvent(event);
            return false;
        });
    }
}