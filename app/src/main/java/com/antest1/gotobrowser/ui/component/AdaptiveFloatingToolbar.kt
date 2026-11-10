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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
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

/**
 * The two strips a swipe can start from to reveal the hidden bar, in the toolbar layout's
 * (unpadded) pixels, and whether any of them has room at all.
 *
 * A single full-length strip lies along the docking edge (left in landscape, bottom in
 * portrait) when the game leaves a margin there. Otherwise two strips go in the margins on
 * either side of the game, still along that edge; one may be empty.
 * Both start [navEdge] in from the edge: a hidden navigation bar on that edge comes back with
 * a swipe from there, and that swipe must not reveal the toolbar as well.
 */
data class RevealStrips(val first: GameGeometry.Rect, val second: GameGeometry.Rect, val available: Boolean)

fun revealStrips(
    isLandscape: Boolean,
    containerWidth: Float,
    containerHeight: Float,
    game: GameGeometry.Rect,
    revealSize: Float,
    navEdge: Float
): RevealStrips = if (isLandscape) {
    val full = game.left - navEdge >= revealSize
    val above = game.top.coerceAtLeast(0f)
    val below = (containerHeight - game.bottom).coerceAtLeast(0f)
    RevealStrips(
        first = GameGeometry.Rect(navEdge, 0f, revealSize, if (full) containerHeight else above),
        second = GameGeometry.Rect(navEdge, game.bottom, revealSize, if (full) 0f else below),
        available = full || maxOf(above, below) >= revealSize
    )
} else {
    val full = containerHeight - game.bottom - navEdge >= revealSize
    val left = game.left.coerceAtLeast(0f)
    val right = (containerWidth - game.right).coerceAtLeast(0f)
    val top = containerHeight - revealSize - navEdge
    RevealStrips(
        first = GameGeometry.Rect(0f, top, if (full) containerWidth else left, revealSize),
        second = GameGeometry.Rect(game.right, top, if (full) 0f else right, revealSize),
        available = full || maxOf(left, right) >= revealSize
    )
}

private fun GameGeometry.Rect.fixedConstraints() =
    Constraints.fixed(width.roundToInt().coerceAtLeast(0), height.roundToInt().coerceAtLeast(0))

/** A release at least this fast toward the edge or away from it settles the bar that way. */
private val BarFlingVelocity = 400.dp

/** Release speeds above this are not carried into the settle animation (the bar would overshoot far). */
private val BarMaxSettleVelocity = 1500.dp

/**
 * The settle animation starts at the finger's release [velocity] (reveal direction, px per
 * second) so the bar keeps moving instead of stopping and starting again. Only a velocity
 * toward the target counts, up to [max]: a slow release away from it just starts at rest.
 */
fun settleVelocity(velocity: Float, shown: Boolean, max: Float): Float =
    if (shown) velocity.coerceIn(0f, max) else velocity.coerceIn(-max, 0f)

/**
 * How far from the docking edge the shown bar sits, before its margin: clear of the display
 * cutout and of where the hidden navigation bar appears on that edge (at the bottom in
 * portrait, on the left in reverse landscape with 3-button navigation), so the bar is not
 * covered when that comes up. [navEdge] is 0 when the navigation bar is elsewhere. The insets
 * already hold the visible navigation bar in multi-window, hence the larger of the two rather
 * than their sum.
 */
fun toolbarDockInset(isLandscape: Boolean, insetLeft: Float, insetBottom: Float, navEdge: Float): Float =
    maxOf(if (isLandscape) insetLeft else insetBottom, navEdge)

/** Share of the way between hidden and docked a slow drag must cover to change the bar's state. */
private const val BAR_SETTLE_FRACTION = 0.25f

/**
 * Whether the bar ends up shown when a drag on it or on a reveal strip ends at [offset]
 * (between [hidden] and [docked]). [velocity] is in the reveal direction, in px per second:
 * a fling decides on its own, otherwise a quarter of the way from where the bar started is
 * enough, so a short swipe in from the edge already brings it out.
 */
fun settleShown(offset: Float, hidden: Float, docked: Float, wasVisible: Boolean, velocity: Float, flingVelocity: Float): Boolean {
    if (velocity >= flingVelocity) return true
    if (velocity <= -flingVelocity) return false
    val progress = (offset - hidden) / (docked - hidden)
    return if (wasVisible) progress > 1f - BAR_SETTLE_FRACTION else progress >= BAR_SETTLE_FRACTION
}

/**
 * Drags along the bar's axis. [onDrag] gets each step and [onDragEnd] the release velocity
 * (px per second), both in the reveal direction. The velocity is tracked on the summed drag
 * rather than on positions, since the bar moves along under the finger.
 */
private fun Modifier.barDrag(isLandscape: Boolean, onDrag: (Float) -> Unit, onDragEnd: (Float) -> Unit): Modifier =
    pointerInput(isLandscape) {
        val tracker = VelocityTracker1D(isDataDifferential = false)
        var total = 0f
        fun step(change: PointerInputChange, amount: Float) {
            change.consume()
            total += amount
            tracker.addDataPoint(change.uptimeMillis, total)
            onDrag(amount)
        }
        var dragging = false
        val start = { _: Offset -> tracker.resetTracking(); total = 0f; dragging = true }
        val end = { velocity: Float -> dragging = false; onDragEnd(velocity) }
        try {
            if (isLandscape) {
                detectHorizontalDragGestures(
                    onDragStart = start,
                    onHorizontalDrag = { change, dragAmount -> step(change, dragAmount) },
                    onDragEnd = { end(tracker.calculateVelocity()) },
                    onDragCancel = { end(0f) }
                )
            } else {
                // Dragging up (negative dragAmount) reveals the bar.
                detectVerticalDragGestures(
                    onDragStart = start,
                    onVerticalDrag = { change, dragAmount -> step(change, -dragAmount) },
                    onDragEnd = { end(tracker.calculateVelocity()) },
                    onDragCancel = { end(0f) }
                )
            }
        } finally {
            // The gesture was cut off without an end or a cancel (e.g. its strip left the
            // composition): settle anyway, or the bar would stay where the finger left it.
            if (dragging) onDragEnd(0f)
        }
    }

/** Android honors at most this much system gesture exclusion per screen edge. */
private val MaxGestureExclusionLength = 200.dp

/**
 * Keeps the system back gesture off the part of a landscape reveal strip next to where the
 * bar docks, so the swipe reaches the strip. The rest of the edge still works as back.
 * A full-height strip excludes its center; split strips (above/below the game) share the
 * budget and exclude the end nearest the screen center.
 * With [topAlignedFromPx] (the bar docks at the top, starting there) the part from that
 * point down is excluded instead, all of it from the upper strip when the strips are split.
 */
private fun Modifier.revealGestureExclusion(id: String, maxLengthPx: Float, topAlignedFromPx: Float?): Modifier =
    systemGestureExclusion { coords ->
        val width = coords.size.width.toFloat()
        val height = coords.size.height.toFloat()
        val parentHeight = coords.parentLayoutCoordinates?.size?.height?.toFloat() ?: height
        val fullHeight = height >= parentHeight - 1f
        if (topAlignedFromPx != null) {
            if (!fullHeight && id != RevealStrip1LayoutId) return@systemGestureExclusion Rect(0f, 0f, 0f, 0f)
            val length = minOf(height, maxLengthPx)
            val top = topAlignedFromPx.coerceIn(0f, height - length)
            return@systemGestureExclusion Rect(0f, top, width, top + length)
        }
        val length = minOf(height, if (fullHeight) maxLengthPx else maxLengthPx / 2f)
        val top = when {
            fullHeight -> (height - length) / 2f
            id == RevealStrip1LayoutId -> height - length
            else -> 0f
        }
        Rect(0f, top, width, top + length)
    }

@OptIn(ExperimentalLayoutApi::class)
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
    /**
     * Landscape only: dock at the top, [topInset] below the safe area, with the [fab] above
     * the bar (3.x placed refresh first in its panel), instead of centered with the fab below.
     */
    alignTop: Boolean = false,
    topInset: Dp = 0.dp,
    /**
     * Whether a swipe in from the docking edge reveals the hidden bar. Off, no reveal strips
     * are laid out, so that edge is left entirely to the system gestures (back, home).
     */
    swipeToReveal: Boolean = true,
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
    // The hidden navigation bar comes back with a swipe in from its edge. When that is the edge
    // the bar docks against (3-button navigation in reverse landscape, or at the bottom in
    // portrait), the reveal strips start past it, so that swipe shows the navigation bar only.
    val navigationBars = WindowInsets.navigationBarsIgnoringVisibility
    val navEdgePx = (if (isLandscape) navigationBars.getLeft(density, layoutDirection)
        else navigationBars.getBottom(density)).toFloat()
    val dockInsetPx = toolbarDockInset(isLandscape, insetLeftPx, insetBottomPx, navEdgePx)

    // Offset logic
    // Vertical: slides horizontally from left (-barSize to inset + margin)
    // Horizontal: slides vertically from bottom (-barSize to inset + margin)
    val dockedOffset = marginPx + dockInsetPx
    val hiddenOffset = -barSizePx
    val flingVelocityPx = with(density) { BarFlingVelocity.toPx() }
    val maxSettleVelocityPx = with(density) { BarMaxSettleVelocity.toPx() }

    val currentVisible by rememberUpdatedState(visible)
    val currentOnVisibleChange by rememberUpdatedState(onVisibleChange)
    val currentOnRevealAvailableChange by rememberUpdatedState(onRevealAvailableChange)
    val gestureExclusionMaxPx = with(density) { MaxGestureExclusionLength.toPx() }
    // Where a top-aligned bar group starts, in this layout's (unpadded) coordinates.
    val topAlignedStartPx = insetTopPx + with(density) { topInset.toPx() }
    val alignTopInLandscape = alignTop && isLandscape

    val offsetAnim = remember { Animatable(if (visible) dockedOffset else hiddenOffset) }

    val settleSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // While a finger drags the bar, it is drawn at this offset (NaN otherwise). Each step
    // updates it right away, with no coroutine in between, and an animation started meanwhile
    // (a show/hide toggle, new insets) cannot pull the bar away from the finger.
    var dragOffset by remember { mutableFloatStateOf(Float.NaN) }
    fun barOffset() = if (dragOffset.isNaN()) offsetAnim.value else dragOffset

    // Called with the drag in the reveal direction (right in landscape, up in portrait).
    fun dragBy(amount: Float) {
        dragOffset = (barOffset() + amount).coerceIn(hiddenOffset, dockedOffset)
    }

    // The drag ended: hand the position back to the animation, which settles the bar.
    fun settle(velocity: Float) {
        val from = barOffset()
        val shouldShow = settleShown(from, hiddenOffset, dockedOffset, currentVisible, velocity, flingVelocityPx)
        coroutineScope.launch {
            offsetAnim.snapTo(from)
            dragOffset = Float.NaN
            if (shouldShow != currentVisible) currentOnVisibleChange(shouldShow)
            offsetAnim.animateTo(
                targetValue = if (shouldShow) dockedOffset else hiddenOffset,
                animationSpec = settleSpec,
                initialVelocity = settleVelocity(velocity, shouldShow, maxSettleVelocityPx)
            )
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
            if (!visible && swipeToReveal) {
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
                            .then(if (isLandscape) Modifier.revealGestureExclusion(id, gestureExclusionMaxPx, topAlignedStartPx.takeIf { alignTopInLandscape }) else Modifier)
                            .barDrag(isLandscape, ::dragBy, ::settle)
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
                                Modifier.offset { IntOffset(barOffset().roundToInt(), 0) }.size(barSize)
                            } else {
                                Modifier.offset { IntOffset(0, -barOffset().roundToInt()) }.size(barSize)
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
                            Modifier.offset { IntOffset(barOffset().roundToInt(), 0) }.width(barSize)
                        } else {
                            Modifier.offset { IntOffset(0, -barOffset().roundToInt()) }.height(barSize)
                        }
                    )
                    .glassyStyle(RoundedCornerShape(barSize / 2))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    .barDrag(isLandscape, ::dragBy, ::settle),
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

        val revealSizePx = with(density) { revealSize.toPx() }
        val strips = revealStrips(isLandscape, containerWidth, containerHeight, game, revealSizePx, navEdgePx)
        currentOnRevealAvailableChange(strips.available)
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
                RevealStrip1LayoutId -> measurable.measure(strips.first.fixedConstraints())
                RevealStrip2LayoutId -> measurable.measure(strips.second.fixedConstraints())
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
                            val fabExtent = if (fabPlaceable != null) fabPlaceable.height + fabSpacingPx.roundToInt() else 0
                            if (alignTopInLandscape) {
                                placeable.place(x = 0, y = topAlignedStartPx.roundToInt() + fabExtent)
                            } else {
                                placeable.place(x = 0, y = (constraints.maxHeight - placeable.height - fabExtent) / 2)
                            }
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
                            placeable.place(x = 0, y = if (alignTopInLandscape) topAlignedStartPx.roundToInt() else startY + barHeight + fabSpacingPx.roundToInt())
                        } else {
                            val barWidth = barPlaceable?.width ?: 0
                            val totalWidth = barWidth + placeable.width + fabSpacingPx.roundToInt()
                            val startX = (constraints.maxWidth - totalWidth) / 2
                            placeable.place(x = startX, y = constraints.maxHeight - placeable.height)
                        }
                    }
                    RevealStrip1LayoutId -> placeable.place(strips.first.left.roundToInt(), strips.first.top.roundToInt())
                    RevealStrip2LayoutId -> placeable.place(strips.second.left.roundToInt(), strips.second.top.roundToInt())
                }
            }
        }
    }
}