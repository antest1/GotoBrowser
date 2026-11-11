package com.antest1.gotobrowser.chs

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.antest1.gotobrowser.R

/**
 * 简中汉化 (PLUS) 设置分区 Compose 组件
 * 视觉与交互完全对齐 SettingsBottomSheet 现有规范。
 */
@Composable
fun ChsSettingsSection(
    viewModel: SettingsViewModel?,
    onChsToggled: (Boolean) -> Unit,
    onSettingChanged: (String) -> Unit
) {
    val context = LocalContext.current
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

    // 分组标题
    Text(
        text = stringResource(R.string.settings_mod_group_chs),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(ListItemDefaults.containerColor)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )

    // 1. 汉化总开关
    val mainColor = MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_enable), color = mainColor) },
        supportingContent = { Text(stringResource(R.string.settings_mod_chs_summary), color = mainColor) },
        trailingContent = {
            Switch(
                checked = chsEnabled,
                onCheckedChange = { checked ->
                    chsEnabled = checked
                    if (!isPreview) {
                        viewModel.onChsPatchToggled(checked)
                    }
                    onChsToggled(checked)
                    onSettingChanged(ChsConfig.KEY_ENABLED)
                }
            )
        },
        modifier = Modifier.clickable {
            val next = !chsEnabled
            chsEnabled = next
            if (!isPreview) {
                viewModel.onChsPatchToggled(next)
            }
            onChsToggled(next)
            onSettingChanged(ChsConfig.KEY_ENABLED)
        }
    )
    HorizontalDivider()

    // 2. 汉化数据源选择
    val sourceOptions = listOf(
        ChsOption(ChsConfig.SOURCE_GITHUB_RAW, stringResource(R.string.settings_mod_chs_source_github), ChsConfig.URL_GITHUB_RAW),
        ChsOption(ChsConfig.SOURCE_JSDELIVR, stringResource(R.string.settings_mod_chs_source_jsdelivr), ChsConfig.URL_JSDELIVR),
        ChsOption(ChsConfig.SOURCE_GHPROXY, stringResource(R.string.settings_mod_chs_source_ghproxy), ChsConfig.URL_GHPROXY),
        ChsOption(ChsConfig.SOURCE_CUSTOM, stringResource(R.string.settings_mod_chs_source_custom), null)
    )
    var selectedSource by remember(viewModel) {
        mutableStateOf(
            if (isPreview) ChsConfig.SOURCE_GITHUB_RAW
            else ChsConfig.getString(context, ChsConfig.KEY_DATA_SOURCE, ChsConfig.SOURCE_GITHUB_RAW)
        )
    }
    var showSourceDialog by remember { mutableStateOf(false) }
    val subColor = if (chsEnabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_source), color = subColor) },
        supportingContent = {
            val label = sourceOptions.firstOrNull { it.value == selectedSource }?.title ?: selectedSource
            Text(label, color = subColor)
        },
        modifier = if (chsEnabled) Modifier.clickable { showSourceDialog = true } else Modifier
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
                    ChsConfig.setString(context, ChsConfig.KEY_DATA_SOURCE, value)
                    viewModel.refreshChsUpdateStatus()
                }
                showSourceDialog = false
            },
            onDismiss = { showSourceDialog = false }
        )
    }

    // 2.1 若选择自定义镜像源，显示自定义 URL 编辑项
    if (selectedSource == ChsConfig.SOURCE_CUSTOM) {
        var customUrl by remember(viewModel) {
            mutableStateOf(
                if (isPreview) ChsConfig.URL_GITHUB_RAW
                else ChsConfig.getString(context, ChsConfig.KEY_CUSTOM_SOURCE_URL, ChsConfig.URL_GITHUB_RAW)
            )
        }
        var showCustomUrlDialog by remember { mutableStateOf(false) }
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_mod_chs_custom_url), color = subColor) },
            supportingContent = { Text(customUrl.ifEmpty { ChsConfig.URL_GITHUB_RAW }, color = subColor) },
            modifier = if (chsEnabled) Modifier.clickable { showCustomUrlDialog = true } else Modifier
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
                        ChsConfig.setString(context, ChsConfig.KEY_CUSTOM_SOURCE_URL, normalized)
                        viewModel.refreshChsUpdateStatus()
                    }
                    showCustomUrlDialog = false
                },
                onDismiss = { showCustomUrlDialog = false }
            )
        }
    }

    // 3. 汉化资源下载范围（完整包 vs 仅文本与 API 数据）
    val modeOptions = listOf(
        ChsOption(ChsConfig.DOWNLOAD_MODE_FULL, stringResource(R.string.settings_mod_chs_mode_full), null),
        ChsOption(ChsConfig.DOWNLOAD_MODE_TEXT_ONLY, stringResource(R.string.settings_mod_chs_mode_text_only), null)
    )
    var selectedMode by remember(viewModel) {
        mutableStateOf(
            if (isPreview) ChsConfig.DOWNLOAD_MODE_FULL
            else ChsConfig.getString(context, ChsConfig.KEY_DOWNLOAD_MODE, ChsConfig.DOWNLOAD_MODE_FULL)
        )
    }
    var showModeDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_download_mode), color = subColor) },
        supportingContent = {
            val label = modeOptions.firstOrNull { it.value == selectedMode }?.title ?: selectedMode
            Text(label, color = subColor)
        },
        modifier = if (chsEnabled) Modifier.clickable { showModeDialog = true } else Modifier
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
                    ChsConfig.setString(context, ChsConfig.KEY_DOWNLOAD_MODE, value)
                    viewModel.refreshChsUpdateStatus()
                }
                showModeDialog = false
            },
            onDismiss = { showModeDialog = false }
        )
    }

    // 4. 汉化模块细粒度开关
    var showSubSwitchesDialog by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_sub_switches), color = subColor) },
        supportingContent = { Text(stringResource(R.string.settings_mod_chs_sub_switches_summary), color = subColor) },
        modifier = if (chsEnabled) Modifier.clickable { showSubSwitchesDialog = true } else Modifier
    )
    HorizontalDivider()

    if (showSubSwitchesDialog) {
        ChsSubSwitchesDialog(
            context = context,
            onSettingChanged = onSettingChanged,
            onDismiss = { showSubSwitchesDialog = false }
        )
    }

    // 5. 下载 / 更新汉化数据
    val canClickDownload = chsEnabled && chsDownloadEnabled
    val dlColor = if (canClickDownload) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_download), color = dlColor) },
        supportingContent = { Text(chsSummary, color = dlColor) },
        modifier = if (canClickDownload) Modifier.clickable {
            if (!isPreview) viewModel.requestChsPatchUpdate()
        } else Modifier
    )
    HorizontalDivider()

    // 6. 删除汉化数据
    var showDeleteDialog by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_delete), color = subColor) },
        supportingContent = { Text(stringResource(R.string.settings_mod_chs_delete_summary), color = subColor) },
        modifier = if (chsEnabled) Modifier.clickable { showDeleteDialog = true } else Modifier
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
                    if (!isPreview) viewModel.deleteChsPatch()
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

    // 7. 关于砍口垒汉化计划
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_about)) },
        supportingContent = { Text(ChsConfig.OFFICIAL_GITHUB_URL) },
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ChsConfig.OFFICIAL_GITHUB_URL))
            context.startActivity(intent)
        }
    )
    HorizontalDivider()
}

private data class ChsOption(val value: String, val title: String, val summary: String?)

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
private fun ChsSubSwitchesDialog(
    context: Context,
    onSettingChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val switchItems = listOf(
        ChsConfig.KEY_ENABLE_TEXT_DICT to R.string.settings_mod_chs_text_dict,
        ChsConfig.KEY_ENABLE_TEXT_REGEX to R.string.settings_mod_chs_text_regex,
        ChsConfig.KEY_ENABLE_RES_PATCH to R.string.settings_mod_chs_res_patch,
        ChsConfig.KEY_API_QUEST to R.string.settings_mod_chs_api_quest,
        ChsConfig.KEY_API_SHIP to R.string.settings_mod_chs_api_ship,
        ChsConfig.KEY_API_MISSION to R.string.settings_mod_chs_api_mission,
        ChsConfig.KEY_API_FURNITURE to R.string.settings_mod_chs_api_furniture,
        ChsConfig.KEY_API_MAPINFO to R.string.settings_mod_chs_api_mapinfo,
        ChsConfig.KEY_API_PICTUREBOOK to R.string.settings_mod_chs_api_picturebook,
        ChsConfig.KEY_API_FLAVOR to R.string.settings_mod_chs_api_flavor,
        ChsConfig.KEY_API_ITEM to R.string.settings_mod_chs_api_item
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_mod_chs_sub_switches)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                switchItems.forEach { (key, titleRes) ->
                    var checked by remember(key) {
                        mutableStateOf(ChsConfig.getBoolean(context, key, true))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = !checked
                                checked = next
                                ChsConfig.setBoolean(context, key, next)
                                onSettingChanged(key)
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(titleRes),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Switch(
                            checked = checked,
                            onCheckedChange = { next ->
                                checked = next
                                ChsConfig.setBoolean(context, key, next)
                                onSettingChanged(key)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
        }
    )
}
