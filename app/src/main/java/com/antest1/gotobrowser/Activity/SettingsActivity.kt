package com.antest1.gotobrowser.Activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT
import com.antest1.gotobrowser.Constants.PREF_ALTER_ENDPOINT
import com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET
import com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD
import com.antest1.gotobrowser.Constants.PREF_BROADCAST
import com.antest1.gotobrowser.Constants.PREF_CHECK_UPDATE
import com.antest1.gotobrowser.Constants.PREF_CURSOR_MODE
import com.antest1.gotobrowser.Constants.PREF_DEVTOOLS_DEBUG
import com.antest1.gotobrowser.Constants.PREF_DISABLE_REFRESH_DIALOG
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
import com.antest1.gotobrowser.Constants.PREF_PIP_MODE
import com.antest1.gotobrowser.Constants.PREF_SETTINGS
import com.antest1.gotobrowser.Constants.PREF_SUBTITLE_FONTSIZE
import com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE
import com.antest1.gotobrowser.Constants.PREF_SUBTITLE_UPDATE
import com.antest1.gotobrowser.Constants.PREF_USE_EXTCACHE
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.ui.theme.GotobrowserTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val DEFAULT_SUBTITLE_FONTSIZE = 18
private const val MIN_SUBTITLE_FONTSIZE = 12
private const val MAX_SUBTITLE_FONTSIZE = 24

private const val GITHUB_KANTAI3D = "https://github.com/laplamgor/kantai3d"
private const val GITHUB_GOTOBROWSER = "https://github.com/antest1/GotoBrowser/"

private data class ListOption(val value: String, val title: String, val summary: String? = null)

class SettingsActivity : AppCompatActivity() {
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setContent {
            GotobrowserTheme {
                SettingsScreen(
                    onBack = { finish() },
                    viewModel = viewModel
                )
            }
        }
        createNotificationChannel()
    }

    companion object {
        const val CHANNEL_ID = "gotobrowser_screenshot"

        @JvmStatic
        fun setInitialSettings(sharedPref: SharedPreferences) {
            val editor = sharedPref.edit()
            for (key in PREF_SETTINGS) {
                if (!sharedPref.contains(key)) {
                    when (key) {
                        PREF_LANDSCAPE, PREF_KEYBOARD, PREF_SUBTITLE_UPDATE -> editor.putBoolean(key, true)
                        PREF_ADJUSTMENT, PREF_BROADCAST, PREF_USE_EXTCACHE,
                        PREF_PIP_MODE, PREF_MULTIWIN_MARGIN, PREF_ALTER_GADGET,
                        PREF_DOWNLOAD_RETRY, PREF_MOD_KANTAI3D, PREF_MOD_KCCP_LANG_PATCH,
                        PREF_MOD_FPS, PREF_MOD_CRIT, PREF_DEVTOOLS_DEBUG -> editor.putBoolean(key, false)
                        PREF_CURSOR_MODE -> editor.putString(key, "1")
                        PREF_ALTER_METHOD -> editor.putString(key, "1")
                        PREF_SUBTITLE_FONTSIZE -> editor.putInt(key, DEFAULT_SUBTITLE_FONTSIZE)
                    }
                }
            }
            editor.apply()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }

    val patchSummary by viewModel.patchUpdateSummary.observeAsState("checking updates...")
    val patchEnabled by viewModel.patchUpdateEnabled.observeAsState(false)
    val subtitleSummary by viewModel.subtitleUpdateSummary.observeAsState("checking updates...")
    val subtitleEnabled by viewModel.subtitleUpdateEnabled.observeAsState(false)
    val patchAboutTitle by viewModel.patchAboutTitleRes.observeAsState(R.string.settings_mod_kantaien_about)
    val patchAboutUrl by viewModel.patchAboutUrl.observeAsState("")

    // Triggers the same "onViewCreated" side effects the old fragment ran:
    // updateSubtitleDescriptionText / updateKCCPLangPatchDescriptionText /
    // updateKCCPLangPatchInfo / updateKantai3dDisable.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshSubtitleDescription()
        viewModel.refreshKccpDependentRows(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            BrowserSettingsSection(viewModel)
            SubtitleSection(viewModel, subtitleSummary, subtitleEnabled)
            ConnectionSection(viewModel, snackbarHostState)
            ModsSection(
                viewModel,
                patchTitle = patchAboutTitle,
                patchUrl = patchAboutUrl,
                patchSummary = patchSummary,
                patchEnabledState = patchEnabled
            )
            AppInfoSection(viewModel)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Sections
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(resId: Int) {
    Text(
        text = stringResource(resId),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun BrowserSettingsSection(viewModel: SettingsViewModel) {
    SectionHeader(R.string.settings_appinfo_browsersettings)
    SwitchRow(viewModel, PREF_LANDSCAPE, R.string.mode_landscape, R.string.settings_recommended_summary)
    SwitchRow(viewModel, PREF_ADJUSTMENT, R.string.mode_adjustment, R.string.settings_recommended_summary)
    SwitchRow(viewModel, PREF_FONT_PREFETCH, R.string.browser_fontprefetch, R.string.settings_recommended_summary)
    SwitchRow(viewModel, PREF_USE_EXTCACHE, R.string.settings_use_external_dir, R.string.settings_recommended_summary,
        onChanged = { viewModel.onExternalCacheChanged() })
    SwitchRow(viewModel, PREF_KEYBOARD, R.string.mode_enable_keyboard)
    ListRow(viewModel, PREF_CURSOR_MODE, R.string.setting_cursor_mode, cursorModeOptions())
    SwitchRow(viewModel, PREF_DISABLE_REFRESH_DIALOG, R.string.browser_disable_refresh_dialog)
    SwitchRow(viewModel, PREF_PIP_MODE, R.string.browser_enablepipmode)
    SwitchRow(viewModel, PREF_MULTIWIN_MARGIN, R.string.settings_mw_margin)
    SwitchRow(viewModel, PREF_LEGACY_RENDERER, R.string.settings_legacy_renderer_enable,
        R.string.settings_legacy_renderer_summary, onChanged = { viewModel.onLegacyRendererChanged() })
}

@Composable
private fun SubtitleSection(viewModel: SettingsViewModel, subtitleSummary: String, subtitleEnabled: Boolean) {
    SectionHeader(R.string.settings_subtitle_label)
    ListRow(viewModel, PREF_SUBTITLE_LOCALE, R.string.settings_subtitle_language, subtitleLocaleOptions(),
        onSelected = { viewModel.onSubtitleLocaleChanged(it); true })

    var showSubtitleSizeDialog by remember { mutableStateOf(false) }
    val subtitleSize = remember { mutableIntStateOf(viewModel.getSubtitleFontSize()) }
    ClickRow(
        title = R.string.settings_subtitle_fontsize,
        summaryText = subtitleSize.intValue.toString(),
        onClick = { showSubtitleSizeDialog = true }
    )
    if (showSubtitleSizeDialog) {
        SubtitleSizeDialog(
            initialSize = subtitleSize.intValue,
            onSave = { newSize ->
                viewModel.setInt(PREF_SUBTITLE_FONTSIZE, newSize)
                subtitleSize.intValue = newSize
                showSubtitleSizeDialog = false
            },
            onDismiss = { showSubtitleSizeDialog = false }
        )
    }

    ClickRow(
        title = R.string.settings_subtitle_download,
        summaryText = subtitleSummary,
        enabled = subtitleEnabled,
        onClick = { viewModel.downloadSubtitleUpdate() }
    )
}

@Composable
private fun ConnectionSection(viewModel: SettingsViewModel, snackbarHostState: androidx.compose.material3.SnackbarHostState) {
    val scope = rememberCoroutineScope()
    SectionHeader(R.string.setting_connection)
    SwitchRow(viewModel, PREF_ALTER_GADGET, R.string.connection_use_alter, R.string.connection_use_alter_summary)

    val alterGadgetEnabled = viewModel.getBoolean(PREF_ALTER_GADGET, false)
    ListRow(
        viewModel, PREF_ALTER_METHOD, R.string.setting_alter_method, alterMethodOptions(),
        enabled = alterGadgetEnabled,
        onSelected = { value ->
            if (viewModel.onAlterMethodSelected(value)) {
                true
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("PROXY_OVERRIDE not supported, use other option")
                }
                false
            }
        }
    )

    var endpoint by remember { mutableStateOf(viewModel.getAlterEndpoint()) }
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
                viewModel.onAlterEndpointChanged(value)
                endpoint = viewModel.getAlterEndpoint()
                showEndpointDialog = false
            },
            onDismiss = { showEndpointDialog = false }
        )
    }

    SwitchRow(viewModel, PREF_DOWNLOAD_RETRY, R.string.settings_retry_enable, R.string.settings_retry_summary)
}

@Composable
private fun ModsSection(
    viewModel: SettingsViewModel,
    patchTitle: Int,
    patchUrl: String,
    patchSummary: String,
    patchEnabledState: Boolean
) {
    val context = LocalContext.current
    SectionHeader(R.string.settings_mod_label)
    SwitchRow(viewModel, PREF_MOD_FPS, R.string.settings_mod_fps_enable, R.string.settings_mod_fps_summary)

    val kantai3dEnabled = viewModel.isKantai3dEnabled()
    SwitchRow(viewModel, PREF_MOD_KANTAI3D, R.string.settings_mod_kantai3d_enable,
        R.string.settings_mod_kantai3d_summary, enabled = kantai3dEnabled)
    ClickRow(
        title = R.string.settings_mod_kantai3d_about,
        summaryText = GITHUB_KANTAI3D,
        onClick = { openUrl(context, GITHUB_KANTAI3D) }
    )

    SwitchRow(viewModel, PREF_MOD_CRIT, R.string.settings_mod_crit_enable, R.string.settings_mod_crit_summary)
    SwitchRow(viewModel, PREF_MOD_KCCP_LANG_PATCH, R.string.settings_mod_kccp, R.string.settings_mod_kccp_summary,
        onChanged = { viewModel.onKccpPatchChanged() })

    val patchEnabled = viewModel.isKccpPatchEnabled()
    ListRow(
        viewModel, PREF_MOD_KCCP_LANG_PATCH_NAME, R.string.settings_mod_kccp_patch_name, kccpLanguageOptions(),
        enabled = patchEnabled,
        summaryProvider = { viewModel.getKccpPatchSummary(it) },
        onSelected = { viewModel.onPatchLanguageChanged(it); true }
    )

    ClickRow(
        title = R.string.settings_mod_kantaien_download,
        summaryText = patchSummary,
        enabled = patchEnabled && patchEnabledState,
        onClick = { viewModel.requestPatchUpdate() }
    )
    ClickRow(
        title = R.string.settings_mod_kantaien_delete,
        summary = R.string.settings_mod_kantaien_delete_summary,
        enabled = patchEnabled,
        onClick = { viewModel.requestPatchDelete() }
    )
    ClickRow(
        title = patchTitle,
        summaryText = patchUrl,
        enabled = patchEnabled,
        onClick = { openUrl(context, patchUrl) }
    )
}

@Composable
private fun AppInfoSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    SectionHeader(R.string.settings_appinfo_label)
    ClickRow(
        title = R.string.settings_version_label,
        summaryText = viewModel.getAppVersion(),
        onClick = {},
        enabled = false
    )
    ClickRow(
        title = R.string.settings_version_check,
        onClick = { activity?.let { viewModel.checkAppUpdate(it) } }
    )
    ClickRow(
        title = R.string.settings_source_code,
        summaryText = GITHUB_GOTOBROWSER,
        onClick = { openUrl(context, GITHUB_GOTOBROWSER) }
    )
    SwitchRow(viewModel, PREF_DEVTOOLS_DEBUG, R.string.setting_devtools_enable, R.string.setting_devtools_description)
}

// ---------------------------------------------------------------------------
// Reusable rows
// ---------------------------------------------------------------------------

@Composable
private fun SwitchRow(
    viewModel: SettingsViewModel,
    key: String,
    titleRes: Int,
    summaryRes: Int? = null,
    enabled: Boolean = true,
    onChanged: () -> Unit = {}
) {
    val checked = remember { mutableStateOf(viewModel.getBoolean(key, false)) }
    ListItem(
        headlineContent = { Text(stringResource(titleRes)) },
        supportingContent = summaryRes?.let { { Text(stringResource(it)) } },
        trailingContent = {
            Switch(
                checked = checked.value,
                enabled = enabled,
                onCheckedChange = {
                    checked.value = it
                    viewModel.setBoolean(key, it)
                    onChanged()
                }
            )
        },
        modifier = Modifier.clickable(enabled = enabled) {
            val newValue = !checked.value
            checked.value = newValue
            viewModel.setBoolean(key, newValue)
            onChanged()
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
    viewModel: SettingsViewModel,
    key: String,
    titleRes: Int,
    options: List<ListOption>,
    enabled: Boolean = true,
    summaryProvider: (String) -> String = { value ->
        options.firstOrNull { it.value == value }?.summary
            ?: options.firstOrNull { it.value == value }?.title ?: value
    },
    onSelected: (String) -> Boolean = { true }
) {
    var selected by remember { mutableStateOf(viewModel.getString(key, options.first().value)) }
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
                    viewModel.setString(key, value)
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
private fun openUrl(context: Context, url: String) {
    if (url.isEmpty()) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
