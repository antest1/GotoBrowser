package com.antest1.gotobrowser.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Surface
import android.view.WindowManager
import android.webkit.SslErrorHandler
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.antest1.gotobrowser.Browser.WebViewL
import com.antest1.gotobrowser.Browser.WebViewManager
import com.antest1.gotobrowser.BuildConfig
import com.antest1.gotobrowser.Constants.CONN_DMM
import com.antest1.gotobrowser.Constants.DEFAULT_ALTER_GADGET_URL
import com.antest1.gotobrowser.Constants.DEFAULT_SUBTITLE_FONT_SIZE
import com.antest1.gotobrowser.Constants.PREF_ALTER_ENDPOINT
import com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET
import com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD
import com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD_PROXY
import com.antest1.gotobrowser.Constants.PREF_BROADCAST
import com.antest1.gotobrowser.Constants.PREF_CONNECTOR
import com.antest1.gotobrowser.Constants.PREF_DEVTOOLS_DEBUG
import com.antest1.gotobrowser.Constants.PREF_DISABLE_REFRESH_DIALOG
import com.antest1.gotobrowser.Constants.PREF_DOWNLOAD_RETRY
import com.antest1.gotobrowser.Constants.PREF_KEYBOARD
import com.antest1.gotobrowser.Constants.PREF_LANDSCAPE
import com.antest1.gotobrowser.Constants.PREF_MULTIWIN_MARGIN
import com.antest1.gotobrowser.Constants.PREF_PANELSTART
import com.antest1.gotobrowser.Constants.PREF_PIP_MODE
import com.antest1.gotobrowser.Constants.PREF_SUBTITLE_FONTSIZE
import com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE
import com.antest1.gotobrowser.Constants.REQUEST_NOTIFICATION_PERMISSION
import com.antest1.gotobrowser.Helpers.BackPressCloseHandler
import com.antest1.gotobrowser.Helpers.KcUtils
import com.antest1.gotobrowser.Notification.ScreenshotNotification
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.Subtitle.SubtitleProviderUtils
import com.antest1.gotobrowser.ui.component.ControlLayout
import com.antest1.gotobrowser.ui.component.RefreshOption
import com.antest1.gotobrowser.ui.component.SettingsBottomSheet
import com.antest1.gotobrowser.ui.component.glassyStyle
import com.antest1.gotobrowser.ui.theme.GotobrowserTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class BrowserActivity : ComponentActivity() {
    companion object {
        const val FOREGROUND_ACTION = "${BuildConfig.APPLICATION_ID}.foreground"
        const val MULTIWIN_MARGIN_DP = 24
    }

    lateinit var viewModel: BrowserViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private var manager: WebViewManager? = null
    var mContentView: WebViewL? = null
    private lateinit var screenshotNotification: ScreenshotNotification
    private lateinit var backPressCloseHandler: BackPressCloseHandler

    internal lateinit var pipController: PipController
    private lateinit var displayController: DisplayController

    val isInPictureInPictureModeState = mutableStateOf(false)
    private val errorText = mutableStateOf("")
    private val subtitleTextValue = mutableStateOf("")
    private val closeButtonVisible = mutableStateOf(false)
    private val toolbarVisible = mutableStateOf(false)
    private val settingsSheetVisible = mutableStateOf(false)
    private val refreshPromptVisible = mutableStateOf(false)
    private val refreshOption = mutableStateOf(RefreshOption.QUICK)
    private val subtitleFontSize = mutableStateOf(DEFAULT_SUBTITLE_FONT_SIZE)
    private val multiwinMarginDp = mutableStateOf(0)

    private val liveSettings = setOf(
        PREF_LANDSCAPE,
        PREF_MULTIWIN_MARGIN,
        PREF_SUBTITLE_FONTSIZE,
        PREF_DOWNLOAD_RETRY,
        PREF_PIP_MODE,
        PREF_PANELSTART,
        PREF_DISABLE_REFRESH_DIALOG,
        PREF_DEVTOOLS_DEBUG
    )

    @SuppressLint("SourceLockedOrientationActivity", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        viewModel = ViewModelProvider(this)[BrowserViewModel::class.java]
        WebViewManager.setWebViewDebugging(viewModel.sharedPref.getBoolean(PREF_DEVTOOLS_DEBUG, false))
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        screenshotNotification = ScreenshotNotification(this)
        backPressCloseHandler = BackPressCloseHandler(this, true)

        sendIsFrontChanged(true)

        val intent = getIntent()
        val isAppLaunch = Intent.ACTION_MAIN == intent.action && savedInstanceState == null

        if (viewModel.sharedPref.getBoolean(PREF_LANDSCAPE, true)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        }

        toolbarVisible.value = viewModel.sharedPref.getBoolean(PREF_PANELSTART, true)
        refreshOption.value = if (viewModel.isQuickGameReload) RefreshOption.QUICK else RefreshOption.FULL
        subtitleFontSize.value = settingsViewModel.getSubtitleFontSize()

        loadSubtitleData()

        manager = WebViewManager(this)
        manager?.setDataDirectorySuffix()

        pipController = PipController(this, mContentView, { isInPictureInPictureModeState.value = it }, { hideSystemBars() })
        displayController = DisplayController(this, mContentView) { multiwinMarginDp.value = it }

        val prefAlterGadget = viewModel.sharedPref.getBoolean(PREF_ALTER_GADGET, false)
        val isProxyMethod = PREF_ALTER_METHOD_PROXY == viewModel.sharedPref.getString(PREF_ALTER_METHOD, "")
        val alterEndpoint = viewModel.sharedPref.getString(PREF_ALTER_ENDPOINT, DEFAULT_ALTER_GADGET_URL)
        val prefConnector = viewModel.sharedPref.getString(PREF_CONNECTOR, CONN_DMM)
        if (prefAlterGadget && isProxyMethod && CONN_DMM == prefConnector) {
            WebViewManager.setKcCacheProxy(alterEndpoint, {}, {
                KcUtils.showToast(applicationContext, R.string.setting_alter_method_proxy_error_toast)
            })
        } else {
            WebViewManager.clearKcCacheProxy()
        }

        if (isAppLaunch) {
            window.decorView.post {
                checkAppUpdateOnStart()
                warnIfKcanotifyBroadcastDisabled()
            }
        }

        setContent {
            GotobrowserTheme {
                LaunchedEffect(settingsSheetVisible.value, refreshPromptVisible.value) {
                    hideSystemBars()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    BrowserScreenContent(
                        viewModel = viewModel,
                        errorText = errorText,
                        subtitleTextValue = subtitleTextValue,
                        closeButtonVisible = closeButtonVisible,
                        subtitleFontSize = subtitleFontSize,
                        multiwinMarginDp = multiwinMarginDp,
                        manager = manager,
                        onViewCreated = { 
                            mContentView = it 
                            pipController = PipController(this@BrowserActivity, it, { isInPictureInPictureModeState.value = it }, { hideSystemBars() })
                            displayController = DisplayController(this@BrowserActivity, it) { multiwinMarginDp.value = it }
                            pipController.setupSmoothPipAnimation()
                            displayController.applyKeyboardSetting()
                        },
                        intent = intent,
                        activity = this@BrowserActivity,
                        onBackgroundTap = { toolbarVisible.value = !toolbarVisible.value }
                    )

                    ControlLayout(
                        visible = toolbarVisible.value,
                        onVisibleChange = { toolbarVisible.value = it },
                        onRefreshClick = { showRefreshDialog(refreshOption.value) },
                        onQuickRefreshClick = { showRefreshDialog(RefreshOption.QUICK) },
                        onFullRefreshClick = { showRefreshDialog(RefreshOption.FULL) },
                        selectedRefreshOption = refreshOption.value,
                        onRefreshOptionChange = { option ->
                            refreshOption.value = option
                            viewModel.setQuickGameReload(option == RefreshOption.QUICK)
                        },
                        onSettingsClick = { settingsSheetVisible.value = true },
                        onHelpClick = { openManual(this@BrowserActivity) }
                    ) {
                        val isMute by viewModel.isMuteMode.observeAsState(false)
                        val isCapture by viewModel.isCaptureMode.observeAsState(false)
                        val isLock by viewModel.isLockMode.observeAsState(false)
                        val isKeep by viewModel.isKeepMode.observeAsState(false)
                        val isCaption by viewModel.isCaptionMode.observeAsState(false)

                        PanelButton(
                            id = R.drawable.volume_off,
                            tooltip = stringResource(R.string.menu_tooltip_mute),
                            active = isMute,
                            onClick = { toggleMuteMode() }
                        )
                        PanelButton(
                            id = R.drawable.camera_icon,
                            tooltip = stringResource(R.string.menu_tooltip_camera),
                            active = isCapture,
                            onClick = {
                                if (!checkStoragePermissionGrated()) showStoragePermissionDialog()
                                viewModel.toggleCaptureMode()
                            }
                        )
                        PanelButton(
                            id = R.drawable.screen_lock,
                            tooltip = stringResource(R.string.menu_tooltip_lock),
                            active = isLock,
                            onClick = { viewModel.toggleLockMode(); displayController.updateOrientationLock() }
                        )
                        PanelButton(
                            id = R.drawable.light_mode,
                            tooltip = stringResource(R.string.menu_tooltip_brighton),
                            active = isKeep,
                            onClick = { viewModel.toggleKeepMode() }
                        )
                        PanelButton(
                            id = R.drawable.caption_icon,
                            tooltip = stringResource(R.string.menu_tooltip_cc),
                            active = isCaption,
                            onClick = { viewModel.toggleCaptionMode() }
                        )
                    }

                    if (settingsSheetVisible.value) {
                        SettingsBottomSheet(
                            viewModel = settingsViewModel,
                            activity = this@BrowserActivity,
                            onDismissRequest = { settingsSheetVisible.value = false },
                            onLogoutRequest = {
                                settingsSheetVisible.value = false
                                showLogoutDialog()
                            },
                            onSettingChanged = { key ->
                                if (key in liveSettings) {
                                    applyLiveSetting(key)
                                } else {
                                    refreshPromptVisible.value = true
                                }
                            }
                        )
                    }

                    if (refreshPromptVisible.value) {
                        SettingsRefreshDialog(
                            onRefreshNow = {
                                refreshPromptVisible.value = false
                                settingsSheetVisible.value = false
                                refreshPageOrFinish(RefreshOption.FULL)
                            },
                            onChangeWithoutRefresh = { refreshPromptVisible.value = false }
                        )
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })

        pipController.setupSmoothPipAnimation()
        hideSystemBars()
    }

    fun isMuteMode(): Boolean = java.lang.Boolean.TRUE == viewModel.isMuteMode.value
    fun isCaptionAvailable(): Boolean = java.lang.Boolean.TRUE == viewModel.isCaptionMode.value
    fun isSubtitleAvailable(): Boolean = viewModel.isSubtitleLoaded

    private fun toggleMuteMode() {
        viewModel.toggleMuteMode()
        mContentView?.let {
            manager?.runMuteScript(it, java.lang.Boolean.TRUE == viewModel.isMuteMode.value)
        }
    }

    private fun loadSubtitleData() {
        val subtitleLocale = viewModel.sharedPref.getString(PREF_SUBTITLE_LOCALE, "en") ?: "en"
        val loaded = SubtitleProviderUtils.getSubtitleProvider(subtitleLocale)
            .loadQuoteData(applicationContext, subtitleLocale)
        viewModel.setSubtitleLoaded(loaded)
    }

    fun setStartedFlag() {
        viewModel.isStartedFlag = true
    }

    fun setErrorText(text: String) { errorText.value = text }
    fun setSubtitleText(text: String) { subtitleTextValue.value = text }
    fun setCloseButtonVisible(visible: Boolean) { closeButtonVisible.value = visible }

    private fun applyLiveSetting(key: String) {
        when (key) {
            PREF_LANDSCAPE -> displayController.updateOrientationLock()
            PREF_MULTIWIN_MARGIN -> displayController.updateMultiwindowMargin()
            PREF_SUBTITLE_FONTSIZE -> subtitleFontSize.value = settingsViewModel.getSubtitleFontSize()
            PREF_PIP_MODE -> pipController.setupSmoothPipAnimation()
            PREF_KEYBOARD -> displayController.applyKeyboardSetting()
            PREF_DEVTOOLS_DEBUG -> WebViewManager.setWebViewDebugging(viewModel.sharedPref.getBoolean(PREF_DEVTOOLS_DEBUG, false))
            PREF_DISABLE_REFRESH_DIALOG -> viewModel.setNoRefreshPopupMode(viewModel.sharedPref.getBoolean(PREF_DISABLE_REFRESH_DIALOG, false))
        }
    }

    fun handleBackPress() {
        if (refreshPromptVisible.value) {
            refreshPromptVisible.value = false
            return
        }
        if (settingsSheetVisible.value) {
            settingsSheetVisible.value = false
            return
        }
        toolbarVisible.value = true
        backPressCloseHandler.handleOnBackPressed()
    }

    override fun onStop() {
        super.onStop()
        mContentView?.let { manager?.runMuteScript(it, true, true) }
        sendIsFrontChanged(false)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        displayController.updateMultiwindowMargin()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        displayController.updateMultiwindowMargin()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipController.handlePictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            toolbarVisible.value = false
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureModeState.value) {
            viewModel.k3dPatcher.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.setHostActivity(this)
        hideSystemBars()
        mContentView?.resumeTimers()
        sendIsFrontChanged(true)
        displayController.updateMultiwindowMargin()
        mContentView?.let { manager?.runMuteScript(it, java.lang.Boolean.TRUE == viewModel.isMuteMode.value) }
        val rot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }
        viewModel.k3dPatcher.setRotation(rot)
        viewModel.k3dPatcher.resume()
    }

    override fun onDestroy() {
        settingsViewModel.clearHostActivity(this)
        mContentView?.removeAllViews()
        mContentView?.destroy()
        super.onDestroy()
    }

    fun showWebkitErrorDialog(errorCode: Int, description: String, failingUrl: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(KcUtils.getWebkitErrorCodeText(errorCode))
            .setCancelable(false)
            .setMessage((description + "\n\n" + failingUrl).trim { it <= ' ' })
            .setPositiveButton("Reload") { _, _ -> refreshPageOrFinish(RefreshOption.FULL) }
            .setNegativeButton("Close") { dialog, _ -> dialog.cancel() }
            .show()
    }

    fun showSslErrorDialog(handler: SslErrorHandler, error: SslError) {
        MaterialAlertDialogBuilder(this)
            .setTitle(KcUtils.getSslErrorCodeTitle(error.primaryError))
            .setCancelable(false)
            .setMessage((KcUtils.getSslErrorCodeDescription(error.primaryError) + "\n\nurl: " + error.url).trim { it <= ' ' })
            .setPositiveButton("Close") { _, _ -> handler.cancel() }
            .setNegativeButton("Proceed") { _, _ -> handler.proceed() }
            .show()
    }

    private fun refreshPageOrFinish(option: RefreshOption = refreshOption.value) {
        viewModel.connectorInfo = WebViewManager.getDefaultPage(this)
        val info = viewModel.connectorInfo
        if (manager != null && info != null && info.size == 2) {
            mContentView?.let {
                when (option) {
                    RefreshOption.QUICK -> manager?.refreshGameFrame(it)
                    RefreshOption.FULL -> manager?.refreshPage(it)
                }
            }
        } else {
            finish()
        }
    }

    fun showRefreshDialog(option: RefreshOption = refreshOption.value) {
        if (java.lang.Boolean.TRUE == viewModel.isNoRefreshPopupMode.value) {
            refreshPageOrFinish(option)
        } else {
            val (titleRes, messageRes) = when (option) {
                RefreshOption.QUICK -> R.string.menu_quick_refresh to R.string.refresh_quick_msg
                RefreshOption.FULL -> R.string.menu_full_refresh to R.string.refresh_full_msg
            }
            mContentView?.pauseTimers()
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(titleRes))
                .setCancelable(false)
                .setMessage(getString(messageRes))
                .setPositiveButton(R.string.action_ok) { _, _ -> refreshPageOrFinish(option) }
                .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                    dialog.cancel()
                    mContentView?.resumeTimers()
                }
                .show()
        }
    }

    fun showLogoutDialog() {
        mContentView?.pauseTimers()
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.app_name))
            .setCancelable(false)
            .setMessage(getString(R.string.logout_msg))
            .setPositiveButton(R.string.action_ok) { _, _ ->
                mContentView?.let { manager?.logoutGame(it) }
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.cancel()
                mContentView?.resumeTimers()
            }
            .show()
    }

    fun showScreenshotNotification(bitmap: Bitmap, uri: Uri) {
        screenshotNotification.showNotification(bitmap, uri)
    }

    private fun checkAppUpdateOnStart() {
        if (viewModel.isAppUpdateCheckStarted) return
        viewModel.isAppUpdateCheckStarted = true
        settingsViewModel.checkAppUpdate(this)
    }

    private fun warnIfKcanotifyBroadcastDisabled() {
        val broadcastEnabled = viewModel.sharedPref.getBoolean(PREF_BROADCAST, true)
        if (broadcastEnabled || !KcUtils.isKcanotifyInstalled(this)) return

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.kcanotify_broadcast_dialog_title))
            .setCancelable(false)
            .setMessage(
                String.format(
                    Locale.US,
                    getString(R.string.kcanotify_broadcast_dialog_message),
                    getString(R.string.mode_broadcast),
                    getString(R.string.action_ok)
                )
            )
            .setPositiveButton(R.string.action_ok) { dialog, _ ->
                viewModel.sharedPref.edit().putBoolean(PREF_BROADCAST, true).apply()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun checkStoragePermissionGrated(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showStoragePermissionDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.app_name))
                .setCancelable(false)
                .setMessage(getString(R.string.noti_screenshot_permission_message))
                .setPositiveButton(R.string.action_ok) { _, _ ->
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION_PERMISSION)
                }
                .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.cancel() }
                .show()
        }
    }

    fun sendIsFrontChanged(isFront: Boolean) {
        val intent = Intent(FOREGROUND_ACTION)
        intent.putExtra("is_front", isFront)
        sendBroadcast(intent)
    }

    fun supportsPiPMode(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    private fun openManual(context: Context) {
        val url = context.getString(R.string.manual_link)
        val intentBuilder = CustomTabsIntent.Builder()
        intentBuilder.setShowTitle(true)
        val params = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.colorSettingsBackground))
            .build()
        intentBuilder.setDefaultColorSchemeParams(params)
        intentBuilder.setUrlBarHidingEnabled(true)

        val customTabsIntent = intentBuilder.build()
        val customTabsApps = context.packageManager.queryIntentActivities(customTabsIntent.intent, 0)
        if (customTabsApps.isNotEmpty()) {
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }
    }

    fun hideSystemBars() {
        if (::displayController.isInitialized) {
            displayController.hideSystemBars()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (::pipController.isInitialized) {
            pipController.onUserLeaveHint()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelButton(
    id: Int,
    tooltip: String? = null,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val button = @Composable {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            Icon(
                painterResource(id = id),
                contentDescription = tooltip,
                tint = if (active) Color(0xFFFFC400) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (tooltip != null) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(tooltip) } },
            state = rememberTooltipState()
        ) {
            button()
        }
    } else {
        button()
    }
}

@Composable
private fun SettingsRefreshDialog(
    onRefreshNow: () -> Unit,
    onChangeWithoutRefresh: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onChangeWithoutRefresh,
        title = { Text(text = stringResource(id = R.string.settings_refresh_required_title)) },
        text = { Text(text = stringResource(id = R.string.settings_refresh_required_msg)) },
        confirmButton = {
            TextButton(onClick = onRefreshNow) {
                Text(text = stringResource(id = R.string.settings_refresh_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onChangeWithoutRefresh) {
                Text(text = stringResource(id = R.string.settings_refresh_later))
            }
        }
    )
}

@Composable
fun BrowserOverlayLayer(
    showSubtitle: Boolean,
    subtitleText: String,
    subtitleVisible: Boolean,
    subtitleFontSize: Int,
    isCapture: Boolean,
    closeButtonVisible: Boolean,
    onSubtitleTap: () -> Unit,
    onCaptureClick: () -> Unit,
    onCloseClick: () -> Unit,
    webViewHeight: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
    containerHeight: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (showSubtitle && subtitleVisible) {
            androidx.compose.ui.layout.Layout(
                modifier = Modifier.fillMaxSize(),
                content = {
                    Text(
                        text = subtitleText.ifEmpty { stringResource(id = R.string.subtitle_default) },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = subtitleFontSize.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable { onSubtitleTap() }
                            .padding(horizontal = 8.dp)
                    )
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                
                val webViewBottomPx = if (webViewHeight != androidx.compose.ui.unit.Dp.Unspecified && containerHeight != androidx.compose.ui.unit.Dp.Unspecified) {
                    ((containerHeight + webViewHeight) / 2f).toPx()
                } else {
                    constraints.maxHeight.toFloat()
                }
                
                val paddingPx = 12.dp.toPx()
                var y = webViewBottomPx + paddingPx
                if (y + placeable.height > constraints.maxHeight - paddingPx) {
                    y = constraints.maxHeight - paddingPx - placeable.height
                }
                
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.placeRelative(
                        x = (constraints.maxWidth - placeable.width) / 2,
                        y = y.toInt()
                    )
                }
            }
        }

        if (isCapture) {
            IconButton(
                onClick = onCaptureClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(64.dp)
                    .glassyStyle(RoundedCornerShape(8.dp))
            ) {
                Icon(
                    painterResource(id = R.drawable.capture_icon),
                    stringResource(R.string.menu_tooltip_camera),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        if (closeButtonVisible) {
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }
    }
}

@Composable
fun BrowserScreenContent(
    viewModel: BrowserViewModel,
    errorText: MutableState<String>,
    subtitleTextValue: MutableState<String>,
    closeButtonVisible: MutableState<Boolean>,
    subtitleFontSize: MutableState<Int>,
    multiwinMarginDp: MutableState<Int>,
    manager: WebViewManager?,
    onViewCreated: (WebViewL) -> Unit,
    intent: Intent?,
    activity: BrowserActivity,
    onBackgroundTap: () -> Unit
) {
    val isCaption by viewModel.isCaptionMode.observeAsState(false)
    val isCapture by viewModel.isCaptureMode.observeAsState(false)
    val isKeep by viewModel.isKeepMode.observeAsState(false)
    val showFlash = remember { mutableStateOf(false) }

    val currentSubtitle = subtitleTextValue.value
    val subtitleVisible = remember { mutableStateOf(true) }
    LaunchedEffect(currentSubtitle) {
        if (currentSubtitle.isNotEmpty()) {
            subtitleVisible.value = true
        }
    }

    LaunchedEffect(isKeep) {
        if (isKeep) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onBackgroundTap() }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val ratio = 1200f / 720f
            val containerWidth = maxWidth.value
            val containerHeight = maxHeight.value

            val finalWidth: androidx.compose.ui.unit.Dp
            val finalHeight: androidx.compose.ui.unit.Dp

            if (containerWidth / containerHeight > ratio) {
                finalHeight = maxHeight
                finalWidth = maxHeight * ratio
            } else {
                finalWidth = maxWidth
                finalHeight = maxWidth / ratio
            }

            AndroidView(
                factory = { ctx ->
                    WebViewL(ctx).apply {
                        onViewCreated(this)
                        addJavascriptInterface(viewModel.k3dPatcher, "kantai3dInterface")
                        manager?.setHardwareAcceleratedFlag()
                        viewModel.connectorInfo = WebViewManager.getDefaultPage(activity)
                        val info = viewModel.connectorInfo
                        if (info != null && info.size == 2) {
                            manager?.setWebViewSettings(this)
                            WebViewManager.enableBrowserCookie(this)
                            manager?.setWebViewClient(activity, this)
                            manager?.setPopupView(this)
                            manager?.openPage(this, info)
                        }
                    }
                },
                modifier = Modifier
                    .size(width = finalWidth, height = finalHeight)
                    .padding(top = multiwinMarginDp.value.dp, bottom = multiwinMarginDp.value.dp)
                    .background(Color.Black)
                    .clickable(enabled = false) { }
                    .pointerInput(Unit) {
                        activity.pipController.handlePinchToPipGesture(this)
                    }
            )

            if (errorText.value.isNotEmpty()) {
                Text(
                    text = errorText.value,
                    color = Color(0xFF7987A8),
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            if (showFlash.value) {
                Box(modifier = Modifier.matchParentSize().background(Color.White.copy(0.5f)))
                LaunchedEffect(Unit) {
                    Handler().postDelayed({ showFlash.value = false }, 100)
                }
            }

            if (!activity.isInPictureInPictureModeState.value) {
                BrowserOverlayLayer(
                    showSubtitle = isCaption,
                    subtitleText = subtitleTextValue.value,
                    subtitleVisible = subtitleVisible.value,
                    subtitleFontSize = subtitleFontSize.value,
                    isCapture = isCapture,
                    closeButtonVisible = closeButtonVisible.value,
                    onSubtitleTap = { subtitleVisible.value = false },
                    onCaptureClick = {
                        activity.mContentView?.let { manager?.captureGameScreen(it) }
                        showFlash.value = true
                    },
                    onCloseClick = { activity.finish() },
                    webViewHeight = finalHeight,
                    containerHeight = maxHeight
                )
            }
        }
    }
}

@Preview(name = "Browser - Overlays + Toolbar", showBackground = true, widthDp = 800, heightDp = 480)
@Composable
fun BrowserScreenPreview() {
    GotobrowserTheme {
        ControlPreviewContent()
    }
}

@Preview(name = "Browser - Portrait", showBackground = true, widthDp = 480, heightDp = 800)
@Composable
fun BrowserScreenPortraitPreview() {
    GotobrowserTheme {
        ControlPreviewContent()
    }
}

@Composable
fun ControlPreviewContent() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .aspectRatio(1200f / 720f)
                .background(Color(0xFF101010)),
            contentAlignment = Alignment.Center
        ) {
            Text("WebView (not renderable in Preview)", color = Color.White)
        }

        BrowserOverlayLayer(
            showSubtitle = true,
            subtitleText = "Sample subtitle text",
            subtitleVisible = true,
            subtitleFontSize = 18,
            isCapture = true,
            closeButtonVisible = false,
            onSubtitleTap = {},
            onCaptureClick = {},
            onCloseClick = {}
        )

        ControlLayout(
            visible = true,
            onVisibleChange = {},
            onRefreshClick = {},
            onSettingsClick = {},
            onHelpClick = {}
        ) {
            PanelButton(id = R.drawable.volume_off, tooltip = stringResource(R.string.menu_tooltip_mute), active = true, onClick = {})
            PanelButton(id = R.drawable.camera_icon, tooltip = stringResource(R.string.menu_tooltip_camera), active = true, onClick = {})
            PanelButton(id = R.drawable.screen_lock, tooltip = stringResource(R.string.menu_tooltip_lock), onClick = {})
            PanelButton(id = R.drawable.light_mode, tooltip = stringResource(R.string.menu_tooltip_brighton), onClick = {})
            PanelButton(id = R.drawable.caption_icon, tooltip = stringResource(R.string.menu_tooltip_cc), active = true, onClick = {})
        }
    }
}
