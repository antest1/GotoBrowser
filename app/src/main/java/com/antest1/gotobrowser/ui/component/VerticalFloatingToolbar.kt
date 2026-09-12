package com.antest1.gotobrowser.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val RevealStripLayoutId = "reveal_strip"
private const val BarLayoutId = "bar"

/**
 * A vertical floating toolbar docked to the left edge of its parent.
 *
 * Behavior:
 *  - When [visible] is true, the bar is docked near the left edge, separated
 *    from the screen edge by [margin].
 *  - When [visible] is false, the bar rests off-screen to the left, and a thin
 *    strip along the left edge can be swiped to the right to reveal it.
 *  - The bar can be dragged horizontally: its horizontal position follows the
 *    finger. Dragging it far enough to the left hides it; otherwise it snaps
 *    back to the docked position.
 *  - The bar height wraps its content, up to [barHeightFraction] of the parent
 *    height. If the content exceeds that, the bar becomes scrollable.
 *  - Only the bar (and the reveal strip while hidden) consume touch input, so
 *    the rest of the parent keeps receiving interaction while the bar is shown.
 */
@Composable
fun VerticalFloatingToolbar(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    barWidth: Dp = 56.dp,
    barHeightFraction: Float = 0.7f,
    revealWidth: Dp = 24.dp,
    margin: Dp = 8.dp,
    contentSpacing: Dp = 6.dp,
    shape: Shape = RoundedCornerShape(28.dp),
    containerColor: Color = Color(0xCC666666),
    elevation: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val barWidthPx = with(density) { barWidth.toPx() }
    val marginPx = with(density) { margin.toPx() }
    val coroutineScope = rememberCoroutineScope()

    // Horizontal position when the bar is fully docked (left edge + margin) and
    // when it is fully hidden off-screen to the left.
    val dockedX = marginPx
    val hiddenX = -barWidthPx
    val triggerX = (dockedX + hiddenX) / 2f

    val currentVisible by rememberUpdatedState(visible)
    val currentOnVisibleChange by rememberUpdatedState(onVisibleChange)

    val offsetX = remember { Animatable(if (visible) dockedX else hiddenX) }

    val settleSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // Decide whether the bar should stay open or close, based on how far it was dragged.
    fun settle() {
        val shouldShow = offsetX.value > triggerX
        if (shouldShow != currentVisible) {
            currentOnVisibleChange(shouldShow)
        } else {
            coroutineScope.launch {
                offsetX.animateTo(
                    targetValue = if (shouldShow) dockedX else hiddenX,
                    animationSpec = settleSpec
                )
            }
        }
    }

    // Animate the bar in/out whenever visibility is changed from the outside
    // (e.g. tapping the background area).
    LaunchedEffect(visible) {
        offsetX.animateTo(
            targetValue = if (visible) dockedX else hiddenX,
            animationSpec = settleSpec
        )
    }

    // A custom layout is used here instead of BoxWithConstraints so that the
    // parent's max height can be read without incurring a subcomposition pass.
    // Children are aligned to the vertical center and horizontal start, and the
    // bar is capped to a fraction of the parent height (wrapping shorter content).
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            // Thin strip along the left edge used to swipe the bar open while hidden.
            if (!visible) {
                Box(
                    modifier = Modifier
                        .layoutId(RevealStripLayoutId)
                        .width(revealWidth)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { coroutineScope.launch { offsetX.stop() } },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    coroutineScope.launch {
                                        offsetX.snapTo(
                                            (offsetX.value + dragAmount).coerceIn(hiddenX, dockedX)
                                        )
                                    }
                                },
                                onDragEnd = { settle() },
                                onDragCancel = { settle() }
                            )
                        }
                )
            }

            Surface(
                modifier = Modifier
                    .layoutId(BarLayoutId)
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .width(barWidth)
                    // Swallow taps that land on the bar so they do not fall through
                    // to the background toggle.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    // Horizontal drag: the bar follows the finger and can be dragged off-screen left.
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { coroutineScope.launch { offsetX.stop() } },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    offsetX.snapTo(
                                        (offsetX.value + dragAmount).coerceIn(hiddenX, dockedX)
                                    )
                                }
                            },
                            onDragEnd = { settle() },
                            onDragCancel = { settle() }
                        )
                    },
                shape = shape,
                color = containerColor,
                shadowElevation = elevation
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(contentSpacing, Alignment.CenterVertically),
                    content = content
                )
            }
        }
    ) { measurables, constraints ->
        // Cap the bar height so it wraps a short content exactly, but scrolls
        // when the content is taller than this fraction of the parent.
        val maxBarHeight = if (constraints.hasBoundedHeight) {
            (constraints.maxHeight * barHeightFraction).roundToInt()
        } else {
            Constraints.Infinity
        }

        // The parent is measured with fillMaxSize, so its incoming constraints
        // impose minWidth = maxWidth = the parent width. Children set their own
        // width (the bar via `width(barWidth)`, the strip via `width(revealWidth)`),
        // but `Modifier.width` is incoming-enforcing, so a non-zero incoming
        // minWidth would coerce them to the full parent width. Loosen the
        // minimums before measuring the children to let them size themselves.
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val placeables = measurables.map { measurable ->
            when (measurable.layoutId) {
                // The bar may not exceed the height cap; it decides its own size
                // below that via its scrollable content.
                BarLayoutId -> measurable.measure(
                    looseConstraints.copy(maxHeight = maxBarHeight)
                )
                // The reveal strip fills the parent height.
                else -> measurable.measure(looseConstraints)
            }
        }

        val width = constraints.maxWidth
        val height = constraints.maxHeight
        layout(width, height) {
            placeables.forEach { placeable ->
                placeable.place(x = 0, y = (height - placeable.height) / 2)
            }
        }
    }
}
