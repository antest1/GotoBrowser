package com.antest1.gotobrowser.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.SslErrorHandler
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.antest1.gotobrowser.Browser.GameAreaLayout
import com.antest1.gotobrowser.Browser.GameGeometry
import com.antest1.gotobrowser.Browser.WebViewL
import com.antest1.gotobrowser.Browser.WebViewManager
import com.antest1.gotobrowser.BuildConfig
import com.antest1.gotobrowser.Constants.CONN_DMM
import com.antest1.gotobrowser.Constants.DEFAULT_ALTER_GADGET_URL
import com.antest1.gotobrowser.Constants.DEFAULT_SUBTITLE_FONT_SIZE
import com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT
import com.antest1.gotobrowser.Constants.PREF_ALTER_ENDPOINT
import com.antest1.gotobrowser.Constants.PREF_ALTER_AUTO_DISABLE
import com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET
import com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD
import com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD_PROXY
import com.antest1.gotobrowser.Constants.PREF_BROADCAST
import com.antest1.gotobrowser.Constants.PREF_CONNECTOR
import com.antest1.gotobrowser.Constants.PREF_CURSOR_MODE
import com.antest1.gotobrowser.Constants.PREF_DEVTOOLS_DEBUG
import com.antest1.gotobrowser.Constants.PREF_DISABLE_REFRESH_DIALOG
import com.antest1.gotobrowser.Constants.PREF_DMM_ID
import com.antest1.gotobrowser.Constants.PREF_DOWNLOAD_RETRY
import com.antest1.gotobrowser.Constants.PREF_FONT_PREFETCH
import com.antest1.gotobrowser.Constants.PREF_FPS_LIMIT
import com.antest1.gotobrowser.Constants.PREF_GAME_ALIGN
import com.antest1.gotobrowser.Constants.PREF_GAME_ALIGN_CENTER
import com.antest1.gotobrowser.Constants.PREF_GAME_ALIGN_TOP
import com.antest1.gotobrowser.Constants.PREF_KEYBOARD
import com.antest1.gotobrowser.Constants.PREF_LANDSCAPE
import com.antest1.gotobrowser.Constants.PREF_LEGACY_RENDERER
import com.antest1.gotobrowser.Constants.PREF_MOD_CRIT
import com.antest1.gotobrowser.Constants.PREF_MOD_FPS
import com.antest1.gotobrowser.Constants.PREF_MOD_KANTAI3D
import com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH
import com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH_NAME
import com.antest1.gotobrowser.Constants.PREF_MULTIWIN_MARGIN
import com.antest1.gotobrowser.Constants.PREF_PANELSTART
import com.antest1.gotobrowser.Constants.PREF_PIP_MODE
import com.antest1.gotobrowser.Constants.PREF_TOOLBAR_POSITION
import com.antest1.gotobrowser.Constants.PREF_TOOLBAR_POSITION_TOP
import com.antest1.gotobrowser.Constants.PREF_TOOLBAR_SWIPE
import com.antest1.gotobrowser.Constants.PREF_SILENT
import com.antest1.gotobrowser.Constants.PREF_SUBTITLE_FONTSIZE
import com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE
import com.antest1.gotobrowser.Constants.PREF_USE_EXTCACHE
import com.antest1.gotobrowser.Constants.REQUEST_NOTIFICATION_PERMISSION
import com.antest1.gotobrowser.Helpers.BackPressCloseHandler
import com.antest1.gotobrowser.Helpers.KcUtils
import com.antest1.gotobrowser.Notification.ScreenshotNotification
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.Subtitle.SubtitleProviderUtils
import com.antest1.gotobrowser.ui.component.ControlLayout
import com.antest1.gotobrowser.ui.component.DialogButton
import com.antest1.gotobrowser.ui.component.LoadingStatusOverlay
import com.antest1.gotobrowser.ui.component.MessageDialog
import com.antest1.gotobrowser.ui.component.MessageDialogHost
import com.antest1.gotobrowser.ui.component.ServerNotice
import com.antest1.gotobrowser.ui.component.ServerNoticeAlignment
import com.antest1.gotobrowser.ui.component.ServerNoticeCard
import com.antest1.gotobrowser.ui.component.cancelForPip
import com.antest1.gotobrowser.ui.component.floatingToolbarEdgeMargin
import com.antest1.gotobrowser.ui.component.loadingConnectorTag
import com.antest1.gotobrowser.ui.component.RefreshOption
import com.antest1.gotobrowser.ui.component.SettingsBottomSheet
import com.antest1.gotobrowser.ui.component.rememberLoadingSnapshot
import com.antest1.gotobrowser.ui.component.refreshAttentionColor
import com.antest1.gotobrowser.ui.component.requiresFullRefresh
import com.antest1.gotobrowser.ui.component.rememberToolbarTooltipPositionProvider
import com.antest1.gotobrowser.ui.component.glassyStyle
import com.antest1.gotobrowser.ui.theme.GotobrowserTheme
import com.antest1.gotobrowser.ui.theme.SettingsTheme
import java.util.Locale
import java.util.concurrent.Executors

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
    private lateinit var gameArea: GameAreaLayout

    val isInPictureInPictureModeState = mutableStateOf(false)
    val loadingTracker = LoadingTracker()
    // Connector of the load in progress, named in the loading card (changes apply on reload).
    private val loadConnectorTag = mutableStateOf<String?>(null)
    private val serverNotice = mutableStateOf<ServerNotice?>(null)
    private val errorText = mutableStateOf("")
    private val closeButtonVisible = mutableStateOf(false)
    private val toolbarVisible = mutableStateOf(false)
    /** False when the game fills the screen, leaving no margin to tap and no edge to swipe from. */
    private var toolbarRevealAvailable = true
    private val settingsSheetVisible = mutableStateOf(false)
    private val refreshPromptVisible = mutableStateOf(false)
    /** What the refresh prompt says; a changed setting and a cleared cache explain it differently. */
    private val refreshPromptReason = mutableStateOf(RefreshPromptReason.SETTING_CHANGED)
    private val refreshOption = mutableStateOf(RefreshOption.QUICK)
    private val messageDialogs = mutableStateListOf<MessageDialog>()
    private val subtitleFontSize = mutableStateOf(DEFAULT_SUBTITLE_FONT_SIZE)
    /** Where the game sits when the screen leaves a margin above and below it. */
    private val gameVerticalBias = mutableStateOf(GameGeometry.BIAS_CENTER)
    /** Toolbar docked at the top with refresh first (landscape), as in 3.x. */
    private val toolbarAtTop = mutableStateOf(false)
    /** Whether an edge swipe reveals the hidden toolbar. */
    private val toolbarSwipe = mutableStateOf(true)
    private val subtitleLoader = Executors.newSingleThreadExecutor()

    // Settings applied immediately without reloading the page.
    private val liveSettings = setOf(
        PREF_LANDSCAPE,
        PREF_MULTIWIN_MARGIN,
        PREF_GAME_ALIGN,
        PREF_TOOLBAR_POSITION,
        PREF_TOOLBAR_SWIPE,
        PREF_SUBTITLE_FONTSIZE,
        PREF_DOWNLOAD_RETRY,
        PREF_PIP_MODE,
        PREF_PANELSTART,
        PREF_DISABLE_REFRESH_DIALOG,
        PREF_DEVTOOLS_DEBUG,
        PREF_FPS_LIMIT,
        PREF_KEYBOARD,
        PREF_SUBTITLE_LOCALE,
        PREF_BROADCAST,
        // Read by ResourceProcess on each game load; nothing to apply now.
        PREF_ALTER_AUTO_DISABLE
    )

    // Settings that only take effect on page load; they must be re-read on refresh.
    private val refreshSettings = setOf(
        PREF_ADJUSTMENT,
        PREF_SILENT,
        PREF_FONT_PREFETCH,
        PREF_DMM_ID,
        PREF_USE_EXTCACHE,
        PREF_CONNECTOR,
        PREF_CURSOR_MODE,
        PREF_LEGACY_RENDERER,
        PREF_ALTER_GADGET,
        PREF_ALTER_METHOD,
        PREF_ALTER_ENDPOINT,
        PREF_MOD_FPS,
        PREF_MOD_CRIT,
        PREF_MOD_KANTAI3D,
        PREF_MOD_KCCP_LANG_PATCH,
        PREF_MOD_KCCP_LANG_PATCH_NAME
    )

    @SuppressLint("SourceLockedOrientationActivity", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
        viewModel = ViewModelProvider(this)[BrowserViewModel::class.java]
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        // First touch of the WebView provider. It throws when the system WebView can't be
        // loaded (e.g. while it is being updated); tell the user instead of crashing.
        try {
            WebViewManager.setWebViewDebugging(viewModel.sharedPref.getBoolean(PREF_DEVTOOLS_DEBUG, false))
        } catch (e: RuntimeException) {
            KcUtils.reportException(e)
            KcUtils.showToast(applicationContext, R.string.webview_unavailable)
            finish()
            return
        }
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
        // Hint shown only on first launch (not on rotation), until the first voice line or a tap.
        if (!viewModel.isSubtitleHintShown) {
            viewModel.isSubtitleHintShown = true
            viewModel.setSubtitleText(getString(R.string.subtitle_default))
        }

        manager = WebViewManager(this)
        manager?.setDataDirectorySuffix()

        if (isAppLaunch) {
            window.decorView.post {
                checkAppUpdateOnStart()
                warnIfKcanotifyBroadcastDisabled()
            }
        }

        val webView = createWebView()
        mContentView = webView
        pipController = PipController(this, webView, { isInPictureInPictureModeState.value = it }, { hideSystemBars() })
        pipController.setupPinchToPipGesture()
        gameArea = GameAreaLayout(this).apply { addView(webView) }
        applyGameAlignment()
        applyToolbarPosition()
        applyToolbarSwipe()
        // Keep the game area clear of the display cutout. Since targetSdk 35 the
        // window always extends into the cutout, so on 16:10 tablets the WebView
        // could otherwise be partially covered. Insets are passed through, not consumed.
        // Must stay in sync with safeOverlayInsets() below (system bars in multi-window only).
        ViewCompat.setOnApplyWindowInsetsListener(gameArea) { v, insets ->
            val types = if (isInMultiWindowMode) {
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            } else {
                WindowInsetsCompat.Type.displayCutout()
            }
            val safe = insets.getInsets(types)
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        displayController = DisplayController(this, webView) { top, bottom -> gameArea.setDividerMargin(top, bottom) }

        val backgroundTapView = View(this).apply {
            setOnClickListener { toolbarVisible.value = !toolbarVisible.value }
            // A plain click also fires after a drag that stays inside this full-screen view,
            // so an edge swipe that brings up the system bars toggled the toolbar.
            // Only a real tap (within touch slop, not cancelled) counts as a click.
            val tapDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent) = true
                override fun onSingleTapUp(e: MotionEvent) = this@apply.performClick()
            })
            tapDetector.setOnDoubleTapListener(null)
            setOnTouchListener { _, event -> tapDetector.onTouchEvent(event); true }
        }
        val rootLayout = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            addView(
                backgroundTapView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            addView(
                gameArea,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        composeView.setContent {
            GotobrowserTheme {
                LaunchedEffect(settingsSheetVisible.value, refreshPromptVisible.value, messageDialogs.size) {
                    hideSystemBars()
                }

                val loadState by rememberLoadingSnapshot(loadingTracker)
                val notice = serverNotice.value
                val refreshAttention = refreshAttentionColor(loadState, notice != null)
                // The guidance points at the refresh FAB, so bring the toolbar (and the FAB) on screen.
                LaunchedEffect(refreshAttention != null) {
                    if (refreshAttention != null && !isInPictureInPictureModeState.value) {
                        toolbarVisible.value = true
                    }
                }

                // Insets are NOT applied at this root on purpose: the floating toolbar
                // slides off the physical screen edge, so it needs unpadded coordinates
                // and handles the cutout itself. Each overlay applies safeOverlayInsets()
                // where it needs to stay clear of the cutout / system bars.
                Box(modifier = Modifier.fillMaxSize()) {
                    BrowserOverlayContent(
                        viewModel = viewModel,
                        loadState = loadState,
                        serverNotice = notice,
                        onDismissServerNotice = { serverNotice.value = null },
                        errorText = errorText,
                        closeButtonVisible = closeButtonVisible,
                        subtitleFontSize = subtitleFontSize,
                        activity = this@BrowserActivity,
                        onCapture = { manager?.captureGameScreen(webView) },
                        onClose = { finish() },
                        gameVerticalBias = gameVerticalBias.value,
                        loadingConnectorTag = loadConnectorTag.value
                    )

                    ControlLayout(
                        visible = toolbarVisible.value,
                        onVisibleChange = { toolbarVisible.value = it },
                        onRevealAvailableChange = { toolbarRevealAvailable = it },
                        onRefreshClick = { showRefreshDialog(refreshOption.value) },
                        onQuickRefreshClick = { showRefreshDialog(RefreshOption.QUICK) },
                        onFullRefreshClick = { showRefreshDialog(RefreshOption.FULL) },
                        selectedRefreshOption = refreshOption.value,
                        onRefreshOptionChange = { option ->
                            refreshOption.value = option
                            viewModel.setQuickGameReload(option == RefreshOption.QUICK)
                        },
                        onSettingsClick = { settingsSheetVisible.value = true },
                        refreshAttention = refreshAttention,
                        forceFullRefresh = requiresFullRefresh(loadState, notice),
                        gameVerticalBias = gameVerticalBias.value,
                        toolbarAtTop = toolbarAtTop.value,
                        swipeToReveal = toolbarSwipe.value
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
                            onHelpClick = { openManual(this@BrowserActivity) },
                            onSettingChanged = { key -> onSettingChanged(key) },
                            onCacheCleared = { showRefreshPrompt(RefreshPromptReason.CACHE_CLEARED) }
                        )
                    }

                    if (refreshPromptVisible.value) {
                        // Same palette as the sheet it is asked from.
                        SettingsTheme {
                            SettingsRefreshDialog(
                                reason = refreshPromptReason.value,
                                onRefreshNow = {
                                    refreshPromptVisible.value = false
                                    settingsSheetVisible.value = false
                                    refreshPageOrFinish(RefreshOption.FULL)
                                },
                                onChangeWithoutRefresh = { refreshPromptVisible.value = false }
                            )
                        }
                    }

                    MessageDialogHost(messageDialogs)
                }
            }
        }

        rootLayout.addView(
            composeView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(rootLayout)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })

        pipController.setupSmoothPipAnimation()
        displayController.applyKeyboardSetting()
        displayController.applyFpsLimit()
        hideSystemBars()
    }

    private fun createWebView(): WebViewL {
        val webView = WebViewL(this)
        webView.addJavascriptInterface(viewModel.k3dPatcher, "kantai3dInterface")
        manager?.setHardwareAcceleratedFlag()
        val info = WebViewManager.getDefaultPage(this)
        viewModel.connectorInfo = info
        if (info.size == 2) {
            manager?.setWebViewSettings(webView)
            WebViewManager.enableBrowserCookie(webView)
            manager?.setWebViewClient(this, webView)
            manager?.setPopupView(webView)
            startLoading()
            reloadSettings { manager?.openPage(webView, info) }
        }
        webView.setBackgroundColor(android.graphics.Color.BLACK)
        return webView
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

    /** @param force reload even if the same locale/storage was already loaded (e.g. new data downloaded) */
    fun loadSubtitleData(force: Boolean = false) {
        // Loaded off the main thread:
        // The result is only consumed later when a voice line plays.
        // A single-thread executor keeps loads in order, so the latest locale wins.
        // "" means no language has been picked yet; there is no quotes file to load.
        val subtitleLocale = viewModel.sharedPref.getString(PREF_SUBTITLE_LOCALE, "") ?: ""
        if (subtitleLocale.isEmpty()) {
            viewModel.subtitleRequestKey = null
            viewModel.isSubtitleLoaded = false
            return
        }
        val requestKey = "$subtitleLocale|${viewModel.sharedPref.getBoolean(PREF_USE_EXTCACHE, false)}"
        if (!force && requestKey == viewModel.subtitleRequestKey) return
        viewModel.subtitleRequestKey = requestKey

        val appContext = applicationContext
        subtitleLoader.execute {
            val loaded = SubtitleProviderUtils.getSubtitleProvider(subtitleLocale)
                .loadQuoteData(appContext, subtitleLocale)
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    viewModel.isSubtitleLoaded = loaded
                    // Allow a retry on the next refresh if loading failed
                    if (!loaded && viewModel.subtitleRequestKey == requestKey) {
                        viewModel.subtitleRequestKey = null
                    }
                }
            }
        }
    }

    fun setStartedFlag() {
        viewModel.isStartedFlag = true
    }

    fun setErrorText(text: String) { errorText.value = text }
    fun setSubtitleText(text: String) { viewModel.setSubtitleText(text) }
    fun setCloseButtonVisible(visible: Boolean) { closeButtonVisible.value = visible }

    private fun onSettingChanged(key: String) {
        when (key) {
            in liveSettings -> applyLiveSetting(key)
            in refreshSettings -> showRefreshPrompt(RefreshPromptReason.SETTING_CHANGED)
            else -> {
                // Unclassified keys fall back to a refresh prompt.
                if (BuildConfig.DEBUG) Log.w("GOTO", "unclassified setting key: $key")
                showRefreshPrompt(RefreshPromptReason.SETTING_CHANGED)
            }
        }
    }

    private fun showRefreshPrompt(reason: RefreshPromptReason) {
        refreshPromptReason.value = reason
        refreshPromptVisible.value = true
    }

    private fun applyLiveSetting(key: String) {
        when (key) {
            PREF_LANDSCAPE -> displayController.updateOrientationLock()
            PREF_MULTIWIN_MARGIN -> displayController.updateMultiwindowMargin()
            PREF_GAME_ALIGN -> applyGameAlignment()
            PREF_TOOLBAR_POSITION -> applyToolbarPosition()
            PREF_TOOLBAR_SWIPE -> applyToolbarSwipe()
            PREF_SUBTITLE_FONTSIZE -> subtitleFontSize.value = settingsViewModel.getSubtitleFontSize()
            PREF_PIP_MODE -> pipController.setupSmoothPipAnimation()
            PREF_KEYBOARD -> displayController.applyKeyboardSetting()
            PREF_SUBTITLE_LOCALE -> loadSubtitleData()
            PREF_DEVTOOLS_DEBUG -> WebViewManager.setWebViewDebugging(viewModel.sharedPref.getBoolean(PREF_DEVTOOLS_DEBUG, false))
            PREF_DISABLE_REFRESH_DIALOG -> viewModel.setNoRefreshPopupMode(viewModel.sharedPref.getBoolean(PREF_DISABLE_REFRESH_DIALOG, false))
            PREF_FPS_LIMIT -> displayController.applyFpsLimit()
        }
    }

    // The WebView (GameAreaLayout) and the overlays lined up with it must change together.
    private fun applyGameAlignment() {
        val bias = when (viewModel.sharedPref.getString(PREF_GAME_ALIGN, PREF_GAME_ALIGN_CENTER)) {
            PREF_GAME_ALIGN_TOP -> GameGeometry.BIAS_TOP
            else -> GameGeometry.BIAS_CENTER
        }
        gameVerticalBias.value = bias
        gameArea.verticalBias = bias
    }

    private fun applyToolbarSwipe() {
        toolbarSwipe.value = viewModel.sharedPref.getBoolean(PREF_TOOLBAR_SWIPE, true)
    }

    private fun applyToolbarPosition() {
        toolbarAtTop.value = PREF_TOOLBAR_POSITION_TOP == viewModel.sharedPref.getString(PREF_TOOLBAR_POSITION, null)
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
        // Back only runs the exit flow: the back gesture shares the screen edge with the
        // system bar reveal, so it must not toggle the toolbar too. The exception is a screen
        // with no margin and no reveal strip, where back is the only way to bring it up.
        if (!toolbarVisible.value && !toolbarRevealAvailable) {
            toolbarVisible.value = true
            return
        }
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
            // The sheet and its dialogs are separate windows the PiP window cannot show usefully.
            settingsSheetVisible.value = false
            refreshPromptVisible.value = false
            messageDialogs.cancelForPip()
        }
        val rot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }
        viewModel.k3dPatcher.setRotation(rot)
        viewModel.k3dPatcher.resume()
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode) {
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
        subtitleLoader.shutdown()
        mContentView?.removeAllViews()
        mContentView?.destroy()
        super.onDestroy()
    }

    /**
     * Shows [dialog] as a Material 3 dialog over the game, in the settings sheet's
     * theme. Safe to call from any thread; ignored once the activity is finishing.
     *
     * @return false if the dialog will not be shown, so callers waiting on an
     *         answer can stop waiting
     */
    fun showMessageDialog(dialog: MessageDialog): Boolean {
        if (isFinishing || isDestroyed) return false
        runOnUiThread { if (!isFinishing && !isDestroyed) messageDialogs.add(dialog) }
        return true
    }

    fun showWebkitErrorDialog(errorCode: Int, description: String, failingUrl: String) {
        showMessageDialog(
            MessageDialog(
                title = KcUtils.getWebkitErrorCodeText(errorCode),
                message = (description + "\n\n" + failingUrl).trim { it <= ' ' },
                confirm = DialogButton("Reload") { refreshPageOrFinish(RefreshOption.FULL) },
                dismiss = DialogButton("Close")
            )
        )
    }

    /**
     * A main-frame load failed. While the first screen is loading it is shown in the
     * loading card (which stays, unlike a dialog that leaves a black screen once closed).
     *
     * @return true if handled there; otherwise the caller shows the error dialog
     */
    fun onMainFrameLoadError(errorCode: Int, description: String?, failingUrl: String?): Boolean =
        loadingTracker.onFailure(
            LoadingTracker.Failure.Network(errorCode, description.orEmpty(), failingUrl)
        )

    /** A main-frame HTTP error (4xx/5xx), shown in the loading card while loading. */
    fun onMainFrameHttpError(statusCode: Int, url: String?) {
        loadingTracker.onFailure(LoadingTracker.Failure.Http(statusCode, "HTTP $statusCode", url))
    }

    /** A kcsapi call returned api_result != 1. Safe to call from any thread. */
    fun showServerNotice(code: Int, url: String, message: String) {
        postServerNotice(ServerNotice(ServerNotice.Kind.REJECTED, code.toString(), apiOf(url), message), replace = true)
    }

    /**
     * A kcsapi call got no usable response: no network, a timeout ([status] 0) or an HTTP
     * error. Safe to call from any thread. Several calls usually fail together when the
     * connection drops, so the first notice stays until it is dismissed.
     */
    fun showRequestFailure(url: String, status: Int, code: String, message: String) {
        val notice = if (status > 0) {
            ServerNotice(ServerNotice.Kind.HTTP, "HTTP $status", apiOf(url), message)
        } else {
            ServerNotice(ServerNotice.Kind.NETWORK, code, apiOf(url), message)
        }
        postServerNotice(notice, replace = false)
    }

    private fun postServerNotice(notice: ServerNotice, replace: Boolean) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            if (replace || serverNotice.value == null) serverNotice.value = notice
        }
    }

    private fun apiOf(url: String): String = url.substringAfter("/kcsapi/", url).substringBefore('?')

    fun showSslErrorDialog(handler: SslErrorHandler, error: SslError) {
        showMessageDialog(
            MessageDialog(
                title = KcUtils.getSslErrorCodeTitle(error.primaryError),
                message = (KcUtils.getSslErrorCodeDescription(error.primaryError) + "\n\nurl: " + error.url).trim { it <= ' ' },
                confirm = DialogButton("Close") { handler.cancel() },
                dismiss = DialogButton("Proceed") { handler.proceed() }
            )
        )
    }

    private fun startLoading() {
        loadConnectorTag.value = loadingConnectorTag(viewModel.sharedPref.getString(PREF_CONNECTOR, CONN_DMM))
        loadingTracker.start()
    }

    private fun refreshPageOrFinish(option: RefreshOption = refreshOption.value) {
        viewModel.connectorInfo = WebViewManager.getDefaultPage(this)
        val info = viewModel.connectorInfo
        if (manager != null && info != null && info.size == 2) {
            startLoading()
            serverNotice.value = null
            errorText.value = ""
            reloadSettings {
                mContentView?.let {
                    when (option) {
                        RefreshOption.QUICK -> {
                            loadingTracker.onGameFrameReload()
                            manager?.refreshGameFrame(it)
                        }
                        RefreshOption.FULL -> manager?.refreshPage(it)
                    }
                }
            }
        } else {
            finish()
        }
    }

    /**
     * Re-applies the settings that are only read on page load (see refreshSettings).
     * [onReady] runs once it is safe to load the page.
     */
    private fun reloadSettings(onReady: () -> Unit) {
        viewModel.k3dPatcher.pause()
        viewModel.prepareModPatchers()
        viewModel.k3dPatcher.resume()
        mContentView?.let { manager?.applyRendererSetting(it) }
        loadSubtitleData()
        applyGadgetProxy(onReady)
    }

    private fun applyGadgetProxy(onReady: () -> Unit) {
        val pref = viewModel.sharedPref
        val useProxy = pref.getBoolean(PREF_ALTER_GADGET, false) &&
                PREF_ALTER_METHOD_PROXY == pref.getString(PREF_ALTER_METHOD, "") &&
                CONN_DMM == pref.getString(PREF_CONNECTOR, CONN_DMM)
        val proceed = Runnable {
            runOnUiThread { if (!isFinishing && !isDestroyed) onReady() }
        }
        if (useProxy) {
            val endpoint = pref.getString(PREF_ALTER_ENDPOINT, DEFAULT_ALTER_GADGET_URL)
            WebViewManager.setKcCacheProxy(endpoint, proceed) {
                KcUtils.showToast(applicationContext, R.string.setting_alter_method_proxy_error_toast)
                proceed.run()
            }
        } else {
            WebViewManager.clearKcCacheProxy(proceed)
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
            showMessageDialog(
                MessageDialog(
                    title = getString(titleRes),
                    message = getString(messageRes),
                    confirm = DialogButton(getString(R.string.action_ok)) { refreshPageOrFinish(option) },
                    dismiss = DialogButton(getString(R.string.action_cancel)) { mContentView?.resumeTimers() },
                    cancelOnPip = true
                )
            )
        }
    }

    fun showLogoutDialog() {
        mContentView?.pauseTimers()
        showMessageDialog(
            MessageDialog(
                title = getString(R.string.app_name),
                message = getString(R.string.logout_msg),
                confirm = DialogButton(getString(R.string.action_ok)) {
                    mContentView?.let { manager?.logoutGame(it) }
                },
                dismiss = DialogButton(getString(R.string.action_cancel)) { mContentView?.resumeTimers() },
                cancelOnPip = true
            )
        )
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

        showMessageDialog(
            MessageDialog(
                title = getString(R.string.kcanotify_broadcast_dialog_title),
                message = String.format(
                    Locale.US,
                    getString(R.string.kcanotify_broadcast_dialog_message),
                    getString(R.string.mode_broadcast),
                    getString(R.string.action_ok)
                ),
                confirm = DialogButton(getString(R.string.action_ok)) {
                    viewModel.sharedPref.edit().putBoolean(PREF_BROADCAST, true).apply()
                },
                dismiss = DialogButton(getString(R.string.action_cancel))
            )
        )
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
            showMessageDialog(
                MessageDialog(
                    title = getString(R.string.app_name),
                    message = getString(R.string.noti_screenshot_permission_message),
                    confirm = DialogButton(getString(R.string.action_ok)) {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION_PERMISSION)
                    },
                    dismiss = DialogButton(getString(R.string.action_cancel))
                )
            )
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

/**
 * Display cutout, plus the system bars in multi-window mode only. IME is
 * intentionally excluded so overlays do not jump when the keyboard opens on the login page.
 *
 * In fullscreen the bars are always hidden (immersive), so they are left out: closing a
 * dialog window (e.g. the settings sheet) while the bars are swiped in briefly reports them
 * as visible to the activity, which made overlays jump inward and back.
 */
@Composable
fun safeOverlayInsets(): WindowInsets {
    val activity = LocalActivity.current
    // Entering or leaving multi-window comes with a configuration change (window size).
    val multiWindow = remember(LocalConfiguration.current) { activity?.isInMultiWindowMode == true }
    return if (multiWindow) WindowInsets.systemBars.union(WindowInsets.displayCutout)
    else WindowInsets.displayCutout
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelButton(
    id: Int,
    tooltip: String? = null,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val buttonSize = dimensionResource(R.dimen.browser_panel_button_size)
    val iconSize = dimensionResource(R.dimen.browser_panel_button_icon_size)
    val button = @Composable {
        IconButton(onClick = onClick, modifier = Modifier.size(buttonSize)) {
            Icon(
                painterResource(id = id),
                contentDescription = tooltip,
                tint = if (active) Color(0xFFFFC400) else Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }

    if (tooltip != null) {
        TooltipBox(
            // Toolbar hugs the start edge, so side tooltips open toward the game area.
            positionProvider = rememberToolbarTooltipPositionProvider(TooltipAnchorPosition.End),
            tooltip = { PlainTooltip { Text(tooltip) } },
            state = rememberTooltipState()
        ) {
            button()
        }
    } else {
        button()
    }
}

internal enum class RefreshPromptReason(val messageRes: Int, val laterRes: Int) {
    SETTING_CHANGED(R.string.settings_refresh_required_msg, R.string.settings_refresh_later),
    CACHE_CLEARED(R.string.cache_cleared_refresh_msg, R.string.refresh_later)
}

@Composable
internal fun SettingsRefreshDialog(
    onRefreshNow: () -> Unit,
    onChangeWithoutRefresh: () -> Unit,
    reason: RefreshPromptReason = RefreshPromptReason.SETTING_CHANGED
) {
    AlertDialog(
        onDismissRequest = onChangeWithoutRefresh,
        title = { Text(text = stringResource(id = R.string.settings_refresh_required_title)) },
        text = { Text(text = stringResource(id = reason.messageRes)) },
        confirmButton = {
            TextButton(onClick = onRefreshNow) {
                Text(text = stringResource(id = R.string.settings_refresh_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onChangeWithoutRefresh) {
                Text(text = stringResource(id = reason.laterRes))
            }
        }
    )
}

// White bold text with a black outline and drop shadow, so it stays readable over any game scene.
@Composable
private fun SubtitleText(text: String, fontSize: Int, modifier: Modifier = Modifier) {
    val baseStyle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = fontSize.sp,
        textAlign = TextAlign.Center
    )
    val outlineWidth = with(LocalDensity.current) { 3.dp.toPx() }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = baseStyle.copy(
                color = Color.Black,
                drawStyle = Stroke(width = outlineWidth, join = StrokeJoin.Round),
                shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
            )
        )
        Text(text = text, style = baseStyle.copy(color = Color.White))
    }
}

@Composable
fun BrowserOverlayLayer(
    showSubtitle: Boolean,
    subtitleText: String,
    subtitleFontSize: Int,
    isCapture: Boolean,
    closeButtonVisible: Boolean,
    onSubtitleTap: () -> Unit,
    onCaptureClick: () -> Unit,
    onCloseClick: () -> Unit,
    gameWidth: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
    gameHeight: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
    /** Matches GameAreaLayout.verticalBias; see GameGeometry.fit. */
    gameVerticalBias: Float = GameGeometry.BIAS_CENTER
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (showSubtitle && subtitleText.isNotEmpty()) {
            androidx.compose.ui.layout.Layout(
                modifier = Modifier.fillMaxSize(),
                content = {
                    SubtitleText(
                        text = subtitleText,
                        fontSize = subtitleFontSize,
                        modifier = Modifier
                            .clickable { onSubtitleTap() }
                            .padding(horizontal = 8.dp)
                    )
                }
            ) { measurables, constraints ->
                // The game sits in this layer as GameAreaLayout places it. The
                // subtitle wraps to the game width so it never spills into the side
                // letterbox, and sits just below the game, clamped to the screen bottom.
                // maxWidth can be 0 while the window is being resized; keep the range non-empty.
                val gameW = if (gameWidth.isSpecified) gameWidth.roundToPx().coerceIn(minOf(1, constraints.maxWidth), constraints.maxWidth) else constraints.maxWidth
                val gameH = if (gameHeight.isSpecified) gameHeight.roundToPx().coerceIn(0, constraints.maxHeight) else constraints.maxHeight
                val gameTop = ((constraints.maxHeight - gameH) * (1f + gameVerticalBias) / 2f).toInt()
                val gameBottom = gameTop + gameH

                val placeable = measurables.first().measure(
                    constraints.copy(minWidth = 0, minHeight = 0, maxWidth = gameW)
                )

                val paddingPx = 12.dp.roundToPx()
                val y = minOf(gameBottom + paddingPx, constraints.maxHeight - paddingPx - placeable.height)

                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.placeRelative(
                        x = (constraints.maxWidth - placeable.width) / 2,
                        y = y
                    )
                }
            }
        }

        if (isCapture) {
            // Placed below the top-right settings button so the two never overlap,
            // and right-aligned with that row by sharing its end padding.
            val cameraSize = dimensionResource(R.dimen.browser_camera_size)
            val cameraIconSize = dimensionResource(R.dimen.browser_camera_icon_size)
            val topMenuPadding = dimensionResource(R.dimen.top_menu_padding)
            val topMenuButtonSize = dimensionResource(R.dimen.top_menu_button_size)
            val topMenuGap = dimensionResource(R.dimen.top_menu_gap)
            val edgeMargin = floatingToolbarEdgeMargin()
            IconButton(
                onClick = onCaptureClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = topMenuPadding + topMenuButtonSize + topMenuGap,
                        end = edgeMargin
                    )
                    .size(cameraSize)
                    .glassyStyle(RoundedCornerShape(8.dp))
            ) {
                Icon(
                    painterResource(id = R.drawable.capture_icon),
                    stringResource(R.string.menu_tooltip_camera),
                    tint = Color.White,
                    modifier = Modifier.size(cameraIconSize)
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
fun BrowserOverlayContent(
    viewModel: BrowserViewModel,
    loadState: LoadingTracker.Snapshot,
    serverNotice: ServerNotice?,
    onDismissServerNotice: () -> Unit,
    errorText: MutableState<String>,
    closeButtonVisible: MutableState<Boolean>,
    subtitleFontSize: MutableState<Int>,
    activity: BrowserActivity,
    onCapture: () -> Unit,
    onClose: () -> Unit,
    /** Matches GameAreaLayout.verticalBias; see GameGeometry.fit. */
    gameVerticalBias: Float = GameGeometry.BIAS_CENTER,
    /** See loadingConnectorTag in LoadingStatus. */
    loadingConnectorTag: String? = null
) {
    val isCaption by viewModel.isCaptionMode.observeAsState(false)
    val subtitleText by viewModel.subtitleText.observeAsState("")
    val isCapture by viewModel.isCaptureMode.observeAsState(false)
    val isKeep by viewModel.isKeepMode.observeAsState(false)
    val showFlash = remember { mutableStateOf(false) }

    LaunchedEffect(isKeep) {
        if (isKeep) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // Same insets GameAreaLayout receives as padding, so the WebView geometry
    // computed below matches where the game is actually drawn.
    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(safeOverlayInsets())) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val game = GameGeometry.fit(maxWidth.value, maxHeight.value, gameVerticalBias)
            val gameWidth = game.width.dp
            val gameHeight = game.height.dp

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
                // Status sits within the game area: loading card centered (or at the bottom
                // while the game draws its own loading screen), server errors centered as well,
                // below the message at the top of the game's own connection error screen.
                Box(modifier = Modifier.align(BiasAlignment(0f, gameVerticalBias)).size(gameWidth, gameHeight)) {
                    LoadingStatusOverlay(loadState, connectorTag = loadingConnectorTag)
                    ServerNoticeCard(
                        notice = serverNotice,
                        onDismiss = onDismissServerNotice,
                        modifier = Modifier.align(ServerNoticeAlignment).padding(16.dp)
                    )
                }

                BrowserOverlayLayer(
                    showSubtitle = isCaption,
                    subtitleText = subtitleText,
                    subtitleFontSize = subtitleFontSize.value,
                    isCapture = isCapture,
                    closeButtonVisible = closeButtonVisible.value,
                    // Clear rather than hide, so the next voice line (even the same one) shows again.
                    onSubtitleTap = { viewModel.setSubtitleText("") },
                    onCaptureClick = {
                        onCapture()
                        showFlash.value = true
                    },
                    onCloseClick = onClose,
                    gameWidth = gameWidth,
                    gameHeight = gameHeight,
                    gameVerticalBias = gameVerticalBias
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
            onSettingsClick = {}
        ) {
            PanelButton(id = R.drawable.volume_off, tooltip = stringResource(R.string.menu_tooltip_mute), active = true, onClick = {})
            PanelButton(id = R.drawable.camera_icon, tooltip = stringResource(R.string.menu_tooltip_camera), active = true, onClick = {})
            PanelButton(id = R.drawable.screen_lock, tooltip = stringResource(R.string.menu_tooltip_lock), onClick = {})
            PanelButton(id = R.drawable.light_mode, tooltip = stringResource(R.string.menu_tooltip_brighton), onClick = {})
            PanelButton(id = R.drawable.caption_icon, tooltip = stringResource(R.string.menu_tooltip_cc), active = true, onClick = {})
        }
    }
}