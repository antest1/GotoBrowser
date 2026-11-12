package com.antest1.gotobrowser.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
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
 * 第三方 MOD 页面中的「砍口垒汉化计划 in Goto」分区。
 * 总开关关闭时仅显示开关；开启后展开下载更新、数据源、下载范围与删除数据。
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

    val chsDownloadEnabled = if (isPreview) false
    else viewModel.chsUpdateEnabled.observeAsState(false).value

    var chsEnabled by remember(viewModel) {
        mutableStateOf(if (isPreview) false else viewModel.isChsPatchEnabled)
    }

    if (!isPreview) {
        LaunchedEffect(chsSummary) {
            chsEnabled = viewModel.isChsPatchEnabled
            viewModel.checkChsUpdateOnce()
        }
    }

    ChsGroupHeader(R.string.settings_mod_group_chs)

    val mainColor = MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_enable), color = mainColor) },
        supportingContent = { Text(stringResource(R.string.settings_mod_chs_summary), color = mainColor) },
        trailingContent = {
            Switch(
                checked = chsEnabled,
                onCheckedChange = { checked ->
                    chsEnabled = checked
                    if (!isPreview) viewModel.onChsPatchToggled(checked)
                    onChsToggled(checked)
                    onSettingChanged(ChsConfig.KEY_ENABLED)
                }
            )
        },
        modifier = Modifier.clickable {
            val next = !chsEnabled
            chsEnabled = next
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
    val mainColor = MaterialTheme.colorScheme.onSurface
    val hasInstalledData = if (isPreview) false else ChsPaths.hasInstalledData(context)

    // 下载与更新（点击弹出全量包 / 增量更新选项）
    var showDownloadDialog by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_download), color = mainColor) },
        supportingContent = { Text(chsSummary, color = mainColor) },
        modifier = Modifier.clickable { showDownloadDialog = true }
    )
    HorizontalDivider()

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

    // 数据源
    val sourceOptions = listOf(
        ChsOption(ChsConfig.SOURCE_GITHUB_RAW, stringResource(R.string.settings_mod_chs_source_github)),
        ChsOption(ChsConfig.SOURCE_JSDELIVR, stringResource(R.string.settings_mod_chs_source_jsdelivr)),
        ChsOption(ChsConfig.SOURCE_GHPROXY, stringResource(R.string.settings_mod_chs_source_ghproxy)),
        ChsOption(ChsConfig.SOURCE_CUSTOM, stringResource(R.string.settings_mod_chs_source_custom))
    )
    var selectedSource by remember(viewModel) {
        mutableStateOf(
            if (isPreview) ChsConfig.SOURCE_GITHUB_RAW
            else ChsConfig.getString(ChsConfig.KEY_DATA_SOURCE, ChsConfig.SOURCE_GITHUB_RAW)
        )
    }
    var showSourceDialog by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_source), color = mainColor) },
        supportingContent = {
            Text(sourceOptions.firstOrNull { it.value == selectedSource }?.title ?: selectedSource, color = mainColor)
        },
        modifier = Modifier.clickable { showSourceDialog = true }
    )
    HorizontalDivider()
    if (showSourceDialog) {
        ChsOptionDialog(
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
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_mod_chs_custom_url), color = mainColor) },
            supportingContent = { Text(customUrl.ifEmpty { ChsConfig.URL_GITHUB_RAW }, color = mainColor) },
            modifier = Modifier.clickable { showCustomUrlDialog = true }
        )
        HorizontalDivider()
        if (showCustomUrlDialog) {
            ChsEditUrlDialog(
                title = stringResource(R.string.settings_mod_chs_custom_url),
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

    // 删除数据
    var showDeleteDialog by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_delete), color = mainColor) },
        supportingContent = { Text(stringResource(R.string.settings_mod_chs_delete_summary), color = mainColor) },
        modifier = Modifier.clickable { showDeleteDialog = true }
    )
    HorizontalDivider()
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

    // 关于
    var showAboutDialog by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_about), color = mainColor) },
        supportingContent = { Text(stringResource(R.string.settings_mod_chs_about_summary), color = mainColor) },
        modifier = Modifier.clickable { showAboutDialog = true }
    )
    HorizontalDivider()

    if (showAboutDialog) {
        val context = LocalContext.current
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
private fun ChsGroupHeader(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(ListItemDefaults.containerColor)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

private data class ChsOption(val value: String, val title: String)

@Composable
private fun ChsOptionDialog(
    title: String,
    options: List<ChsOption>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option.value == selected, onClick = { onSelect(option.value) })
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = option.value == selected, onClick = { onSelect(option.value) })
                        Text(option.title, modifier = Modifier.padding(start = 4.dp))
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
private fun ChsEditUrlDialog(
    title: String,
    initialValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
