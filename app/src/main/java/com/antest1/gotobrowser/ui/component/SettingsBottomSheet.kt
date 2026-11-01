package com.antest1.gotobrowser.ui.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antest1.gotobrowser.Activity.SettingsViewModel
import com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT
import com.antest1.gotobrowser.Constants.PREF_ALTER_ENDPOINT
import com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET
import com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD
import com.antest1.gotobrowser.Constants.PREF_BROADCAST
import com.antest1.gotobrowser.Constants.CONN_DMM
import com.antest1.gotobrowser.Constants.CONN_KANMOE
import com.antest1.gotobrowser.Constants.CONN_OOI
import com.antest1.gotobrowser.Constants.PREF_CONNECTOR
import com.antest1.gotobrowser.Constants.PREF_CURSOR_MODE
import com.antest1.gotobrowser.Constants.PREF_DEVTOOLS_DEBUG
import com.antest1.gotobrowser.Constants.PREF_DISABLE_REFRESH_DIALOG
import com.antest1.gotobrowser.Constants.PREF_DMM_ID
import com.antest1.gotobrowser.Constants.PREF_DMM_PASS
import com.antest1.gotobrowser.Constants.PREF_DOWNLOAD_RETRY
import com.antest1.gotobrowser.Constants.PREF_FONT_PREFETCH
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
import com.antest1.gotobrowser.Constants.PREF_SILENT
import com.antest1.gotobrowser.Constants.PREF_SUBTITLE_FONTSIZE
import com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE
import com.antest1.gotobrowser.Constants.PREF_TP_DISCLAIMED
import com.antest1.gotobrowser.Constants.PREF_USE_EXTCACHE
import com.antest1.gotobrowser.Constants.PREF_LATEST_URL
import com.antest1.gotobrowser.Constants.URL_LIST
import com.antest1.gotobrowser.Helpers.KcUtils
import com.antest1.gotobrowser.R
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private const val MIN_SUBTITLE_FONTSIZE = 12
private const val MAX_SUBTITLE_FONTSIZE = 24

private const val GITHUB_KANTAI3D = "https://github.com/laplamgor/kantai3d"
private const val GITHUB_GOTOBROWSER = "https://github.com/antest1/GotoBrowser/"

private data class ListOption(val value: String, val title: String, val summary: String? = null)

enum class SettingsScreen {
    MAIN,
    DISPLAY,
    CONTROL,
    DOWNLOAD,
    SUBTITLE,
    CONNECTION_LOGIN,
    MODS,
    APP_INFO
}

/**
 * Modal bottom sheet hosting the settings list on top of the browser.
 *
 * The browser (WebView) keeps running behind the sheet; this does not navigate
 * to a different activity. Every preference mutation is surfaced through
 * [onSettingChanged] with the changed preference key, leaving the host to decide
 * whether the change can be applied live or needs a WebView reload.
 *
 * @param activity host activity, used for actions that need a window (dialogs,
 *        snackbars). The sheet renders in its own window, so it cannot be
 *        resolved from [LocalContext] and must be supplied by the host.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    viewModel: SettingsViewModel?,
    activity: android.app.Activity?,
    onDismissRequest: () -> Unit,
    onSettingChanged: (String) -> Unit,
    onLogoutRequest: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    var currentScreen by remember { mutableStateOf(SettingsScreen.MAIN) }

    LaunchedEffect(Unit) {
        activity?.window?.let { window ->
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 4.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.height(48.withDp())) {
                if (currentScreen != SettingsScreen.MAIN) {
                    IconButton(
                        onClick = { currentScreen = SettingsScreen.MAIN }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.padding(start = 12.dp))
                }
            }
            Text(
                text = when (currentScreen) {
                    SettingsScreen.MAIN -> stringResource(R.string.settings_menu_tooltip)
                    SettingsScreen.DISPLAY -> stringResource(R.string.settings_display_label)
                    SettingsScreen.CONTROL -> stringResource(R.string.settings_control_label)
                    SettingsScreen.DOWNLOAD -> stringResource(R.string.settings_download_label)
                    SettingsScreen.SUBTITLE -> stringResource(R.string.settings_subtitle_label)
                    SettingsScreen.CONNECTION_LOGIN -> stringResource(R.string.settings_connection_login_label)
                    SettingsScreen.MODS -> stringResource(R.string.settings_mod_label)
                    SettingsScreen.APP_INFO -> stringResource(R.string.settings_appinfo_label)
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val isGoingBack = targetState == SettingsScreen.MAIN
                if (isGoingBack) {
                    (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(animationSpec = tween(300)))
                } else {
                    (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(animationSpec = tween(300)))
                }.using(
                    SizeTransform(
                        clip = false,
                        sizeAnimationSpec = { _, _ -> keyframes { durationMillis = 300 } }
                    )
                )
            },
            label = "SettingsScreenTransition"
        ) { screen ->
            Box(modifier = Modifier.fillMaxWidth()) {
                SettingsContent(
                    viewModel = viewModel,
                    activity = activity,
                    snackbarHostState = snackbarHostState,
                    currentScreen = screen,
                    onScreenChanged = { currentScreen = it },
                    modifier = Modifier.fillMaxWidth(),
                    onDismissRequest = onDismissRequest,
                    onSettingChanged = onSettingChanged,
                    onLogoutRequest = onLogoutRequest
                )
                // Rendered inside the sheet so its messages appear above the
                // sheet's own window rather than behind it. Padding keeps it
                // clear of the last settings row.
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }
        }
    }
}

private fun Int.withDp() = this.dp

/**
 * The full settings list, decoupled from any container, so it can be hosted by
 * the browser's bottom sheet (or any other surface later on).
 *
 * @param onSettingChanged invoked with the preference key whenever a user action
 *        mutates a preference. The host decides if it can be applied live or
 *        requires a WebView reload.
 */
@Composable
fun SettingsContent(
    viewModel: SettingsViewModel?,
    activity: android.app.Activity?,
    snackbarHostState: SnackbarHostState,
    currentScreen: SettingsScreen,
    onScreenChanged: (SettingsScreen) -> Unit,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    onSettingChanged: (String) -> Unit = {},
    onLogoutRequest: () -> Unit = {}
) {
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current || viewModel == null

    val patchSummary = if (isPreview) "checking updates..." else viewModel.patchUpdateSummary.observeAsState("checking updates...").value
    val patchEnabled = if (isPreview) false else viewModel.patchUpdateEnabled.observeAsState(false).value
    val subtitleSummary = if (isPreview) "checking updates..." else viewModel.subtitleUpdateSummary.observeAsState("checking updates...").value
    val subtitleEnabled = if (isPreview) false else viewModel.subtitleUpdateEnabled.observeAsState(false).value
    val patchAboutTitle = if (isPreview) R.string.settings_mod_kantaien_about else viewModel.patchAboutTitleRes.observeAsState(R.string.settings_mod_kantaien_about).value
    val patchAboutUrl = if (isPreview) "" else viewModel.patchAboutUrl.observeAsState("").value

    if (!isPreview) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            viewModel.refreshSubtitleDescription()
            viewModel.refreshKccpDependentRows(null)
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        when (currentScreen) {
            SettingsScreen.MAIN -> {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_connection_login_label)) },
                    modifier = Modifier.clickable { onScreenChanged(SettingsScreen.CONNECTION_LOGIN) }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_display_label)) },
                    modifier = Modifier.clickable { onScreenChanged(SettingsScreen.DISPLAY) }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_control_label)) },
                    modifier = Modifier.clickable { onScreenChanged(SettingsScreen.CONTROL) }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_download_label)) },
                    modifier = Modifier.clickable { onScreenChanged(SettingsScreen.DOWNLOAD) }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_subtitle_label)) },
                    modifier = Modifier.clickable { onScreenChanged(SettingsScreen.SUBTITLE) }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_mod_label)) },
                    modifier = Modifier.clickable { onScreenChanged(SettingsScreen.MODS) }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_appinfo_label)) },
                    modifier = Modifier.clickable { onScreenChanged(SettingsScreen.APP_INFO) }
                )
                HorizontalDivider()
            }
            SettingsScreen.DISPLAY -> {
                DisplaySection(viewModel, onSettingChanged)
            }
            SettingsScreen.CONTROL -> {
                ControlSection(viewModel, onSettingChanged)
            }
            SettingsScreen.DOWNLOAD -> {
                DownloadSection(viewModel, onSettingChanged)
            }
            SettingsScreen.SUBTITLE -> {
                SubtitleSection(viewModel, subtitleSummary, subtitleEnabled, onSettingChanged)
            }
            SettingsScreen.CONNECTION_LOGIN -> {
                ConnectionLoginSection(viewModel, snackbarHostState, onSettingChanged, onLogoutRequest)
            }
            SettingsScreen.MODS -> {
                ModsSection(
                    viewModel,
                    patchTitle = patchAboutTitle,
                    patchUrl = patchAboutUrl,
                    patchSummary = patchSummary,
                    patchEnabledState = patchEnabled,
                    onSettingChanged = onSettingChanged
                )
            }
            SettingsScreen.APP_INFO -> {
                AppInfoSection(viewModel, activity, snackbarHostState, onDismissRequest, onSettingChanged)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------------------
// Sections
// ---------------------------------------------------------------------------

@Composable
private fun DisplaySection(viewModel: SettingsViewModel?, onSettingChanged: (String) -> Unit) {
    SwitchRow(viewModel, PREF_LANDSCAPE, R.string.mode_landscape, onSettingChanged = onSettingChanged)
    SwitchRow(viewModel, PREF_ADJUSTMENT, R.string.mode_adjustment, R.string.settings_recommended_summary, defaultValue = true, onSettingChanged = onSettingChanged)
    SwitchRow(viewModel, PREF_PIP_MODE, R.string.browser_enablepipmode, onSettingChanged = onSettingChanged)
    SwitchRow(viewModel, PREF_MULTIWIN_MARGIN, R.string.settings_mw_margin, onSettingChanged = onSettingChanged)
    SwitchRow(viewModel, PREF_LEGACY_RENDERER, R.string.settings_legacy_renderer_enable,
        R.string.settings_legacy_renderer_summary, onChanged = { onLegacyRendererChanged(viewModel) }, onSettingChanged = onSettingChanged)
}

@Composable
private fun ControlSection(viewModel: SettingsViewModel?, onSettingChanged: (String) -> Unit) {
    SwitchRow(viewModel, PREF_KEYBOARD, R.string.mode_enable_keyboard, defaultValue = true, onSettingChanged = onSettingChanged)
    ListRow(viewModel, PREF_CURSOR_MODE, R.string.setting_cursor_mode, cursorModeOptions(), onSettingChanged = onSettingChanged)
    SwitchRow(viewModel, PREF_DISABLE_REFRESH_DIALOG, R.string.browser_disable_refresh_dialog, onSettingChanged = onSettingChanged)
    SwitchRow(viewModel, PREF_PANELSTART, R.string.mode_show_panel, defaultValue = true, onSettingChanged = onSettingChanged)
}

@Composable
private fun DownloadSection(viewModel: SettingsViewModel?, onSettingChanged: (String) -> Unit) {
    val context = LocalContext.current
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current || viewModel == null
    SwitchRow(viewModel, PREF_USE_EXTCACHE, R.string.settings_use_external_dir, R.string.settings_recommended_summary,
        onChanged = { if (!isPreview) viewModel.onExternalCacheChanged() }, onSettingChanged = onSettingChanged)
    SwitchRow(viewModel, PREF_FONT_PREFETCH, R.string.browser_fontprefetch, R.string.settings_recommended_summary, defaultValue = true, onSettingChanged = onSettingChanged)
    SwitchRow(viewModel, PREF_DOWNLOAD_RETRY, R.string.settings_retry_enable, R.string.settings_retry_summary,
        onSettingChanged = onSettingChanged)
    ClickRow(
        title = R.string.cache_clear_text,
        summary = R.string.clearcache_msg,
        onClick = {
            if (!isPreview) {
                viewModel.clearBrowserCache()
                KcUtils.showToast(context.applicationContext, R.string.cache_cleared_toast)
            }
        }
    )
}

@Composable
private fun SubtitleSection(viewModel: SettingsViewModel?, subtitleSummary: String, subtitleEnabled: Boolean, onSettingChanged: (String) -> Unit) {
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current || viewModel == null
    ListRow(viewModel, PREF_SUBTITLE_LOCALE, R.string.settings_subtitle_language, subtitleLocaleOptions(),
        onSelected = { if (!isPreview) viewModel.onSubtitleLocaleChanged(it); true },
        onSettingChanged = onSettingChanged)

    var showSubtitleSizeDialog by remember { mutableStateOf(false) }
    val subtitleSize = remember { mutableIntStateOf(if (isPreview) 16 else viewModel.subtitleFontSize) }
    ClickRow(
        title = R.string.settings_subtitle_fontsize,
        summaryText = subtitleSize.intValue.toString(),
        onClick = { showSubtitleSizeDialog = true }
    )
    if (showSubtitleSizeDialog) {
        SubtitleSizeDialog(
            initialSize = subtitleSize.intValue,
            onSave = { newSize ->
                if (!isPreview) {
                    viewModel.setInt(PREF_SUBTITLE_FONTSIZE, newSize)
                }
                subtitleSize.intValue = newSize
                showSubtitleSizeDialog = false
                onSettingChanged(PREF_SUBTITLE_FONTSIZE)
            },
            onDismiss = { showSubtitleSizeDialog = false }
        )
    }

    ClickRow(
        title = R.string.settings_subtitle_download,
        summaryText = subtitleSummary,
        enabled = subtitleEnabled,
        onClick = { if (!isPreview) viewModel.downloadSubtitleUpdate() }
    )
}

@Composable
private fun ConnectionLoginSection(
    viewModel: SettingsViewModel?,
    snackbarHostState: SnackbarHostState,
    onSettingChanged: (String) -> Unit,
    onLogoutRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current || viewModel == null
    var showLoginForm by remember { mutableStateOf(false) }

    // --- Login ---------------------------------------------------------------
    SectionHeader(R.string.settings_login_section_header)

    ClickRow(
        title = R.string.autocomplete_title,
        summary = R.string.autocomplete_msg,
        onClick = { showLoginForm = true }
    )

    if (showLoginForm) {
        LoginFormDialog(
            initialId = if (isPreview) "" else viewModel.getString(PREF_DMM_ID, ""),
            initialPassword = if (isPreview) "" else viewModel.getString(PREF_DMM_PASS, ""),
            onSave = { loginId, loginPassword ->
                if (!isPreview) {
                    viewModel.setString(PREF_DMM_ID, loginId)
                    viewModel.setString(PREF_DMM_PASS, loginPassword)
                }
                showLoginForm = false
                onSettingChanged(PREF_DMM_ID)
            },
            onDismiss = { showLoginForm = false }
        )
    }

    ClickRow(
        title = R.string.menu_tooltip_logout,
        onClick = onLogoutRequest
    )

    // Shown when the user switches to an unofficial connector for the first time.
    var showDisclaimer by remember { mutableStateOf(false) }
    // Shown when the user disables broadcast while Kcanotify is installed.
    var showKcanotifyWarning by remember { mutableStateOf(false) }

    var connector by remember { mutableStateOf(if (isPreview) CONN_DMM else viewModel.getString(PREF_CONNECTOR, CONN_DMM)) }

    ListRow(
        viewModel, PREF_CONNECTOR, R.string.select_server, connectorOptions(),
        onSelected = { value ->
            connector = value
            if (!isPreview) {
                // Point the session at the new connector's start page and let the
                // user know which URL that resolves to.
                val index = connectorOptions().indexOfFirst { it.value == value }
                if (index in URL_LIST.indices) {
                    viewModel.setString(PREF_LATEST_URL, URL_LIST[index])
                    KcUtils.showToast(context.applicationContext, URL_LIST[index])
                }
                // Unofficial connectors serve resources that differ from DMM's,
                // so make sure the user acknowledges that once.
                if (value != CONN_DMM && !viewModel.getBoolean(PREF_TP_DISCLAIMED, false)) {
                    showDisclaimer = true
                }
            }
            true
        },
        onSettingChanged = onSettingChanged
    )

    if (showDisclaimer) {
        ThirdPartyConnectorDialog(
            onAccept = {
                if (!isPreview) viewModel.setBoolean(PREF_TP_DISCLAIMED, true)
                showDisclaimer = false
            },
            onDismiss = { showDisclaimer = false }
        )
    }

    if (showKcanotifyWarning) {
        KcanotifyBroadcastDialog(
            onAccept = {
                if (!isPreview) viewModel.setBoolean(PREF_BROADCAST, true)
                showKcanotifyWarning = false
            },
            onDismiss = { showKcanotifyWarning = false }
        )
    }

    // The silent mode only works with the official DMM connector.
    SwitchRow(viewModel, PREF_SILENT, R.string.mode_silent, enabled = connector == CONN_DMM, onSettingChanged = onSettingChanged)
    SwitchRow(viewModel, PREF_BROADCAST, R.string.mode_broadcast, R.string.mode_broadcast_summary, defaultValue = true,
        onChanged = {
            if (!isPreview && !viewModel.getBoolean(PREF_BROADCAST, true)
                && KcUtils.isKcanotifyInstalled(context.applicationContext)) {
                showKcanotifyWarning = true
            }
        },
        onSettingChanged = onSettingChanged)

    // --- Gadget Block Bypass -------------------------------------------------
    // The bypass only exists to make the login above succeed on blocked IPs, so
    // it lives in the same screen, separated by a header instead of another
    // navigation level.
    SectionHeader(R.string.settings_bypass_section_header)

    SwitchRow(viewModel, PREF_ALTER_GADGET, R.string.connection_use_alter, R.string.connection_use_alter_summary,
        onSettingChanged = onSettingChanged)

    val alterGadgetEnabled = if (isPreview) false else viewModel.getBoolean(PREF_ALTER_GADGET, false)
    ListRow(
        viewModel, PREF_ALTER_METHOD, R.string.setting_alter_method, alterMethodOptions(),
        enabled = alterGadgetEnabled,
        onSelected = { value ->
            if (isPreview) {
                true
            } else {
                if (viewModel.onAlterMethodSelected(value)) {
                    true
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("PROXY_OVERRIDE not supported, use other option")
                    }
                    false
                }
            }
        },
        onSettingChanged = onSettingChanged
    )

    var endpoint by remember { mutableStateOf(if (isPreview) "http://localhost" else viewModel.alterEndpoint) }
    var showEndpointDialog by remember { mutableStateOf(false) }
    ClickRow(
        title = R.string.setting_alter_endpoint,
        summaryText = endpoint,
        enabled = alterGadgetEnabled,
        onClick = { showEndpointDialog = true }
    )
    if (showEndpointDialog) {
        EditTextDialog(
            title = R.string.setting_alter_endpoint,
            initialValue = endpoint,
            onSave = { value ->
                if (!isPreview) {
                    viewModel.onAlterEndpointChanged(value)
                    endpoint = viewModel.alterEndpoint
                } else {
                    endpoint = value
                }
                showEndpointDialog = false
                onSettingChanged(PREF_ALTER_ENDPOINT)
            },
            onDismiss = { showEndpointDialog = false }
        )
    }
}

@Composable
private fun ModsSection(
    viewModel: SettingsViewModel?,
    patchTitle: Int,
    patchUrl: String,
    patchSummary: String,
    patchEnabledState: Boolean,
    onSettingChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current || viewModel == null

    // --- Group 1: General Mods -----------------------------------------------
    SectionHeader(R.string.settings_mod_group_general)
    SwitchRow(viewModel, PREF_MOD_FPS, R.string.settings_mod_fps_enable, R.string.settings_mod_fps_summary,
        onSettingChanged = onSettingChanged)
    SwitchRow(viewModel, PREF_MOD_CRIT, R.string.settings_mod_crit_enable, R.string.settings_mod_crit_summary,
        onSettingChanged = onSettingChanged)

    // --- Group 2: Kantai3D Mod -----------------------------------------------
    SectionHeader(R.string.settings_mod_group_kantai3d)
    val kantai3dEnabled = if (isPreview) false else viewModel.isKantai3dEnabled
    SwitchRow(viewModel, PREF_MOD_KANTAI3D, R.string.settings_mod_kantai3d_enable,
        R.string.settings_mod_kantai3d_summary, enabled = kantai3dEnabled, onSettingChanged = onSettingChanged)
    ClickRow(
        title = R.string.settings_mod_kantai3d_about,
        summaryText = GITHUB_KANTAI3D,
        onClick = { openUrl(context, GITHUB_KANTAI3D) }
    )

    // --- Group 3: English & Language Patches --------------------------------
    SectionHeader(R.string.settings_mod_group_kccp)
    SwitchRow(viewModel, PREF_MOD_KCCP_LANG_PATCH, R.string.settings_mod_kccp, R.string.settings_mod_kccp_summary,
        onChanged = { if (!isPreview) viewModel.onKccpPatchChanged() }, onSettingChanged = onSettingChanged)

    val patchEnabled = if (isPreview) false else viewModel.isKccpPatchEnabled()
    ListRow(
        viewModel, PREF_MOD_KCCP_LANG_PATCH_NAME, R.string.settings_mod_kccp_patch_name, kccpLanguageOptions(),
        enabled = patchEnabled,
        summaryProvider = { if (isPreview) "" else viewModel.getKccpPatchSummary(it) },
        onSelected = { if (!isPreview) viewModel.onPatchLanguageChanged(it); true },
        onSettingChanged = onSettingChanged
    )

    ClickRow(
        title = R.string.settings_mod_kantaien_download,
        summaryText = patchSummary,
        enabled = patchEnabled && patchEnabledState,
        onClick = { if (!isPreview) viewModel.requestPatchUpdate() }
    )
    ClickRow(
        title = R.string.settings_mod_kantaien_delete,
        summary = R.string.settings_mod_kantaien_delete_summary,
        enabled = patchEnabled,
        onClick = { if (!isPreview) viewModel.requestPatchDelete() }
    )
    ClickRow(
        title = patchTitle,
        summaryText = patchUrl,
        onClick = { openUrl(context, patchUrl) }
    )
}

@Composable
private fun AppInfoSection(
    viewModel: SettingsViewModel?,
    activity: android.app.Activity?,
    snackbarHostState: SnackbarHostState,
    onDismissRequest: () -> Unit,
    onSettingChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current || viewModel == null
    val scope = rememberCoroutineScope()
    ClickRow(
        title = R.string.settings_version_label,
        summaryText = if (isPreview) "3.0-rev9" else viewModel.appVersion,
        onClick = {},
        enabled = false
    )
    ClickRow(
        title = R.string.settings_version_check,
        onClick = {
            if (isPreview) return@ClickRow
            if (activity == null) {
                KcUtils.showToast(context.applicationContext, "Unable to check update")
                return@ClickRow
            }
            // Show the result in the sheet's own snackbar: the sheet renders in
            // a separate window, so the activity's snackbar would be hidden
            // behind it.
            viewModel.checkAppUpdate(activity) { message ->
                if (message == null) {
                    // Download chosen; the user is leaving for the browser.
                    onDismissRequest()
                } else {
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        }
    )
    ClickRow(
        title = R.string.settings_source_code,
        summaryText = GITHUB_GOTOBROWSER,
        onClick = { openUrl(context, GITHUB_GOTOBROWSER) }
    )
    ClickRow(
        title = R.string.app_name,
        summaryText = String.format(
            Locale.US,
            stringResource(id = R.string.copyright_format),
            Calendar.getInstance().get(Calendar.YEAR)
        ),
        onClick = {},
        enabled = false
    )
    SwitchRow(viewModel, PREF_DEVTOOLS_DEBUG, R.string.setting_devtools_enable, R.string.setting_devtools_description,
        onSettingChanged = onSettingChanged)
}

// ---------------------------------------------------------------------------
// Reusable rows
// ---------------------------------------------------------------------------

/**
 * Non-interactive label used to group rows inside a single settings screen, so
 * related settings can stay together without adding another navigation level.
 */
@Composable
private fun SectionHeader(titleRes: Int) {
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    )
}

/** Preview-safe wrapper so rows can call the view model hook directly. */
private fun onLegacyRendererChanged(viewModel: SettingsViewModel?) {
    viewModel?.onLegacyRendererChanged()
}

@Composable
private fun SwitchRow(
    viewModel: SettingsViewModel?,
    key: String,
    titleRes: Int,
    summaryRes: Int? = null,
    defaultValue: Boolean = false,
    enabled: Boolean = true,
    onChanged: () -> Unit = {},
    onSettingChanged: (String) -> Unit = {}
) {
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current || viewModel == null
    val checked = remember { mutableStateOf(if (isPreview) defaultValue else viewModel.getBoolean(key, defaultValue)) }
    ListItem(
        headlineContent = { Text(stringResource(titleRes)) },
        supportingContent = summaryRes?.let { { Text(stringResource(it)) } },
        trailingContent = {
            Switch(
                checked = checked.value,
                enabled = enabled,
                onCheckedChange = {
                    checked.value = it
                    if (!isPreview) {
                        viewModel.setBoolean(key, it)
                    }
                    onChanged()
                    onSettingChanged(key)
                }
            )
        },
        modifier = Modifier.clickable(enabled = enabled) {
            val newValue = !checked.value
            checked.value = newValue
            if (!isPreview) {
                viewModel.setBoolean(key, newValue)
            }
            onChanged()
            onSettingChanged(key)
        }
    )
    HorizontalDivider()
}

@Composable
private fun ClickRow(
    title: Int,
    summary: Int? = null,
    summaryText: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ListItem(
        headlineContent = { Text(stringResource(title), color = textColor) },
        supportingContent = when {
            summaryText != null -> { { Text(summaryText, color = textColor) } }
            summary != null -> { { Text(stringResource(summary), color = textColor) } }
            else -> null
        },
        modifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier
    )
    HorizontalDivider()
}

@Composable
private fun ListRow(
    viewModel: SettingsViewModel?,
    key: String,
    titleRes: Int,
    options: List<ListOption>,
    enabled: Boolean = true,
    summaryProvider: (String) -> String = { value ->
        options.firstOrNull { it.value == value }?.summary
            ?: options.firstOrNull { it.value == value }?.title ?: value
    },
    onSelected: (String) -> Boolean = { true },
    onSettingChanged: (String) -> Unit = {}
) {
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current || viewModel == null
    var selected by remember { mutableStateOf(if (isPreview) options.first().value else viewModel.getString(key, options.first().value)) }
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(stringResource(titleRes)) },
        supportingContent = { Text(summaryProvider(selected)) },
        modifier = if (enabled) Modifier.clickable { showDialog = true } else Modifier
    )
    HorizontalDivider()

    if (showDialog) {
        ListOptionDialog(
            title = stringResource(titleRes),
            options = options,
            selected = selected,
            onSelect = { value ->
                if (onSelected(value)) {
                    selected = value
                    if (!isPreview) {
                        viewModel.setString(key, value)
                    }
                    onSettingChanged(key)
                }
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun ListOptionDialog(
    title: String,
    options: List<ListOption>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option.value == selected, onClick = { onSelect(option.value) })
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = option.value == selected, onClick = { onSelect(option.value) })
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(option.title)
                            option.summary?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.text_cancel)) }
        }
    )
}

@Composable
private fun EditTextDialog(
    title: Int,
    initialValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) { Text(stringResource(R.string.text_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.text_cancel)) }
        }
    )
}

@Composable
private fun SubtitleSizeDialog(
    initialSize: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var size by remember { mutableFloatStateOf(initialSize.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.settings_subtitle_fontsize)) },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.subtitle_default),
                    fontSize = size.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Slider(
                    value = size,
                    onValueChange = { size = it },
                    valueRange = MIN_SUBTITLE_FONTSIZE.toFloat()..MAX_SUBTITLE_FONTSIZE.toFloat(),
                    steps = (MAX_SUBTITLE_FONTSIZE - MIN_SUBTITLE_FONTSIZE) - 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = MIN_SUBTITLE_FONTSIZE.toString())
                    Text(text = MAX_SUBTITLE_FONTSIZE.toString())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(size.roundToInt()) }) {
                Text(text = stringResource(id = R.string.text_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.text_cancel))
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * One-time notice shown when the user switches away from the official DMM
 * connector, since other connectors may serve modified game resources.
 */
@Composable
private fun ThirdPartyConnectorDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disclaimer") },
        text = { Text(stringResource(id = R.string.thirdpartyconnector_msg)) },
        confirmButton = {
            TextButton(onClick = onAccept) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/**
 * Shown when the user turns broadcast mode off while Kcanotify is installed,
 * because Kcanotify needs it to follow the game state.
 */
@Composable
private fun KcanotifyBroadcastDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.kcanotify_broadcast_dialog_title)) },
        text = {
            Text(
                String.format(
                    Locale.US,
                    stringResource(id = R.string.kcanotify_broadcast_dialog_message),
                    stringResource(id = R.string.mode_broadcast),
                    stringResource(id = R.string.action_ok)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private fun cursorModeOptions() = listOf(
    ListOption("1", "Screen Touch"),
    ListOption("2", "Using Mouse")
)

private fun subtitleLocaleOptions() = listOf(
    ListOption("en", "English (KC3改)"),
    ListOption("kr", "한국어 (KC3改)"),
    ListOption("jp", "日本語 (KC3改)"),
    ListOption("zh-cn", "简中 (kcwiki)"),
    ListOption("zh-tw", "繁中 (kcwiki)")
)

private fun alterMethodOptions() = listOf(
    ListOption("1", "URL Replacement"),
    ListOption("2", "KCCacheProxy (remote)")
)

private fun kccpLanguageOptions() = listOf(
    ListOption("kccp_lang_en", "KanColle English Patch"),
    ListOption("kccp_lang_id", "KanColle Indonesia Patch")
)

private fun connectorOptions() = listOf(
    ListOption(CONN_DMM, CONN_DMM),
    ListOption(CONN_KANMOE, CONN_KANMOE),
    ListOption(CONN_OOI, CONN_OOI)
)

private fun openUrl(context: Context, url: String) {
    if (url.isEmpty()) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

@Composable
private fun LoginFormDialog(
    initialId: String,
    initialPassword: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var loginId by remember { mutableStateOf(initialId) }
    var loginPassword by remember { mutableStateOf(initialPassword) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.autocomplete_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.autocomplete_msg),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = loginId,
                    onValueChange = { loginId = it },
                    label = { Text("DMM ID") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = { loginPassword = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(loginId, loginPassword) }) {
                Text(text = stringResource(id = R.string.text_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.text_cancel))
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Settings Bottom Sheet", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun SettingsBottomSheetPreview() {
    MaterialTheme {
        Column {
            SettingsBottomSheet(
                viewModel = null,
                activity = null,
                onDismissRequest = {},
                onLogoutRequest = {},
                onSettingChanged = {}
            )
        }
    }
}
