package com.antest1.gotobrowser.ui.component

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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.antest1.gotobrowser.Activity.SettingsViewModel
import com.antest1.gotobrowser.Helpers.chs.ChsConfig
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
            ChsExpandedRows(viewModel, isPreview, chsSummary, chsDownloadEnabled)
        }
    }
}

@Composable
private fun ChsExpandedRows(
    viewModel: SettingsViewModel?,
    isPreview: Boolean,
    chsSummary: String,
    chsDownloadEnabled: Boolean
) {
    val mainColor = MaterialTheme.colorScheme.onSurface

    // 下载更新
    val dlColor = if (chsDownloadEnabled) mainColor else mainColor.copy(alpha = 0.38f)
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_download), color = dlColor) },
        supportingContent = { Text(chsSummary, color = dlColor) },
        modifier = if (chsDownloadEnabled) Modifier.clickable {
            if (!isPreview) viewModel?.requestChsPatchUpdate()
        } else Modifier
    )
    HorizontalDivider()

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

    // 下载范围
    val modeOptions = listOf(
        ChsOption(ChsConfig.DOWNLOAD_MODE_FULL, stringResource(R.string.settings_mod_chs_mode_full)),
        ChsOption(ChsConfig.DOWNLOAD_MODE_TEXT_ONLY, stringResource(R.string.settings_mod_chs_mode_text_only))
    )
    var selectedMode by remember(viewModel) {
        mutableStateOf(
            if (isPreview) ChsConfig.DOWNLOAD_MODE_FULL
            else ChsConfig.getString(ChsConfig.KEY_DOWNLOAD_MODE, ChsConfig.DOWNLOAD_MODE_FULL)
        )
    }
    var showModeDialog by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_download_mode), color = mainColor) },
        supportingContent = {
            Text(modeOptions.firstOrNull { it.value == selectedMode }?.title ?: selectedMode, color = mainColor)
        },
        modifier = Modifier.clickable { showModeDialog = true }
    )
    HorizontalDivider()
    if (showModeDialog) {
        ChsOptionDialog(
            title = stringResource(R.string.settings_mod_chs_download_mode),
            options = modeOptions,
            selected = selectedMode,
            onSelect = { value ->
                selectedMode = value
                if (!isPreview) {
                    ChsConfig.setString(ChsConfig.KEY_DOWNLOAD_MODE, value)
                    viewModel?.refreshChsUpdateStatus()
                }
                showModeDialog = false
            },
            onDismiss = { showModeDialog = false }
        )
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
