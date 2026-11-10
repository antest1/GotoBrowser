package com.antest1.gotobrowser.docshots

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import com.antest1.gotobrowser.Activity.BrowserOverlayLayer
import com.antest1.gotobrowser.Activity.LoadingTracker
import com.antest1.gotobrowser.Activity.PanelButton
import com.antest1.gotobrowser.Activity.PatchDownloadState
import com.antest1.gotobrowser.Activity.SettingsRefreshDialog
import com.antest1.gotobrowser.Activity.SettingsViewModel
import com.antest1.gotobrowser.Constants.DEFAULT_ALTER_GADGET_URL
import com.antest1.gotobrowser.Constants.PREF_ALTER_ENDPOINT
import com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET
import com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD
import com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD_URL
import com.antest1.gotobrowser.Constants.PREF_BROADCAST
import com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.ui.component.ControlLayout
import com.antest1.gotobrowser.ui.component.DialogButton
import com.antest1.gotobrowser.ui.component.KcanotifyBroadcastDialog
import com.antest1.gotobrowser.ui.component.LoadingStatusOverlay
import com.antest1.gotobrowser.ui.component.MessageDialog
import com.antest1.gotobrowser.ui.component.MessageDialogHost
import com.antest1.gotobrowser.ui.component.PatchDeleteDialog
import com.antest1.gotobrowser.ui.component.PatchDownloadDialog
import com.antest1.gotobrowser.ui.component.RefreshOption
import com.antest1.gotobrowser.ui.component.ServerNotice
import com.antest1.gotobrowser.ui.component.ServerNoticeAlignment
import com.antest1.gotobrowser.ui.component.ServerNoticeCard
import com.antest1.gotobrowser.ui.component.SettingsBottomSheet
import com.antest1.gotobrowser.ui.component.ThirdPartyConnectorDialog
import com.antest1.gotobrowser.ui.component.refreshAttentionColor
import com.antest1.gotobrowser.ui.component.requiresFullRefresh
import com.antest1.gotobrowser.ui.theme.GotobrowserTheme
import com.antest1.gotobrowser.ui.theme.SettingsTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Renders the app's Compose UI into images for the manual (docs/).
 *
 * Not a regression test: each case records one PNG per locale into
 * `tools/docshots/out/<locale>/` (ko, en, ja, zh-cn, zh-tw). The game area is a WebView and cannot be
 * rendered here, so it is filled with `tools/docshots/input/game_bg.*`
 * (a 5:3 capture of the game only) when that file exists.
 *
 * Run: gradlew :app:testDebugUnitTest --tests "*DocScreenshotTest*" -Pdocshots
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class DocScreenshotTest(private val locale: String) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun locales() = listOf("ko", "en", "ja", "zh-cn", "zh-tw")

        /** Resource qualifier for each docs locale (the app's zh-HK / MO / SG share zh-TW's text). */
        private val QUALIFIER = mapOf("zh-cn" to "zh-rCN", "zh-tw" to "zh-rTW")

        // 3120x1440 Galaxy at its default 560dpi (3.5x), as in the device captures
        private const val LANDSCAPE = "w891dp-h411dp-land-560dpi"
        private const val PORTRAIT = "w411dp-h891dp-port-560dpi"

        /** Regular / bold doc typefaces per locale, shared by every case (see applyDocTypeface). */
        private val docTypefaces = mutableMapOf<String, Pair<Typeface, Typeface>>()
    }

    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<ComponentActivity>? = null
    private lateinit var activity: ComponentActivity

    private val outDir = File(System.getProperty("docshots.outDir") ?: "build/docshots")
    private val inputDir = File(System.getProperty("docshots.inputDir") ?: "docshots-input")

    /**
     * Draws regular text one step heavier (Medium) and bold text as Bold, for Latin
     * (Roboto) and CJK (Noto Sans CJK) alike: at docs size, the default Regular looks
     * thinner than device captures. Everything else falls back to the system fonts.
     */
    private fun applyDocTypeface() {
        val fontDir = System.getProperty("robolectric.nativeruntime.fontdir")?.let(::File) ?: return
        // Noto Sans CJK collection order: 0 = ja, 1 = ko, 2 = zh-Hans, 3 = zh-Hant
        val cjkIndex = when (locale) {
            "ja" -> 0
            "zh-cn" -> 2
            "zh-tw" -> 3
            else -> 1
        }

        fun font(name: String, weight: Int, ttcIndex: Int = 0) =
            Font.Builder(File(fontDir, name)).setWeight(weight).setTtcIndex(ttcIndex).build()

        // Built once per locale: each build maps the CJK collections (~18MB each) again,
        // and doing that for every case crashed the test JVM after ~70 renders.
        val (regular, bold) = docTypefaces.getOrPut(locale) {
            val latin = FontFamily.Builder(font("Roboto-Medium.ttf", 400))
                .addFont(font("Roboto-Bold.ttf", 700)).build()
            val cjk = FontFamily.Builder(font("NotoSansCJK-Medium.ttc", 400, cjkIndex))
                .addFont(font("NotoSansCJK-Bold.ttc", 700, cjkIndex)).build()
            val regular = Typeface.CustomFallbackBuilder(latin)
                .addCustomFallback(cjk)
                .setSystemFallback("sans-serif")
                .build()
            regular to Typeface.create(regular, 700, false)
        }

        // Compose resolves FontFamily.Default through Typeface.DEFAULT; point the shared
        // instances at the new native typeface rather than replacing the (final) fields.
        val native = Typeface::class.java.getDeclaredField("native_instance").apply { isAccessible = true }
        listOf(Typeface.DEFAULT, Typeface.SANS_SERIF).forEach { native.setLong(it, native.getLong(regular)) }
        native.setLong(Typeface.DEFAULT_BOLD, native.getLong(bold))
        @Suppress("UNCHECKED_CAST")
        val systemFonts = Typeface::class.java.getDeclaredField("sSystemFontMap")
            .apply { isAccessible = true }.get(null) as MutableMap<String, Typeface>
        systemFonts["sans-serif"] = regular
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun str(id: Int) = activity.getString(id)

    /**
     * Launches [content] on a fresh activity in this locale, runs [action]
     * (clicks, touches) and records the whole screen, dialogs included.
     */
    private fun shoot(
        name: String,
        device: String = LANDSCAPE,
        action: () -> Unit = {},
        content: @Composable () -> Unit
    ) {
        RuntimeEnvironment.setQualifiers("+${QUALIFIER[locale] ?: locale}-$device")
        applyDocTypeface()
        scenario = ActivityScenario.launch(ComponentActivity::class.java).also { s ->
            s.onActivity { a ->
                activity = a
                a.setContent { content() }
            }
        }
        settle()
        action()
        val file = File(outDir, "$locale/$name.png").apply { parentFile?.mkdirs() }
        captureScreenRoboImage(file.path)
    }

    private fun settle(ms: Long = 1_500) {
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    // ------------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------------

    /** Game area (1200:720) fitted and centered on black, like the real layout. */
    @Composable
    private fun GameArea(background: String = "game_bg") {
        val file = listOf("png", "jpg", "webp").map { File(inputDir, "$background.$it") }.firstOrNull { it.exists() }
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            val wide = maxWidth / maxHeight > 1200f / 720f
            Box(
                Modifier.align(Alignment.Center)
                    .then(if (wide) Modifier.fillMaxHeight() else Modifier.fillMaxWidth())
                    .aspectRatio(1200f / 720f)
                    .background(Color(0xFF101010))
            ) {
                if (file != null) {
                    val bitmap = BitmapFactory.decodeFile(file.path).asImageBitmap()
                    Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                }
            }
        }
    }

    /** Game + overlay (screenshot button) + toolbar, as the browser draws them. */
    @Composable
    private fun Browser(subtitle: String = "", refreshOption: RefreshOption = RefreshOption.QUICK, toolbarAtTop: Boolean = false) {
        GotobrowserTheme {
            GameArea()
            BrowserOverlayLayer(
                showSubtitle = subtitle.isNotEmpty(),
                subtitleText = subtitle,
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
                selectedRefreshOption = refreshOption,
                toolbarAtTop = toolbarAtTop
            ) {
                PanelButton(id = R.drawable.volume_off, tooltip = stringResource(R.string.menu_tooltip_mute), active = true, onClick = {})
                PanelButton(id = R.drawable.camera_icon, tooltip = stringResource(R.string.menu_tooltip_camera), active = true, onClick = {})
                PanelButton(id = R.drawable.screen_lock, tooltip = stringResource(R.string.menu_tooltip_lock), onClick = {})
                PanelButton(id = R.drawable.light_mode, tooltip = stringResource(R.string.menu_tooltip_brighton), onClick = {})
                PanelButton(id = R.drawable.caption_icon, tooltip = stringResource(R.string.menu_tooltip_cc), active = true, onClick = {})
            }
        }
    }

    @Composable
    private fun OverGame(content: @Composable () -> Unit) {
        GotobrowserTheme { GameArea() }
        content()
    }

    @Composable
    private fun Messages(vararg dialogs: MessageDialog) {
        OverGame { MessageDialogHost(mutableStateListOf(*dialogs)) }
    }

    private fun message(title: String, message: String, vararg buttons: Int) = MessageDialog(
        title, message,
        DialogButton(str(buttons[0])),
        buttons.getOrNull(1)?.let { DialogButton(str(it)) },
        buttons.getOrNull(2)?.let { DialogButton(str(it)) }
    )

    // ------------------------------------------------------------------
    // Browser UI
    // ------------------------------------------------------------------

    @Test
    fun toolbar() = shoot("ui_toolbar") { Browser() }

    @Test
    fun toolbarPortrait() = shoot("ui_toolbar_portrait", device = PORTRAIT) { Browser() }

    /** Long press on a toolbar button shows its name. */
    @Test
    fun toolbarTooltip() = shoot("ui_toolbar_tooltip", action = {
        compose.onNodeWithContentDescription(str(R.string.menu_tooltip_lock)).performTouchInput { down(center) }
        settle(1_000)
    }) { Browser() }

    /** Press the refresh button and drag up: the other refresh mode is offered. */
    @Test
    fun refreshMenu() = shoot("ui_refresh_menu", action = {
        compose.onNodeWithContentDescription(str(R.string.menu_tooltip_refresh)).performTouchInput {
            // The node is the icon, so move by a fixed distance: one menu item (~56dp) up.
            down(center)
            moveBy(Offset(0f, -64.dp.toPx()))
        }
        settle(800)
    }) { Browser() }

    /** Toolbar docked at the top, refresh first (Settings > Controls & Toolbar > Toolbar Position). */
    @Test
    fun toolbarTop() = shoot("ui_toolbar_top") { Browser(toolbarAtTop = true) }

    /** With the toolbar at the top, the refresh menu opens downward: press and drag down. */
    @Test
    fun refreshMenuTop() = shoot("ui_refresh_menu_top", action = {
        compose.onNodeWithContentDescription(str(R.string.menu_tooltip_refresh)).performTouchInput {
            down(center)
            moveBy(Offset(0f, 64.dp.toPx()))
        }
        settle(800)
    }) { Browser(toolbarAtTop = true) }

    // ------------------------------------------------------------------
    // Loading and error notices
    // ------------------------------------------------------------------

    /**
     * Game area with the loading card / in-game notice drawn inside it, and the toolbar
     * pointing at the refresh button as the browser does for that state.
     */
    @Composable
    private fun Status(state: LoadingTracker.Snapshot, notice: ServerNotice? = null, background: String = "none") {
        GotobrowserTheme {
            GameArea(background)
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth / maxHeight > 1200f / 720f
                Box(
                    Modifier.align(Alignment.Center)
                        .then(if (wide) Modifier.fillMaxHeight() else Modifier.fillMaxWidth())
                        .aspectRatio(1200f / 720f)
                ) {
                    // The default connector, as the app names it in the card title.
                    LoadingStatusOverlay(state, connectorTag = "DMM")
                    ServerNoticeCard(notice, onDismiss = {}, modifier = Modifier.align(ServerNoticeAlignment).padding(16.dp))
                }
            }
            ControlLayout(
                visible = true,
                onVisibleChange = {},
                onRefreshClick = {},
                onSettingsClick = {},
                refreshAttention = refreshAttentionColor(state, notice != null),
                forceFullRefresh = requiresFullRefresh(state, notice)
            ) {
                PanelButton(id = R.drawable.volume_off, tooltip = stringResource(R.string.menu_tooltip_mute), active = true, onClick = {})
                PanelButton(id = R.drawable.camera_icon, tooltip = stringResource(R.string.menu_tooltip_camera), active = true, onClick = {})
                PanelButton(id = R.drawable.screen_lock, tooltip = stringResource(R.string.menu_tooltip_lock), onClick = {})
                PanelButton(id = R.drawable.light_mode, tooltip = stringResource(R.string.menu_tooltip_brighton), onClick = {})
                PanelButton(id = R.drawable.caption_icon, tooltip = stringResource(R.string.menu_tooltip_cc), active = true, onClick = {})
            }
        }
    }

    /** The card fades in after a delay() on the main Looper, which the compose clock does not advance. */
    private val showStatus: () -> Unit = {
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        settle(3_000)
    }

    @Test
    fun loadingStatus() = shoot("ui_loading_status", action = showStatus) {
        Status(LoadingTracker.Snapshot(stage = LoadingTracker.Stage.CONNECTING, progress = 62))
    }

    @Test
    fun loadingError() = shoot("ui_loading_error", action = showStatus) {
        Status(
            LoadingTracker.Snapshot(
                stage = LoadingTracker.Stage.CONNECTING,
                failure = LoadingTracker.Failure.Network(
                    WebViewClient.ERROR_HOST_LOOKUP, "net::ERR_NAME_NOT_RESOLVED", "https://play.games.dmm.com/game/kancolle"
                )
            )
        )
    }

    /** Over the game's own connection error screen, which the notice must not cover. */
    @Test
    fun gameNotice() = shoot("ui_game_notice", action = showStatus) {
        Status(
            LoadingTracker.Snapshot(stage = LoadingTracker.Stage.READY),
            ServerNotice(ServerNotice.Kind.NETWORK, "ERR_NETWORK", "api_req_hokyu/charge", "Network Error"),
            background = "error_screen"
        )
    }

    // ------------------------------------------------------------------
    // Settings sheet
    // ------------------------------------------------------------------

    /**
     * A real [SettingsViewModel] over preferences set up the way the manual describes
     * them (rows then show real values, e.g. the app version, instead of preview stubs).
     * KCCP stays off so opening the sheet does not start a patch update check.
     */
    private fun docViewModel(): SettingsViewModel {
        activity.getSharedPreferences(str(R.string.preference_key), Context.MODE_PRIVATE).edit()
            .putBoolean(PREF_BROADCAST, true)
            .putBoolean(PREF_ALTER_GADGET, true)
            .putString(PREF_ALTER_METHOD, PREF_ALTER_METHOD_URL)
            .putString(PREF_ALTER_ENDPOINT, DEFAULT_ALTER_GADGET_URL)
            .putString(PREF_SUBTITLE_LOCALE, mapOf("ko" to "kr", "ja" to "jp", "zh-cn" to "zh-cn", "zh-tw" to "zh-tw").getOrDefault(locale, "en"))
            .commit()
        // KcUtils grabs Crashlytics in its static initializer; give it a local, offline app.
        if (FirebaseApp.getApps(activity).isEmpty()) {
            FirebaseApp.initializeApp(
                activity,
                FirebaseOptions.Builder()
                    .setApplicationId("1:000000000000:android:0000000000000000")
                    .setProjectId("docshots")
                    .setApiKey("docshots")
                    .build()
            )
        }
        return SettingsViewModel(activity.application)
    }

    @Composable
    private fun Sheet() = OverGame {
        val viewModel = remember { docViewModel() }
        SettingsBottomSheet(viewModel = viewModel, activity = activity, onDismissRequest = {}, onSettingChanged = {})
    }

    private fun openPage(label: Int, scrollTo: Int? = null): () -> Unit = {
        compose.onNodeWithText(str(label)).performClick()
        settle(1_000)
        // A section header can share the row's text; the row is the last match.
        scrollTo?.let { compose.onAllNodesWithText(str(it)).onLast().performScrollTo(); settle(500) }
    }

    @Test
    fun settingsMain() = shoot("settings_main") { Sheet() }

    @Test
    fun settingsConnection() = shoot("settings_connection", action = openPage(R.string.settings_connection_login_label)) { Sheet() }

    /** Scrolled to broadcast mode, for the Kcanotify page. */
    @Test
    fun settingsBroadcast() = shoot(
        "settings_broadcast",
        action = openPage(R.string.settings_connection_login_label, scrollTo = R.string.mode_broadcast)
    ) { Sheet() }

    @Test
    fun settingsGadget() = shoot(
        "settings_gadget",
        action = openPage(R.string.settings_connection_login_label, scrollTo = R.string.setting_alter_endpoint)
    ) { Sheet() }

    @Test
    fun settingsDisplay() = shoot("settings_display", action = openPage(R.string.settings_display_label)) { Sheet() }

    @Test
    fun settingsControl() = shoot("settings_control", action = openPage(R.string.settings_control_label)) { Sheet() }

    @Test
    fun settingsCache() = shoot("settings_cache", action = openPage(R.string.settings_download_label)) { Sheet() }

    @Test
    fun settingsSubtitle() = shoot("settings_subtitle", action = openPage(R.string.settings_subtitle_label)) { Sheet() }

    @Test
    fun settingsMods() = shoot("settings_mods", action = openPage(R.string.settings_mod_label)) { Sheet() }

    @Test
    fun settingsAppInfo() = shoot("settings_appinfo", action = openPage(R.string.settings_appinfo_label)) { Sheet() }

    // ------------------------------------------------------------------
    // Dialogs (same composables / strings as the app)
    // ------------------------------------------------------------------

    @Test
    fun dialogQuickRefresh() = shoot("dialog_quick_refresh") {
        Messages(message(str(R.string.menu_quick_refresh), str(R.string.refresh_quick_msg), R.string.action_ok, R.string.action_cancel))
    }

    @Test
    fun dialogFullRefresh() = shoot("dialog_full_refresh") {
        Messages(message(str(R.string.menu_full_refresh), str(R.string.refresh_full_msg), R.string.action_ok, R.string.action_cancel))
    }

    @Test
    fun dialogRefreshRequired() = shoot("dialog_refresh_required") {
        OverGame { SettingsTheme { SettingsRefreshDialog(onRefreshNow = {}, onChangeWithoutRefresh = {}) } }
    }

    @Test
    fun dialogIpBlock() = shoot("dialog_ipblock") {
        Messages(message(
            str(R.string.dialog_ipblock_title),
            String.format(str(R.string.dialog_ipblock_message), str(R.string.connection_use_alter)),
            R.string.action_ok
        ))
    }

    @Test
    fun dialogKcanotify() = shoot("dialog_kcanotify") {
        OverGame { SettingsTheme { KcanotifyBroadcastDialog(onAccept = {}, onDismiss = {}) } }
    }

    @Test
    fun dialogThirdPartyConnector() = shoot("dialog_thirdparty_connector") {
        OverGame { SettingsTheme { ThirdPartyConnectorDialog(onAccept = {}, onDismiss = {}) } }
    }

    @Test
    fun dialogAssetRetry() = shoot("dialog_asset_retry") {
        Messages(message(
            str(R.string.dialog_retry_title),
            String.format(str(R.string.dialog_retry_message), "/kcs2/img/common/common_main.png"),
            R.string.dialog_retry_yes, R.string.dialog_retry_no, R.string.dialog_retry_never
        ))
    }

    @Test
    fun dialogLogout() = shoot("dialog_logout") {
        Messages(message(str(R.string.app_name), str(R.string.logout_msg), R.string.action_ok, R.string.action_cancel))
    }

    @Test
    fun dialogPatchDownload() = shoot("dialog_patch_download") {
        OverGame { SettingsTheme { PatchDownloadDialog(PatchDownloadState.Running(str(R.string.patch_download_archive), 0.42f)) } }
    }

    @Test
    fun dialogPatchDelete() = shoot("dialog_patch_delete") {
        OverGame { SettingsTheme { PatchDeleteDialog(onConfirm = {}, onDismiss = {}) } }
    }
}
