package com.antest1.gotobrowser.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antest1.gotobrowser.Activity.LoadingTracker
import com.antest1.gotobrowser.Constants.CONN_DMM
import com.antest1.gotobrowser.Constants.CONN_OOI
import com.antest1.gotobrowser.Helpers.KcUtils
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.ui.theme.SettingsTheme
import com.antest1.gotobrowser.ui.theme.StatusErrorColor
import com.antest1.gotobrowser.ui.theme.StatusWarningColor
import kotlinx.coroutines.delay

/** A kcsapi call that went wrong during play, shown once at the top of the game area. */
data class ServerNotice(val kind: Kind, val code: String, val api: String, val message: String) {
    enum class Kind {
        /** api_result is not 1: only a full refresh recovers. */
        REJECTED,
        /** No response: the connection dropped or timed out. */
        NETWORK,
        /** The server answered with an HTTP error. */
        HTTP
    }

    /** One line for bug reports (long-press copies it). */
    val detail: String get() = listOf(code, api, message).filter { it.isNotBlank() }.joinToString(" · ")
}

private const val SHOW_DELAY_MILLIS = 500L
private const val POLL_MILLIS = 500L
private const val STEP_COUNT = 3

/** Prefix for every loading card title, naming the connector being loaded. */
private val LocalConnectorTag = staticCompositionLocalOf<String?> { null }

/** The tag shown in the loading card title for a PREF_CONNECTOR value, or null for none. */
fun loadingConnectorTag(connector: String?): String? = when (connector) {
    CONN_DMM -> "DMM"
    CONN_OOI -> "OOI"
    else -> null
}

/** Polls [tracker]; equal snapshots do not recompose, so this is idle once loading ends. */
@Composable
fun rememberLoadingSnapshot(tracker: LoadingTracker): State<LoadingTracker.Snapshot> =
    produceState(tracker.snapshot(), tracker) {
        while (true) {
            value = tracker.snapshot()
            delay(POLL_MILLIS)
        }
    }

/** Ring color for the refresh FAB, or null when nothing needs the user's attention. */
fun refreshAttentionColor(snapshot: LoadingTracker.Snapshot, hasServerNotice: Boolean): Color? = when {
    hasServerNotice || snapshot.failure != null -> StatusErrorColor
    snapshot.stalledSeconds > 0 -> StatusWarningColor
    else -> null
}

/** Quick refresh cannot recover a failed page load or a rejected kcsapi call. */
fun requiresFullRefresh(snapshot: LoadingTracker.Snapshot, notice: ServerNotice?): Boolean =
    notice?.kind == ServerNotice.Kind.REJECTED || snapshot.failure != null

/**
 * Loading / stalled / failed status, drawn inside the game area. Appears only after
 * [SHOW_DELAY_MILLIS] so fast loads never flash it. Guidance only: the action is the
 * existing refresh FAB, which [refreshAttentionColor] highlights.
 */
@Composable
fun LoadingStatusOverlay(
    snapshot: LoadingTracker.Snapshot,
    modifier: Modifier = Modifier,
    /** See [loadingConnectorTag]. */
    connectorTag: String? = null
) {
    val active = snapshot.isLoading
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (active) {
            delay(SHOW_DELAY_MILLIS)
            armed = true
        } else {
            armed = false
        }
    }
    // Keep drawing the last loading state while the card fades out.
    var shown by remember { mutableStateOf(snapshot) }
    SideEffect { if (active) shown = snapshot }

    val compact = shown.isCompact()
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = active && armed,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(400)),
            modifier = Modifier
                .align(if (compact) Alignment.BottomCenter else Alignment.Center)
                .padding(horizontal = 16.dp, vertical = if (compact) 16.dp else 0.dp)
        ) {
            CompositionLocalProvider(LocalConnectorTag provides connectorTag) {
                StatusCard(shown, compact)
            }
        }
    }
}

/** While resources download the game draws its own loading screen, so the card shrinks to the bottom. */
private fun LoadingTracker.Snapshot.isCompact() =
    stage == LoadingTracker.Stage.DOWNLOADING && stalledSeconds == 0 && failure == null

/**
 * Drawn like an M3 Surface (surfaceContainerHigh, large shape, level-1 shadow) but without
 * one: a Surface consumes touches, and the card must never stop the page below it from
 * being used (a login form, an error page).
 */
@Composable
private fun StatusCard(snapshot: LoadingTracker.Snapshot, compact: Boolean) {
    SettingsTheme(darkTheme = true) {
        val shape = MaterialTheme.shapes.large
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                Modifier
                    // Same width when compact: a long title ("[DMM] ゲームリソースをダウンロード中")
                    // plus the request count must still fit on one line.
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .shadow(3.dp, shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                    .padding(horizontal = 16.dp, vertical = if (compact) 12.dp else 16.dp)
            ) {
                val failure = snapshot.failure
                when {
                    failure != null -> FailureContent(snapshot, failure)
                    snapshot.stalledSeconds > 0 -> StalledContent(snapshot)
                    else -> ProgressContent(snapshot, compact)
                }
            }
        }
    }
}

@Composable
private fun ProgressContent(snapshot: LoadingTracker.Snapshot, compact: Boolean) {
    when (snapshot.stage) {
        LoadingTracker.Stage.PREPARING -> {
            Header(stringResource(R.string.loading_preparing_title), trailing = stepText(snapshot))
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            Supporting(stringResource(R.string.loading_preparing_desc))
        }
        LoadingTracker.Stage.CONNECTING -> {
            val progress = snapshot.progress
            val known = progress in 1..99
            val title = stringResource(R.string.loading_connecting_title)
            Header(if (known) "$title · $progress%" else title, trailing = stepText(snapshot))
            if (known) {
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            }
            Supporting(stringResource(R.string.loading_connecting_desc))
        }
        else -> {
            Header(
                stringResource(R.string.loading_resources_title),
                trailing = stringResource(R.string.loading_resources_count, snapshot.requestCount)
            )
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = if (compact) 10.dp else 12.dp))
        }
    }
}

@Composable
private fun StalledContent(snapshot: LoadingTracker.Snapshot) {
    Header(
        stringResource(R.string.loading_stalled_title, snapshot.stalledSeconds),
        trailing = stepText(snapshot),
        leading = {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = StatusWarningColor, modifier = Modifier.size(20.dp))
        }
    )
    // Frozen midway through the step it stopped in (no real % here), so the track stays visible.
    LinearProgressIndicator(
        progress = { ((stepOf(snapshot) ?: STEP_COUNT) - 0.5f) / STEP_COUNT },
        color = StatusWarningColor,
        trackColor = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
    )
    Supporting(stringResource(R.string.loading_stalled_desc))
}

@Composable
private fun FailureContent(snapshot: LoadingTracker.Snapshot, failure: LoadingTracker.Failure) {
    val error = MaterialTheme.colorScheme.error
    val (title, description) = failureTexts(failure)
    Header(
        title,
        trailing = stepText(snapshot),
        titleColor = error,
        leading = { ErrorBadge(color = error, onColor = MaterialTheme.colorScheme.onError) }
    )
    Supporting(description, topPadding = 8.dp)
    DetailBlock(
        rows = listOf(
            stringResource(R.string.detail_code) to failure.code,
            stringResource(R.string.detail_host) to failure.host.orEmpty()
        ),
        dividerColor = MaterialTheme.colorScheme.outlineVariant,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        valueColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Technical details as their own section under a divider: one labelled row per value,
 * wrapping instead of truncating, so a long host or message is still readable.
 */
@Composable
private fun DetailBlock(rows: List<Pair<String, String>>, dividerColor: Color, labelColor: Color, valueColor: Color) {
    val shown = rows.filter { it.second.isNotBlank() }
    if (shown.isEmpty()) return
    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        shown.forEach { (label, value) ->
            Row {
                // Wide enough for the longest label ("メッセージ") on one line.
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(72.dp).padding(top = 1.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = valueColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun failureTexts(failure: LoadingTracker.Failure): Pair<String, String> = when (failure) {
    is LoadingTracker.Failure.Network -> when (failure.errorCode) {
        WebViewClient.ERROR_HOST_LOOKUP, WebViewClient.ERROR_CONNECT, WebViewClient.ERROR_IO ->
            stringResource(R.string.loading_error_connect_title) to stringResource(R.string.loading_error_connect_desc)
        WebViewClient.ERROR_TIMEOUT ->
            stringResource(R.string.loading_error_timeout_title) to stringResource(R.string.loading_error_timeout_desc)
        else ->
            stringResource(R.string.loading_error_generic_title) to stringResource(R.string.loading_error_generic_desc)
    }
    is LoadingTracker.Failure.Http -> when {
        failure.statusCode == 403 ->
            stringResource(R.string.loading_error_forbidden_title, failure.statusCode) to stringResource(R.string.loading_error_forbidden_desc)
        failure.statusCode >= 500 ->
            stringResource(R.string.loading_error_server_title, failure.statusCode) to stringResource(R.string.loading_error_server_desc)
        else ->
            stringResource(R.string.loading_error_generic_title) to stringResource(R.string.loading_error_generic_desc)
    }
}

private fun stepOf(snapshot: LoadingTracker.Snapshot): Int? = when (snapshot.stage) {
    LoadingTracker.Stage.PREPARING -> 1
    LoadingTracker.Stage.CONNECTING -> 2
    LoadingTracker.Stage.DOWNLOADING -> 3
    else -> null
}

private fun stepText(snapshot: LoadingTracker.Snapshot): String? = stepOf(snapshot)?.let { "$it/$STEP_COUNT" }

@Composable
private fun Header(
    title: String,
    trailing: String?,
    titleColor: Color = Color.Unspecified,
    leading: (@Composable () -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        val tag = LocalConnectorTag.current
        Text(
            text = if (tag != null) "[$tag] $title" else title,
            style = MaterialTheme.typography.titleSmall,
            color = titleColor,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun Supporting(text: String, topPadding: androidx.compose.ui.unit.Dp = 10.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = topPadding)
    )
}

/** Filled circle with "!", the shape of the Material "error" symbol (not in material-icons-core). */
@Composable
private fun ErrorBadge(color: Color, onColor: Color) {
    Box(
        modifier = Modifier.size(20.dp).background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("!", color = onColor, fontSize = 13.sp, fontWeight = FontWeight.Black, lineHeight = 13.sp)
    }
}

/**
 * Where [ServerNoticeCard] sits in the game area: a little below the middle, so the card
 * starts under the message at the top of the game's own connection error screen.
 */
val ServerNoticeAlignment: Alignment = BiasAlignment(0f, 0.4f)

/**
 * A failed kcsapi call, as a card in the middle of the game area: the same look as a load
 * error, and clear of the game's own error message, which sits at the top of its error
 * screen. No action button: tap to dismiss, long-press to copy the detail line
 * (code · api · server message) for bug reports.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerNoticeCard(notice: ServerNotice?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(notice) }
    SideEffect { if (notice != null) shown = notice }

    AnimatedVisibility(
        visible = notice != null,
        enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.96f),
        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.96f),
        modifier = modifier
    ) {
        val current = notice ?: shown ?: return@AnimatedVisibility
        val (title, description) = when (current.kind) {
            ServerNotice.Kind.REJECTED -> R.string.server_notice_title to R.string.server_notice_desc
            ServerNotice.Kind.NETWORK -> R.string.notice_network_title to R.string.notice_network_desc
            ServerNotice.Kind.HTTP -> R.string.notice_http_title to R.string.notice_http_desc
        }
        SettingsTheme(darkTheme = true) {
            val context = LocalContext.current
            val shape = MaterialTheme.shapes.large
            val error = MaterialTheme.colorScheme.error
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                Column(
                    Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .shadow(3.dp, shape)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .combinedClickable(
                            onClick = onDismiss,
                            onLongClick = { copyToClipboard(context, current.detail) }
                        )
                        .padding(16.dp)
                ) {
                    Header(
                        stringResource(title),
                        trailing = null,
                        titleColor = error,
                        leading = { ErrorBadge(color = error, onColor = MaterialTheme.colorScheme.onError) }
                    )
                    Supporting(stringResource(description), topPadding = 8.dp)
                    DetailBlock(
                        rows = listOf(
                            stringResource(R.string.detail_code) to current.code,
                            stringResource(R.string.detail_api) to current.api,
                            stringResource(R.string.detail_message) to current.message
                        ),
                        dividerColor = MaterialTheme.colorScheme.outlineVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("GotoBrowser", text))
    // Android 13+ shows its own confirmation.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        KcUtils.showToast(context, R.string.notice_copied)
    }
}
