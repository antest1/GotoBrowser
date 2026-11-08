package com.antest1.gotobrowser.ui.component

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.antest1.gotobrowser.Activity.safeOverlayInsets
import com.antest1.gotobrowser.Browser.GameGeometry
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val RevealStrip1LayoutId = "reveal_strip_1"
private const val RevealStrip2LayoutId = "reveal_strip_2"
private const val BarLayoutId = "bar"
private const val FabLayoutId = "fab"

/**
 * Distance between the floating toolbar and the screen edge it hugs:
 * 16dp in portrait (horizontal bar), 24dp in landscape (vertical bar).
 * The right-side corner buttons (settings, capture) use the same value so both edges match.
 */
@Composable
fun floatingToolbarEdgeMargin(): Dp {
    val configuration = LocalConfiguration.current
    return if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 24.dp else 16.dp
}

/** Android honors at most this much system gesture exclusion per screen edge. */
private val MaxGestureExclusionLength = 200.dp

/**
 * Keeps the system back gesture off the part of a landscape reveal strip next to where the
 * bar docks, so the swipe reaches the strip. The rest of the edge still works as back.
 * A full-height strip excludes its center; split strips (above/below the game) share the
 * budget and exclude the end nearest the screen center.
 */
private fun Modifier.revealGestureExclusion(id: String, maxLengthPx: Float): Modifier =
    systemGestureExclusion { coords ->
        val width = coords.size.width.toFloat()
        val height = coords.size.height.toFloat()
        val parentHeight = coords.parentLayoutCoordinates?.size?.height?.toFloat() ?: height
        val fullHeight = height >= parentHeight - 1f
        val length = minOf(height, if (fullHeight) maxLengthPx else maxLengthPx / 2f)
        val top = when {
            fullHeight -> (height - length) / 2f
            id == RevealStrip1LayoutId -> height - length
            else -> 0f
        }
        Rect(0f, top, width, top + length)
    }

@Composable
fun AdaptiveFloatingToolbar(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    /** Reports whether a reveal strip has room on screen (the game leaves a margin to swipe from). */
    onRevealAvailableChange: (Boolean) -> Unit = {},
    /** Matches GameAreaLayout.verticalBias; see GameGeometry.fit. */
    gameVerticalBias: Float = GameGeometry.BIAS_CENTER,
    modifier: Modifier = Modifier,
    barSize: Dp = 56.dp,
    barLengthFraction: Float = 0.7f,
    revealSize: Dp = 24.dp,
    contentSpacing: Dp = 8.dp,
    /** Padding along the bar's main axis, before the first and after the last item. */
    contentInset: Dp = 8.dp,
    elevation: Dp = 0.dp,
    fab: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val margin = floatingToolbarEdgeMargin()
    val marginPx = with(density) { margin.toPx() }
    val barSizePx = with(density) { barSize.toPx() }

    // Display cutout / system bar insets on the edge the bar docks against.
    // The bar is positioned in unpadded (physical screen) coordinates so that
    // the hidden state is really off-screen; the docked state is pushed in by
    // the inset so it does not sit under the camera hole.
    val layoutDirection = LocalLayoutDirection.current
    val safeInsets = safeOverlayInsets()
    val insetLeftPx = safeInsets.getLeft(density, layoutDirection).toFloat()
    val insetTopPx = safeInsets.getTop(density).toFloat()
    val insetRightPx = safeInsets.getRight(density, layoutDirection).toFloat()
    val insetBottomPx = safeInsets.getBottom(density).toFloat()
    val dockInsetPx = if (isLandscape) insetLeftPx else insetBottomPx

    // Offset logic
    // Vertical: slides horizontally from left (-barSize to inset + margin)
    // Horizontal: slides vertically from bottom (-barSize to inset + margin)
    val dockedOffset = marginPx + dockInsetPx
    val hiddenOffset = -barSizePx
    val triggerOffset = (dockedOffset + hiddenOffset) / 2f

    val currentVisible by rememberUpdatedState(visible)
    val currentOnVisibleChange by rememberUpdatedState(onVisibleChange)
    val currentOnRevealAvailableChange by rememberUpdatedState(onRevealAvailableChange)
    val gestureExclusionMaxPx = with(density) { MaxGestureExclusionLength.toPx() }

    val offsetAnim = remember { Animatable(if (visible) dockedOffset else hiddenOffset) }

    val settleSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun settle() {
        val shouldShow = offsetAnim.value > triggerOffset
        if (shouldShow != currentVisible) {
            currentOnVisibleChange(shouldShow)
        } else {
            coroutineScope.launch {
                offsetAnim.animateTo(
                    targetValue = if (shouldShow) dockedOffset else hiddenOffset,
                    animationSpec = settleSpec
                )
            }
        }
    }

    // Keyed on the target offsets as well as visibility: window insets usually
    // arrive after the first composition, so dockedOffset changes once the
    // cutout is known. Without these keys the bar would stay at its inset-less
    // initial position until the next show/hide toggle.
    LaunchedEffect(visible, dockedOffset, hiddenOffset) {
        offsetAnim.animateTo(
            targetValue = if (visible) dockedOffset else hiddenOffset,
            animationSpec = settleSpec
        )
    }

    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            if (!visible) {
                val createStrip = @Composable { id: String ->
                    Box(
                        modifier = Modifier
                            .layoutId(id)
                            .then(
                                if (isLandscape) Modifier.width(revealSize).fillMaxHeight()
                                else Modifier.fillMaxWidth().height(revealSize)
                            )
                            // The left edge is also the system back gesture area. The bottom
                            // edge (portrait) is the home gesture, which cannot be excluded.
                            .then(if (isLandscape) Modifier.revealGestureExclusion(id, gestureExclusionMaxPx) else Modifier)
                            .pointerInput(isLandscape) {
                                if (isLandscape) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { coroutineScope.launch { offsetAnim.stop() } },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            coroutineScope.launch {
                                                offsetAnim.snapTo((offsetAnim.value + dragAmount).coerceIn(hiddenOffset, dockedOffset))
                                            }
                                        },
                                        onDragEnd = { settle() },
                                        onDragCancel = { settle() }
                                    )
                                } else {
                                    detectVerticalDragGestures(
                                        onDragStart = { coroutineScope.launch { offsetAnim.stop() } },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            // Dragging up (negative dragAmount) reveals the bar
                                            coroutineScope.launch {
                                                offsetAnim.snapTo((offsetAnim.value - dragAmount).coerceIn(hiddenOffset, dockedOffset))
                                            }
                                        },
                                        onDragEnd = { settle() },
                                        onDragCancel = { settle() }
                                    )
                                }
                            }
                    )
                }
                createStrip(RevealStrip1LayoutId)
                createStrip(RevealStrip2LayoutId)
            }

            if (fab != null) {
                Box(
                    modifier = Modifier
                        .layoutId(FabLayoutId)
                        .zIndex(1f)
                        .then(
                            if (isLandscape) {
                                Modifier.offset { IntOffset(offsetAnim.value.roundToInt(), 0) }.size(barSize)
                            } else {
                                Modifier.offset { IntOffset(0, -offsetAnim.value.roundToInt()) }.size(barSize)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    fab()
                }
            }

            Surface(
                modifier = Modifier
                    .layoutId(BarLayoutId)
                    .then(
                        if (isLandscape) {
                            Modifier.offset { IntOffset(offsetAnim.value.roundToInt(), 0) }.width(barSize)
                        } else {
                            Modifier.offset { IntOffset(0, -offsetAnim.value.roundToInt()) }.height(barSize)
                        }
                    )
                    .glassyStyle(RoundedCornerShape(barSize / 2))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    .pointerInput(isLandscape) {
                        if (isLandscape) {
                            detectHorizontalDragGestures(
                                onDragStart = { coroutineScope.launch { offsetAnim.stop() } },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    coroutineScope.launch {
                                        offsetAnim.snapTo((offsetAnim.value + dragAmount).coerceIn(hiddenOffset, dockedOffset))
                                    }
                                },
                                onDragEnd = { settle() },
                                onDragCancel = { settle() }
                            )
                        } else {
                            detectVerticalDragGestures(
                                onDragStart = { coroutineScope.launch { offsetAnim.stop() } },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    coroutineScope.launch {
                                        offsetAnim.snapTo((offsetAnim.value - dragAmount).coerceIn(hiddenOffset, dockedOffset))
                                    }
                                },
                                onDragEnd = { settle() },
                                onDragCancel = { settle() }
                            )
                        }
                    },
                shape = RoundedCornerShape(barSize / 2),
                color = Color.Transparent,
                shadowElevation = elevation
            ) {
                if (isLandscape) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = contentInset),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(contentSpacing, Alignment.CenterVertically)
                    ) { content() }
                } else {
                    Row(
                        modifier = Modifier.fillMaxHeight().horizontalScroll(rememberScrollState()).padding(horizontal = contentInset),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(contentSpacing, Alignment.CenterHorizontally)
                    ) { content() }
                }
            }
        }
    ) { measurables, constraints ->
        val maxBarLength = ((if (isLandscape) constraints.maxHeight else constraints.maxWidth) * barLengthFraction).roundToInt()

        // WebView geometry (simplified dodge logic for now, similar to original).
        // GameAreaLayout pads itself by the same insets and places the game the same way,
        // in screen coordinates since this layout is not inset.
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val game = GameGeometry.fit(
            containerWidth, containerHeight, gameVerticalBias,
            insetLeftPx, insetTopPx, insetRightPx, insetBottomPx
        )
        val wvl = game.left
        val wvt = game.top
        val wvr = game.right
        val wvb = game.bottom

        val revealSizePx = with(density) { revealSize.toPx() }
        // Mirrors the strip sizing below: a full-length strip along the edge when the game
        // leaves a margin there, otherwise strips in the margins on either side of the game.
        currentOnRevealAvailableChange(
            if (isLandscape) wvl >= revealSizePx || maxOf(wvt, containerHeight - wvb) >= revealSizePx
            else containerHeight - wvb >= revealSizePx || maxOf(wvl, containerWidth - wvr) >= revealSizePx
        )
        val fabSpacingPx = with(density) { 16.dp.toPx() }
        val barSizePx = with(density) { barSize.toPx() }

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { measurable ->
            when (measurable.layoutId) {
                BarLayoutId -> {
                    if (isLandscape) measurable.measure(looseConstraints.copy(maxHeight = maxBarLength))
                    else measurable.measure(looseConstraints.copy(maxWidth = maxBarLength))
                }
                FabLayoutId -> {
                    // Cap the minimum by the container so a very small window
                    // (split screen, resizing) cannot produce min > max.
                    val fabMin = barSizePx.roundToInt()
                    measurable.measure(looseConstraints.copy(
                        minWidth = fabMin.coerceAtMost(constraints.maxWidth),
                        minHeight = fabMin.coerceAtMost(constraints.maxHeight)
                    ))
                }
                RevealStrip1LayoutId -> {
                    if (isLandscape) {
                        val heightCap = if (wvl < revealSizePx) wvt.roundToInt().coerceAtLeast(0) else constraints.maxHeight
                        measurable.measure(looseConstraints.copy(maxHeight = heightCap))
                    } else {
                        val widthCap = if (containerHeight - wvb < revealSizePx) wvl.roundToInt().coerceAtLeast(0) else constraints.maxWidth
                        measurable.measure(looseConstraints.copy(maxWidth = widthCap))
                    }
                }
                RevealStrip2LayoutId -> {
                    if (isLandscape) {
                        val heightCap = if (wvl < revealSizePx) (constraints.maxHeight - wvb.roundToInt()).coerceAtLeast(0) else 0
                        measurable.measure(looseConstraints.copy(maxHeight = heightCap))
                    } else {
                        val widthCap = if (containerHeight - wvb < revealSizePx) (constraints.maxWidth - wvr.roundToInt()).coerceAtLeast(0) else 0
                        measurable.measure(looseConstraints.copy(maxWidth = widthCap))
                    }
                }
                else -> measurable.measure(looseConstraints)
            }
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val barPlaceable = placeables.find { measurables[placeables.indexOf(it)].layoutId == BarLayoutId }
            val fabPlaceable = placeables.find { measurables[placeables.indexOf(it)].layoutId == FabLayoutId }

            measurables.zip(placeables).forEach { (measurable, placeable) ->
                when (measurable.layoutId) {
                    BarLayoutId -> {
                        if (isLandscape) {
                            val totalHeight = placeable.height + (fabPlaceable?.height ?: 0) + (if (fabPlaceable != null) fabSpacingPx.roundToInt() else 0)
                            val startY = (constraints.maxHeight - totalHeight) / 2
                            placeable.place(x = 0, y = startY)
                        } else {
                            val totalWidth = placeable.width + (fabPlaceable?.width ?: 0) + (if (fabPlaceable != null) fabSpacingPx.roundToInt() else 0)
                            val startX = (constraints.maxWidth - totalWidth) / 2
                            val toolbarX = if (fabPlaceable != null) startX + fabPlaceable.width + fabSpacingPx.roundToInt() else startX
                            placeable.place(x = toolbarX, y = constraints.maxHeight - placeable.height)
                        }
                    }
                    FabLayoutId -> {
                        if (isLandscape) {
                            val barHeight = barPlaceable?.height ?: 0
                            val totalHeight = barHeight + placeable.height + fabSpacingPx.roundToInt()
                            val startY = (constraints.maxHeight - totalHeight) / 2
                            placeable.place(x = 0, y = startY + barHeight + fabSpacingPx.roundToInt())
                        } else {
                            val barWidth = barPlaceable?.width ?: 0
                            val totalWidth = barWidth + placeable.width + fabSpacingPx.roundToInt()
                            val startX = (constraints.maxWidth - totalWidth) / 2
                            placeable.place(x = startX, y = constraints.maxHeight - placeable.height)
                        }
                    }
                    RevealStrip1LayoutId -> {
                        if (isLandscape) placeable.place(x = 0, y = 0)
                        else placeable.place(x = 0, y = constraints.maxHeight - placeable.height)
                    }
                    RevealStrip2LayoutId -> {
                        if (isLandscape) placeable.place(x = 0, y = wvb.roundToInt())
                        else placeable.place(x = wvr.roundToInt(), y = constraints.maxHeight - placeable.height)
                    }
                }
            }
        }
    }
}