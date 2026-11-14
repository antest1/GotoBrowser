package com.antest1.gotobrowser.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.antest1.gotobrowser.Activity.SettingsViewModel
import com.antest1.gotobrowser.Helpers.chs.ChsConfig
import com.antest1.gotobrowser.Helpers.chs.ChsPaths
import com.antest1.gotobrowser.R

/**
 * Simplified Chinese localization (Kancolle_to_CHS) settings group on the Mods screen.
 * Collapses management rows while disabled and observes ViewModel LiveData for KCCP exclusion.
 */
@Composable
fun ChsSettingsSection(
    viewModel: SettingsViewModel?,
    onChsToggled: (Boolean) -> Unit,
    onSettingChanged: (String) -> Unit
) {
    val isPreview = LocalInspectionMode.current || viewModel == null

    val chsSummary = if (isPreview) "checking updates..."
    else viewModel.chsUpdateSummary.observeAsState("checking updates...").value

    val chsEnabled = if (isPreview) false
    else viewModel.chsEnabledState.observeAsState(viewModel.isChsPatchEnabled).value

    if (!isPreview) {
        LaunchedEffect(viewModel) {
            viewModel.checkChsUpdateOnce()
        }
    }

    SectionHeader(R.string.settings_mod_group_chs)

    val mainColor = MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_enable), color = mainColor) },
        supportingContent = { Text(stringResource(R.string.settings_mod_chs_summary), color = mainColor) },
        trailingContent = {
            Switch(
                checked = chsEnabled,
                onCheckedChange = { checked ->
                    if (!isPreview) viewModel.onChsPatchToggled(checked)
                    onChsToggled(checked)
                    onSettingChanged(ChsConfig.KEY_ENABLED)
                }
            )
        },
        modifier = Modifier.clickable {
            val next = !chsEnabled
            if (!isPreview) viewModel.onChsPatchToggled(next)
            onChsToggled(next)
            onSettingChanged(ChsConfig.KEY_ENABLED)
        }
    )
    HorizontalDivider()

    AnimatedVisibility(visible = chsEnabled) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ChsExpandedRows(viewModel, isPreview, chsSummary)
        }
    }
}

@Composable
private fun ChsExpandedRows(
    viewModel: SettingsViewModel?,
    isPreview: Boolean,
    chsSummary: String
) {
    val context = LocalContext.current
    val hasInstalledData = if (isPreview) false else ChsPaths.hasInstalledData(context)

    // Download & update row (opens release zip vs incremental diff dialog)
    var showDownloadDialog by remember { mutableStateOf(false) }
    ClickRow(
        title = R.string.settings_mod_chs_download,
        summaryText = chsSummary,
        onClick = { showDownloadDialog = true }
    )

    if (showDownloadDialog) {
        ChsDownloadDialog(
            hasInstalledData = hasInstalledData,
            onDownloadArchive = {
                showDownloadDialog = false
                if (!isPreview) viewModel?.requestChsReleaseArchiveUpdate()
            },
            onDownloadPatch = {
                showDownloadDialog = false
                if (!isPreview) viewModel?.requestChsPatchUpdate()
            },
            onDismiss = { showDownloadDialog = false }
        )
    }

    // Mirror / data source selector
    val sourceOptions = listOf(
        ListOption(ChsConfig.SOURCE_GITHUB_RAW, stringResource(R.string.settings_mod_chs_source_github)),
        ListOption(ChsConfig.SOURCE_JSDELIVR, stringResource(R.string.settings_mod_chs_source_jsdelivr)),
        ListOption(ChsConfig.SOURCE_GHPROXY, stringResource(R.string.settings_mod_chs_source_ghproxy)),
        ListOption(ChsConfig.SOURCE_CUSTOM, stringResource(R.string.settings_mod_chs_source_custom))
    )
    var selectedSource by remember(viewModel) {
        mutableStateOf(
            if (isPreview) ChsConfig.SOURCE_GITHUB_RAW
            else ChsConfig.getString(ChsConfig.KEY_DATA_SOURCE, ChsConfig.SOURCE_GITHUB_RAW)
        )
    }
    var showSourceDialog by remember { mutableStateOf(false) }
    ClickRow(
        title = R.string.settings_mod_chs_source,
        summaryText = sourceOptions.firstOrNull { it.value == selectedSource }?.title ?: selectedSource,
        onClick = { showSourceDialog = true }
    )
    if (showSourceDialog) {
        ListOptionDialog(
            title = stringResource(R.string.settings_mod_chs_source),
            options = sourceOptions,
            selected = selectedSource,
            onSelect = { value ->
                selectedSource = value
                if (!isPreview) {
                    ChsConfig.setString(ChsConfig.KEY_DATA_SOURCE, value)
                    viewModel?.refreshChsUpdateStatus()
                }
                showSourceDialog = false
            },
            onDismiss = { showSourceDialog = false }
        )
    }

    if (selectedSource == ChsConfig.SOURCE_CUSTOM) {
        var customUrl by remember(viewModel) {
            mutableStateOf(
                if (isPreview) ChsConfig.URL_GITHUB_RAW
                else ChsConfig.getString(ChsConfig.KEY_CUSTOM_SOURCE_URL, ChsConfig.URL_GITHUB_RAW)
            )
        }
        var showCustomUrlDialog by remember { mutableStateOf(false) }
        ClickRow(
            title = R.string.settings_mod_chs_custom_url,
            summaryText = customUrl.ifEmpty { ChsConfig.URL_GITHUB_RAW },
            onClick = { showCustomUrlDialog = true }
        )
        if (showCustomUrlDialog) {
            EditTextDialog(
                title = R.string.settings_mod_chs_custom_url,
                initialValue = customUrl,
                onSave = { newUrl ->
                    val normalized = newUrl.trim().ifEmpty { ChsConfig.URL_GITHUB_RAW }
                    customUrl = normalized
                    if (!isPreview) {
                        ChsConfig.setString(ChsConfig.KEY_CUSTOM_SOURCE_URL, normalized)
                        viewModel?.refreshChsUpdateStatus()
                    }
                    showCustomUrlDialog = false
                },
                onDismiss = { showCustomUrlDialog = false }
            )
        }
    }

    // Delete local CHS data
    var showDeleteDialog by remember { mutableStateOf(false) }
    ClickRow(
        title = R.string.settings_mod_chs_delete,
        summary = R.string.settings_mod_chs_delete_summary,
        onClick = { showDeleteDialog = true }
    )
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.settings_mod_chs_delete)) },
            text = { Text(stringResource(R.string.settings_mod_chs_delete_summary)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    if (!isPreview) viewModel?.deleteChsPatch()
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // About / credits dialog
    var showAboutDialog by remember { mutableStateOf(false) }
    ClickRow(
        title = R.string.settings_mod_chs_about,
        summary = R.string.settings_mod_chs_about_summary,
        onClick = { showAboutDialog = true }
    )

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.settings_mod_chs_about)) },
            text = { Text(stringResource(R.string.settings_mod_chs_about_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ChsConfig.OFFICIAL_GITHUB_URL))
                    context.startActivity(intent)
                }) {
                    Text("GitHub")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }
}

@Composable
private fun ChsDownloadDialog(
    hasInstalledData: Boolean,
    onDownloadArchive: () -> Unit,
    onDownloadPatch: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_mod_chs_download_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (!hasInstalledData) {
                    ChsDownloadOptionItem(
                        title = stringResource(R.string.settings_mod_chs_download_archive_recommended),
                        description = stringResource(R.string.settings_mod_chs_download_archive_desc),
                        isRecommended = true,
                        onClick = onDownloadArchive
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    ChsDownloadOptionItem(
                        title = stringResource(R.string.settings_mod_chs_download_patch_option),
                        description = stringResource(R.string.settings_mod_chs_download_patch_desc),
                        isRecommended = false,
                        onClick = onDownloadPatch
                    )
                } else {
                    ChsDownloadOptionItem(
                        title = stringResource(R.string.settings_mod_chs_download_patch_recommended),
                        description = stringResource(R.string.settings_mod_chs_download_patch_desc),
                        isRecommended = true,
                        onClick = onDownloadPatch
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    ChsDownloadOptionItem(
                        title = stringResource(R.string.settings_mod_chs_download_archive_option),
                        description = stringResource(R.string.settings_mod_chs_download_archive_desc),
                        isRecommended = false,
                        onClick = onDownloadArchive
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ChsDownloadOptionItem(
    title: String,
    description: String,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
