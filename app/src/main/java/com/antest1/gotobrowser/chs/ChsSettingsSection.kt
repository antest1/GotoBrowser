package com.antest1.gotobrowser.chs

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
import com.antest1.gotobrowser.R

/**
 * 第三方 MOD 页面中的「砍口垒汉化计划 in Goto」精简分区（仅包含：启用汉化、下载更新、更多设置）
 */
@Composable
fun ChsSettingsSection(
    viewModel: SettingsViewModel?,
    onChsToggled: (Boolean) -> Unit,
    onOpenMoreSettings: () -> Unit,
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

    // 分组标题：砍口垒汉化计划 in Goto
    ChsGroupHeader(R.string.settings_mod_group_chs)

    // 1. 启用汉化
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

    // 2. 启用汉化后展开：下载更新 & 更多设置
    AnimatedVisibility(visible = chsEnabled) {
        Column(modifier = Modifier.fillMaxWidth()) {
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

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_mod_chs_more), color = mainColor) },
                supportingContent = { Text(stringResource(R.string.settings_mod_chs_more_summary), color = mainColor) },
                modifier = Modifier.clickable(onClick = onOpenMoreSettings)
            )
            HorizontalDivider()
        }
    }
}

/**
 * 「砍口垒汉化计划 in Goto」独立二级菜单页面：
 * 包含数据源切换、下载范围、删除汉化数据、全部 11 项细粒度开关以及关于信息。
 */
@Composable
fun ChsMoreSettingsScreen(
    viewModel: SettingsViewModel?,
    onSettingChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current || viewModel == null

    val chsEnabled = if (isPreview) true else viewModel.isChsPatchEnabled
    val itemColor = if (chsEnabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    // --- 分组 1：数据与更新管理 ---
    ChsGroupHeader(R.string.settings_mod_chs_group_data)

    // 1.1 汉化数据源选择
    val sourceOptions = listOf(
        ChsOption(ChsConfig.SOURCE_GITHUB_RAW, stringResource(R.string.settings_mod_chs_source_github), ChsConfig.URL_GITHUB_RAW),
        ChsOption(ChsConfig.SOURCE_JSDELIVR, stringResource(R.string.settings_mod_chs_source_jsdelivr), ChsConfig.URL_JSDELIVR),
        ChsOption(ChsConfig.SOURCE_GHPROXY, stringResource(R.string.settings_mod_chs_source_ghproxy), ChsConfig.URL_GHPROXY),
        ChsOption(ChsConfig.SOURCE_CUSTOM, stringResource(R.string.settings_mod_chs_source_custom), null)
    )
    var selectedSource by remember(viewModel) {
        mutableStateOf(
            if (isPreview) ChsConfig.SOURCE_GITHUB_RAW
            else ChsConfig.getString(ChsConfig.KEY_DATA_SOURCE, ChsConfig.SOURCE_GITHUB_RAW)
        )
    }
    var showSourceDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_source), color = itemColor) },
        supportingContent = {
            val label = sourceOptions.firstOrNull { it.value == selectedSource }?.title ?: selectedSource
            Text(label, color = itemColor)
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
                    ChsConfig.setString(ChsConfig.KEY_DATA_SOURCE, value)
                    viewModel.refreshChsUpdateStatus()
                }
                showSourceDialog = false
            },
            onDismiss = { showSourceDialog = false }
        )
    }

    // 1.2 自定义镜像源 URL 编辑项
    if (selectedSource == ChsConfig.SOURCE_CUSTOM) {
        var customUrl by remember(viewModel) {
            mutableStateOf(
                if (isPreview) ChsConfig.URL_GITHUB_RAW
                else ChsConfig.getString(ChsConfig.KEY_CUSTOM_SOURCE_URL, ChsConfig.URL_GITHUB_RAW)
            )
        }
        var showCustomUrlDialog by remember { mutableStateOf(false) }
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_mod_chs_custom_url), color = itemColor) },
            supportingContent = { Text(customUrl.ifEmpty { ChsConfig.URL_GITHUB_RAW }, color = itemColor) },
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
                        ChsConfig.setString(ChsConfig.KEY_CUSTOM_SOURCE_URL, normalized)
                        viewModel.refreshChsUpdateStatus()
                    }
                    showCustomUrlDialog = false
                },
                onDismiss = { showCustomUrlDialog = false }
            )
        }
    }

    // 1.3 汉化资源下载范围
    val modeOptions = listOf(
        ChsOption(ChsConfig.DOWNLOAD_MODE_FULL, stringResource(R.string.settings_mod_chs_mode_full), null),
        ChsOption(ChsConfig.DOWNLOAD_MODE_TEXT_ONLY, stringResource(R.string.settings_mod_chs_mode_text_only), null)
    )
    var selectedMode by remember(viewModel) {
        mutableStateOf(
            if (isPreview) ChsConfig.DOWNLOAD_MODE_FULL
            else ChsConfig.getString(ChsConfig.KEY_DOWNLOAD_MODE, ChsConfig.DOWNLOAD_MODE_FULL)
        )
    }
    var showModeDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_download_mode), color = itemColor) },
        supportingContent = {
            val label = modeOptions.firstOrNull { it.value == selectedMode }?.title ?: selectedMode
            Text(label, color = itemColor)
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
                    ChsConfig.setString(ChsConfig.KEY_DOWNLOAD_MODE, value)
                    viewModel.refreshChsUpdateStatus()
                }
                showModeDialog = false
            },
            onDismiss = { showModeDialog = false }
        )
    }

    // 1.4 删除汉化数据
    var showDeleteDialog by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_mod_chs_delete), color = itemColor) },
        supportingContent = { Text(stringResource(R.string.settings_mod_chs_delete_summary), color = itemColor) },
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

    // --- 分组 2：汉化模块细粒度开关 ---
    ChsGroupHeader(R.string.settings_mod_chs_group_modules)

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

    switchItems.forEach { (key, titleRes) ->
        var checked by remember(key) {
            mutableStateOf(if (isPreview) true else ChsConfig.getBoolean(key, true))
        }
        ListItem(
            headlineContent = { Text(stringResource(titleRes), color = itemColor) },
            trailingContent = {
                Switch(
                    checked = checked,
                    enabled = chsEnabled,
                    onCheckedChange = { next ->
                        checked = next
                        if (!isPreview) {
                            ChsConfig.setBoolean(key, next)
                        }
                        onSettingChanged(key)
                    }
                )
            },
            modifier = Modifier.clickable(enabled = chsEnabled) {
                val next = !checked
                checked = next
                if (!isPreview) {
                    ChsConfig.setBoolean(key, next)
                }
                onSettingChanged(key)
            }
        )
        HorizontalDivider()
    }

    // --- 分组 3：关于 ---
    ChsGroupHeader(R.string.settings_mod_chs_group_about)

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
