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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val RevealStrip1LayoutId = "reveal_strip_1"
private const val RevealStrip2LayoutId = "reveal_strip_2"
private const val BarLayoutId = "bar"
private const val FabLayoutId = "fab"

@Composable
fun AdaptiveFloatingToolbar(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    barSize: Dp = 56.dp,
    barLengthFraction: Float = 0.7f,
    revealSize: Dp = 24.dp,
    contentSpacing: Dp = 8.dp,
    elevation: Dp = 0.dp,
    fab: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Margins as requested: 16dp for horizontal (Portrait), 24dp for vertical (Landscape)
    val margin = if (isLandscape) 24.dp else 16.dp
    val marginPx = with(density) { margin.toPx() }
    val barSizePx = with(density) { barSize.toPx() }

    // Offset logic
    // Vertical: slides horizontally from left (-barSize to margin)
    // Horizontal: slides vertically from bottom (containerHeight to containerHeight - margin - barSize)
    val dockedOffset = marginPx
    val hiddenOffset = -barSizePx
    val triggerOffset = (dockedOffset + hiddenOffset) / 2f

    val currentVisible by rememberUpdatedState(visible)
    val currentOnVisibleChange by rememberUpdatedState(onVisibleChange)

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

    LaunchedEffect(visible) {
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
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(contentSpacing, Alignment.CenterVertically)
                    ) { content() }
                } else {
                    Row(
                        modifier = Modifier.fillMaxHeight().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(contentSpacing, Alignment.CenterHorizontally)
                    ) { content() }
                }
            }
        }
    ) { measurables, constraints ->
        val maxBarLength = ((if (isLandscape) constraints.maxHeight else constraints.maxWidth) * barLengthFraction).roundToInt()
        
        // WebView geometry (simplified dodge logic for now, similar to original)
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val ratio = 1200f / 720f
        val wvw: Float
        val wvh: Float
        if (containerWidth / containerHeight > ratio) {
            wvh = containerHeight
            wvw = containerHeight * ratio
        } else {
            wvw = containerWidth
            wvh = containerWidth / ratio
        }
        val wvl = (containerWidth - wvw) / 2f
        val wvt = (containerHeight - wvh) / 2f
        val wvr = wvl + wvw
        val wvb = wvt + wvh

        val revealSizePx = with(density) { revealSize.toPx() }
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
                    measurable.measure(looseConstraints.copy(minWidth = barSizePx.roundToInt(), minHeight = barSizePx.roundToInt()))
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
