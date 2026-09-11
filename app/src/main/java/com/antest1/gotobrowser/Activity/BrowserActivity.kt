package com.antest1.gotobrowser.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Rational
import android.view.*
import android.webkit.SslErrorHandler
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.annotation.NonNull
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.ViewModelProvider
import com.antest1.gotobrowser.Browser.WebViewL
import com.antest1.gotobrowser.Browser.WebViewManager
import com.antest1.gotobrowser.BuildConfig
import com.antest1.gotobrowser.Constants.*
import com.antest1.gotobrowser.Helpers.BackPressCloseHandler
import com.antest1.gotobrowser.Helpers.KcUtils
import com.antest1.gotobrowser.Notification.ScreenshotNotification
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.ui.component.VerticalFloatingToolbar
import com.antest1.gotobrowser.ui.theme.GotobrowserTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class BrowserActivity : ComponentActivity() {
    companion object {
        val FOREGROUND_ACTION = "${BuildConfig.APPLICATION_ID}.foreground"

        @JvmStatic
        fun setSubtitleTextView(context: Context, tv: TextView, size: Int) {
            val colorBlack = ContextCompat.getColor(context, R.color.black)
            tv.textSize = size.toFloat()
            if (size >= 24) {
                tv.setShadowLayer(3f, 3f, 3f, colorBlack)
            } else {
                tv.setShadowLayer(2f, 2f, 2f, colorBlack)
            }
        }
    }

    private var uiOption: Int = 0
    private lateinit var viewModel: BrowserViewModel
    private var manager: WebViewManager? = null
    private var mContentView: WebViewL? = null
    private lateinit var screenshotNotification: ScreenshotNotification
    private lateinit var backPressCloseHandler: BackPressCloseHandler

    private var isInPictureInPictureMode: Boolean = false
    private val errorText = mutableStateOf("")
    private val subtitleTextValue = mutableStateOf("")
    private val closeButtonVisible = mutableStateOf(false)

    @SuppressLint("SourceLockedOrientationActivity", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(BrowserViewModel::class.java)
        screenshotNotification = ScreenshotNotification(this)
        backPressCloseHandler = BackPressCloseHandler(this, true)

        uiOption = window.decorView.systemUiVisibility
        sendIsFrontChanged(true)

        val intent = getIntent()
        viewModel.setKcBrowserMode(WebViewManager.OPEN_KANCOLLE == intent.action)

        if (viewModel.sharedPref.getBoolean(PREF_LANDSCAPE, true)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        }

        manager = WebViewManager(this)
        manager?.setDataDirectorySuffix()

        setContent {
            GotobrowserTheme {
                val toolbarVisible = remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    BrowserScreenContent(
                        viewModel = viewModel,
                        errorText = errorText,
                        subtitleTextValue = subtitleTextValue,
                        closeButtonVisible = closeButtonVisible,
                        manager = manager,
                        onViewCreated = { mContentView = it },
                        intent = intent,
                        activity = this@BrowserActivity,
                        onBackgroundTap = { toolbarVisible.value = !toolbarVisible.value }
                    )

                    VerticalFloatingToolbar(
                        visible = toolbarVisible.value,
                        onVisibleChange = { toolbarVisible.value = it }
                    ) {
                        val isMute by viewModel.isMuteMode.observeAsState(false)
                        val isCapture by viewModel.isCaptureMode.observeAsState(false)
                        val isLock by viewModel.isLockMode.observeAsState(false)
                        val isKeep by viewModel.isKeepMode.observeAsState(false)
                        val isCaption by viewModel.isCaptionMode.observeAsState(false)

                        PanelButton(id = R.drawable.refresh_icon, onClick = { showRefreshDialog() })
                        PanelButton(id = R.drawable.volume_off, active = isMute, onClick = { viewModel.toggleMuteMode() })
                        PanelButton(id = R.drawable.camera_icon, active = isCapture, onClick = {
                            if (!checkStoragePermissionGrated()) showStoragePermissionDialog()
                            viewModel.toggleCaptureMode()
                        })
                        PanelButton(id = R.drawable.screen_lock, active = isLock, onClick = { viewModel.toggleLockMode(); updateOrientationLock() })
                        PanelButton(id = R.drawable.light_mode, active = isKeep, onClick = { viewModel.toggleKeepMode() })
                        PanelButton(id = R.drawable.caption_icon, active = isCaption, onClick = { viewModel.toggleCaptionMode() })
                        if (viewModel.k3dPatcher.isPatcherEnabled) {
                            PanelButton(id = R.drawable.kantai3d_icon, onClick = { showKantai3dDialog(this@BrowserActivity, viewModel.k3dPatcher) })
                        }
                        PanelButton(id = R.drawable.exit_to_app, onClick = { showLogoutDialog() })

                        Spacer(modifier = Modifier.height(4.dp))
                        IconButton(onClick = { toolbarVisible.value = false }) {
                            Icon(painterResource(id = R.drawable.close_icon), "Close", tint = Color.White)
                        }
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })

        setupSmoothPipAnimation()
    }

    fun isKcMode(): Boolean = viewModel.isKcBrowserMode
    fun isMuteMode(): Boolean = java.lang.Boolean.TRUE == viewModel.isMuteMode.value
    fun isCaptionAvailable(): Boolean = java.lang.Boolean.TRUE == viewModel.isCaptionMode.value
    fun isSubtitleAvailable(): Boolean = viewModel.isSubtitleLoaded()
    fun setStartedFlag() { viewModel.setStartedFlag(true) }

    fun setErrorText(text: String) { errorText.value = text }
    fun setSubtitleText(text: String) { subtitleTextValue.value = text }
    fun setCloseButtonVisible(visible: Boolean) { closeButtonVisible.value = visible }

    private fun updateOrientationLock() {
        val isLockMode = java.lang.Boolean.TRUE == viewModel.isLockMode.value
        if (viewModel.sharedPref.getBoolean(PREF_LANDSCAPE, false)) {
            if (isLockMode) {
                val rot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display?.rotation ?: Surface.ROTATION_0
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.rotation
                }
                if (rot == Surface.ROTATION_270) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                } else {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            } else requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        } else {
            if (isLockMode) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            else requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }

    private fun setupSmoothPipAnimation() {
        val pipEnabled = viewModel.sharedPref.getBoolean(PREF_PIP_MODE, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && supportsPiPMode() && pipEnabled) {
            val sourceRectHint = Rect()
            mContentView?.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                if (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom) {
                    mContentView?.getGlobalVisibleRect(sourceRectHint)
                    setPictureInPictureParams(
                        PictureInPictureParams.Builder()
                            .setSeamlessResizeEnabled(false)
                            .setSourceRectHint(sourceRectHint)
                            .setAutoEnterEnabled(true)
                            .setAspectRatio(Rational(1200, 720))
                            .build()
                    )
                }
            }
        }
    }

    fun handleBackPress() {
        if (viewModel.isKcBrowserMode) {
            backPressCloseHandler.handleOnBackPressed()
        } else {
            val intent = Intent(this, EntranceActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        mContentView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        uiOption = mContentView?.systemUiVisibility ?: 0
    }

    override fun onStop() {
        super.onStop()
        mContentView?.let { manager?.runMuteScript(it, true, true) }
        sendIsFrontChanged(false)
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode) {
            viewModel.k3dPatcher.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        mContentView?.resumeTimers()
        sendIsFrontChanged(true)
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
        mContentView?.removeAllViews()
        mContentView?.destroy()
        super.onDestroy()
    }

    fun showWebkitErrorDialog(errorCode: Int, description: String, failingUrl: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(KcUtils.getWebkitErrorCodeText(errorCode))
            .setCancelable(false)
            .setMessage((description + "\n\n" + failingUrl).trim { it <= ' ' })
            .setPositiveButton("Reload") { _, _ -> refreshPageOrFinish() }
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

    private fun refreshPageOrFinish() {
        viewModel.setConnectorInfo(WebViewManager.getDefaultPage(this, viewModel.isKcBrowserMode))
        val info = viewModel.connectorInfo
        if (manager != null && info != null && info.size == 2) {
            mContentView?.let { manager?.refreshPage(it) }
        } else {
            finish()
        }
    }

    fun showRefreshDialog() {
        if (java.lang.Boolean.TRUE == viewModel.isNoRefreshPopupMode.value) {
            refreshPageOrFinish()
        } else {
            mContentView?.pauseTimers()
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.app_name))
                .setCancelable(false)
                .setMessage(getString(R.string.refresh_msg))
                .setPositiveButton(R.string.action_ok) { _, _ -> refreshPageOrFinish() }
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
                if (manager != null) {
                    mContentView?.let { manager?.logoutGame(it) }
                } else {
                    finish()
                }
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

    fun sendIsFrontChanged(is_front: Boolean) {
        val intent = Intent(FOREGROUND_ACTION)
        intent.putExtra("is_front", is_front)
        sendBroadcast(intent)
    }

    fun initPanelKeyboardFromIntent(intent: Intent?) {
        if (intent != null) {
            val options = intent.getStringExtra("options")
            if (options != null && !options.contains(ACTION_SHOWKEYBOARD)) {
                mContentView?.isFocusableInTouchMode = false
                mContentView?.isFocusable = false
                mContentView?.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            }
        }
    }

    private fun supportsPiPMode(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    private fun showKantai3dDialog(context: Context, patcher: com.antest1.gotobrowser.Helpers.K3dPatcher) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.k3d_form, null)
        if (patcher.imageUrl != null) {
            val textView = dialogView.findViewById<TextView>(R.id.kantai3d_msg_text)
            textView.text = String.format(Locale.US, context.getString(if (patcher.isDepthMapLoaded) R.string.msg_kantai3d_loaded else R.string.msg_kantai3d_error), patcher.imageUrl)
        }
        val switchCompat = dialogView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_3d)
        switchCompat.isChecked = patcher.isEffectEnabled
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_mod_kantai3d_enable)
            .setView(dialogView)
            .setPositiveButton(R.string.text_save) { dialog, _ ->
                patcher.isEffectEnabled = switchCompat.isChecked
                dialog.dismiss()
            }
            .setNegativeButton(R.string.text_cancel) { dialog, _ -> dialog.cancel() }
            .show()
    }
}

// Top-level so both the activity's floating toolbar and the IDE preview can use it.
@Composable
fun PanelButton(id: Int, active: Boolean = false, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            painterResource(id = id), null,
            tint = if (active) Color(0xFFFFC400) else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Stateless overlay layer (subtitle, capture, close). Extracted so the real
// overlays can be rendered in an IDE preview without a ViewModel or WebView.
@Composable
fun BrowserOverlayLayer(
    showSubtitle: Boolean,
    subtitleText: String,
    subtitleVisible: Boolean,
    isCapture: Boolean,
    closeButtonVisible: Boolean,
    onSubtitleTap: () -> Unit,
    onCaptureClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Subtitle Overlay
        if (showSubtitle && subtitleVisible) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp), contentAlignment = Alignment.BottomCenter) {
                Text(
                    text = subtitleText.ifEmpty { stringResource(id = R.string.subtitle_default) },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onSubtitleTap() }
                )
            }
        }

        // Camera Button (Square)
        if (isCapture) {
            IconButton(
                onClick = onCaptureClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(24.dp).size(64.dp)
                    .background(Color.Black.copy(alpha = 0.5f)).border(2.dp, Color.White)
            ) {
                Icon(painterResource(id = R.drawable.capture_icon), "Capture", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        // DMM Close Button
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
        // WebView with proper "Scale to Fit" 15:9
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val ratio = 1200f / 720f
            val containerWidth = maxWidth.value
            val containerHeight = maxHeight.value

            val finalWidth: androidx.compose.ui.unit.Dp
            val finalHeight: androidx.compose.ui.unit.Dp

            if (containerWidth / containerHeight > ratio) {
                // Screen is wider than 15:9
                finalHeight = maxHeight
                finalWidth = maxHeight * ratio
            } else {
                // Screen is narrower than 15:9
                finalWidth = maxWidth
                finalHeight = maxWidth / ratio
            }

            AndroidView(
                factory = { ctx ->
                    WebViewL(ctx).apply {
                        onViewCreated(this)
                        addJavascriptInterface(viewModel.k3dPatcher, "kantai3dInterface")
                        manager?.setHardwareAcceleratedFlag()
                        activity.initPanelKeyboardFromIntent(intent)
                        // Initial setup...
                        viewModel.setConnectorInfo(WebViewManager.getDefaultPage(activity, viewModel.isKcBrowserMode))
                        val info = viewModel.connectorInfo
                        if (info != null && info.size == 2) {
                            manager?.setWebViewSettings(this)
                            WebViewManager.enableBrowserCookie(this)
                            manager?.setWebViewClient(activity, this)
                            manager?.setPopupView(this)
                            manager?.openPage(this, info, viewModel.isKcBrowserMode)
                        }
                    }
                },
                modifier = Modifier
                    .size(width = finalWidth, height = finalHeight)
                    .background(Color.Black)
                    .clickable(enabled = false) { } // Prevent clicks on WebView from toggling panel
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
                Box(modifier = Modifier.matchParentSize().background(Color.White).alpha(0.5f))
                LaunchedEffect(Unit) {
                    Handler().postDelayed({ showFlash.value = false }, 250)
                }
            }
        }

        BrowserOverlayLayer(
            showSubtitle = viewModel.isKcBrowserMode && isCaption,
            subtitleText = currentSubtitle,
            subtitleVisible = subtitleVisible.value,
            isCapture = isCapture,
            closeButtonVisible = closeButtonVisible.value,
            onSubtitleTap = { subtitleVisible.value = false },
            onCaptureClick = {
                manager?.captureGameScreen(activity.findViewById(android.R.id.content)) // Or use view reference
                showFlash.value = true
            },
            onCloseClick = { activity.finish() }
        )
    }
}

@Preview(name = "Browser - Overlays + Toolbar", showBackground = true, widthDp = 800, heightDp = 480)
@Composable
fun BrowserScreenPreview() {
    GotobrowserTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Placeholder for the WebView area (an actual WebView cannot render in Preview).
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
                isCapture = true,
                closeButtonVisible = false,
                onSubtitleTap = {},
                onCaptureClick = {},
                onCloseClick = {}
            )

            VerticalFloatingToolbar(visible = true, onVisibleChange = {}) {
                PanelButton(id = R.drawable.refresh_icon, onClick = {})
                PanelButton(id = R.drawable.volume_off, active = true, onClick = {})
                PanelButton(id = R.drawable.camera_icon, active = true, onClick = {})
                PanelButton(id = R.drawable.screen_lock, onClick = {})
                PanelButton(id = R.drawable.light_mode, onClick = {})
                PanelButton(id = R.drawable.caption_icon, active = true, onClick = {})
                PanelButton(id = R.drawable.exit_to_app, onClick = {})
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(onClick = {}) {
                    Icon(painterResource(id = R.drawable.close_icon), "Close", tint = Color.White)
                }
            }
        }
    }
}
