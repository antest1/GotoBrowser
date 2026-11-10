package com.antest1.gotobrowser.ui.component

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.antest1.gotobrowser.Activity.safeOverlayInsets
import com.antest1.gotobrowser.Browser.GameGeometry
import com.antest1.gotobrowser.R

enum class RefreshOption { QUICK, FULL }

/** What a finger on the refresh FAB's long-press menu points at: the FAB's own option or the other one. */
enum class RefreshMenuTarget { TAP, OTHER }

/**
 * The menu item under a finger at [localY] (relative to the FAB's top), or null between or
 * beyond them. The other option sits one [itemStepPx] above the FAB, or below it when
 * [expandDown]: the two layouts are mirror images, so they feel the same.
 */
fun refreshMenuTarget(localY: Float, fabSizePx: Float, itemStepPx: Float, expandDown: Boolean): RefreshMenuTarget? {
    val y = if (expandDown) fabSizePx - localY else localY
    val slop = itemStepPx * 0.4f
    return when {
        y >= -itemStepPx - slop && y < -itemStepPx / 2f -> RefreshMenuTarget.OTHER
        y >= -itemStepPx / 2f && y <= fabSizePx + slop -> RefreshMenuTarget.TAP
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlLayout(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    /** Whether the toolbar can be revealed by a margin tap or an edge swipe. */
    onRevealAvailableChange: (Boolean) -> Unit = {},
    onRefreshClick: () -> Unit,
    onQuickRefreshClick: () -> Unit = onRefreshClick,
    onFullRefreshClick: () -> Unit = onRefreshClick,
    selectedRefreshOption: RefreshOption = RefreshOption.QUICK,
    onRefreshOptionChange: (RefreshOption) -> Unit = {},
    onSettingsClick: () -> Unit,
    /** Ring drawn around the refresh FAB to point the user at it (warning / error), or null. */
    refreshAttention: Color? = null,
    /** Tapping the FAB runs a full refresh, without changing the saved option. */
    forceFullRefresh: Boolean = false,
    /** Matches GameAreaLayout.verticalBias; see GameGeometry.fit. */
    gameVerticalBias: Float = GameGeometry.BIAS_CENTER,
    /** Toolbar docked at the top with the refresh FAB first; landscape only (PREF_TOOLBAR_POSITION). */
    toolbarAtTop: Boolean = false,
    /** Edge swipe reveals the hidden toolbar (PREF_TOOLBAR_SWIPE); a margin tap always does. */
    swipeToReveal: Boolean = true,
    toolbarContent: @Composable () -> Unit
) {
    // In portrait the bar lies along the bottom and the FAB already sits at its start.
    val refreshAboveBar = toolbarAtTop &&
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Box(modifier = Modifier.fillMaxSize()) {
        // Main Toolbar (Adaptive)
        // End inset equals the side inset, so the pill has uniform breathing room around the buttons.
        val panelBarSize = dimensionResource(R.dimen.browser_panel_bar_size)
        val panelButtonSize = dimensionResource(R.dimen.browser_panel_button_size)
        AdaptiveFloatingToolbar(
            visible = visible,
            onVisibleChange = onVisibleChange,
            onRevealAvailableChange = onRevealAvailableChange,
            gameVerticalBias = gameVerticalBias,
            barSize = panelBarSize,
            contentInset = (panelBarSize - panelButtonSize) / 2,
            alignTop = refreshAboveBar,
            swipeToReveal = swipeToReveal,
            // The refresh FAB then lines up with the settings button in the opposite corner.
            topInset = dimensionResource(R.dimen.top_menu_padding),
            fab = {
                FullRefreshHintTooltip(visible = forceFullRefresh && visible) {
                    OneTouchRefreshFab(
                        selectedOption = selectedRefreshOption,
                        onSelectedOptionChange = onRefreshOptionChange,
                        onQuickRefreshClick = onQuickRefreshClick,
                        onFullRefreshClick = onFullRefreshClick,
                        attention = refreshAttention,
                        forceFullRefresh = forceFullRefresh,
                        // Above the bar there is little room overhead, so the menu opens over the bar.
                        expandDown = refreshAboveBar
                    )
                }
            },
            content = toolbarContent
        )

        // Top Right Button (Settings) - a single glassy circle, the same size as the refresh FAB,
        // so the two corner buttons mirror each other. Help lives inside the settings sheet.
        val topMenuButtonSize = dimensionResource(R.dimen.top_menu_button_size)
        val topMenuIconSize = dimensionResource(R.dimen.top_menu_icon_size)
        val topMenuPadding = dimensionResource(R.dimen.top_menu_padding)
        val edgeMargin = floatingToolbarEdgeMargin()
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(safeOverlayInsets())
                .padding(top = topMenuPadding, end = edgeMargin)
        ) {
            TooltipBox(
                // Button sits in the top-end corner: no room above or to the end, so open toward start.
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Start,
                    spacingBetweenTooltipAndAnchor = OverlayTooltipSpacing
                ),
                tooltip = { PlainTooltip { Text(stringResource(R.string.settings_menu_tooltip)) } },
                state = rememberTooltipState()
            ) {
                GlassyCircleButton(
                    iconRes = R.drawable.settings,
                    contentDescription = stringResource(R.string.settings_menu_tooltip),
                    size = topMenuButtonSize,
                    iconSize = topMenuIconSize,
                    onClick = onSettingsClick
                )
            }
        }
    }
}

/**
 * A small round glassy button. Deliberately NOT a Material3 IconButton: it enforces a
 * 48dp minimum interactive size and draws its background/ripple at that size even when a smaller
 * Modifier.size is given, which made 32dp buttons overflow their layout slot and overlap neighbours.
 */
@Composable
private fun GlassyCircleButton(
    iconRes: Int,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .glassyStyle(CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Persistent "Full Refresh" tooltip next to the FAB while only a full refresh can recover.
 * User input is off so the tooltip's own long-press never competes with the FAB's gesture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullRefreshHintTooltip(visible: Boolean, content: @Composable () -> Unit) {
    val state = rememberTooltipState(isPersistent = true)
    LaunchedEffect(visible) {
        if (visible) state.show() else state.dismiss()
    }
    TooltipBox(
        positionProvider = rememberToolbarTooltipPositionProvider(TooltipAnchorPosition.End),
        tooltip = { PlainTooltip { Text(stringResource(R.string.menu_full_refresh)) } },
        state = state,
        enableUserInput = false,
        content = content
    )
}

@Composable
private fun OneTouchRefreshFab(
    selectedOption: RefreshOption,
    onSelectedOptionChange: (RefreshOption) -> Unit,
    onQuickRefreshClick: () -> Unit,
    onFullRefreshClick: () -> Unit,
    attention: Color?,
    forceFullRefresh: Boolean,
    /** Open the menu below the FAB instead of above it. */
    expandDown: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isTouching by remember { mutableStateOf(false) }
    var hoveredOption by remember { mutableStateOf<RefreshOption?>(null) }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val fabSizeDp = dimensionResource(R.dimen.refresh_fab_size)
    val fabIconSizeDp = dimensionResource(R.dimen.refresh_fab_icon_size)
    val itemStepDp = fabSizeDp + 8.dp
    val itemStepPx = with(density) { itemStepDp.toPx() }
    val fabSizePx = with(density) { fabSizeDp.toPx() }

    // While forced, FULL takes the tap position but the saved choice stays as it is.
    val tapOption = if (forceFullRefresh) RefreshOption.FULL else selectedOption
    val otherOption = if (tapOption == RefreshOption.QUICK) RefreshOption.FULL else RefreshOption.QUICK

    fun calculateHoveredOption(localY: Float): RefreshOption? =
        when (refreshMenuTarget(localY, fabSizePx, itemStepPx, expandDown)) {
            RefreshMenuTarget.OTHER -> otherOption
            RefreshMenuTarget.TAP -> tapOption
            null -> null
        }

    val expandAnim by animateFloatAsState(
        targetValue = if (isTouching) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "fab_expand"
    )

    Box(
        modifier = modifier
            .wrapContentSize(align = Alignment.TopStart, unbounded = true)
            .pointerInput(tapOption, forceFullRefresh, expandDown) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        isTouching = true
                        hoveredOption = tapOption
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        var currentHover: RefreshOption? = tapOption

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                val localY = change.position.y
                                val newHover = calculateHoveredOption(localY)
                                if (newHover != currentHover) {
                                    currentHover = newHover
                                    hoveredOption = newHover
                                    if (newHover != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        isTouching = false
                        val finalOption = currentHover
                        hoveredOption = null

                        if (finalOption != null) {
                            if (!(forceFullRefresh && finalOption == RefreshOption.FULL)) {
                                onSelectedOptionChange(finalOption)
                            }
                            when (finalOption) {
                                RefreshOption.QUICK -> onQuickRefreshClick()
                                RefreshOption.FULL -> onFullRefreshClick()
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.TopStart
    ) {
        // Standalone Default FAB (shown when not touching)
        Box(
            modifier = Modifier
                .size(fabSizeDp)
                .attentionRing(attention)
                .glassyStyle(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (tapOption == RefreshOption.QUICK) R.drawable.quick_refresh_icon else R.drawable.refresh_icon
                ),
                contentDescription = stringResource(R.string.menu_tooltip_refresh),
                tint = Color.White,
                modifier = Modifier.size(fabIconSizeDp)
            )
        }

        // M3 Extended FAB Speed Dial Overlay
        if (isTouching || expandAnim > 0f) {
            Box(
                modifier = Modifier
                    .zIndex(10f)
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                    .graphicsLayer {
                        alpha = expandAnim
                        scaleX = 0.7f + 0.3f * expandAnim
                        scaleY = 0.7f + 0.3f * expandAnim
                    },
                contentAlignment = Alignment.TopStart
            ) {
                listOf(
                    otherOption to itemStepDp * expandAnim * (if (expandDown) 1f else -1f),
                    tapOption to 0.dp
                ).forEach { (option, yOffset) ->
                    ExtendedFabMenuItem(
                        label = stringResource(if (option == RefreshOption.QUICK) R.string.menu_quick_refresh else R.string.menu_full_refresh),
                        iconRes = if (option == RefreshOption.QUICK) R.drawable.quick_refresh_icon else R.drawable.refresh_icon,
                        iconTint = Color.White,
                        isHovered = hoveredOption == option,
                        itemHeight = fabSizeDp,
                        iconSize = fabIconSizeDp,
                        modifier = Modifier.offset(y = yOffset)
                    )
                }
            }
        }
    }
}

/**
 * Ring outside the FAB that pulses three times and then stays, so guidance text can point
 * at the existing button instead of adding a new one. Drawn outside the bounds, unclipped.
 */
@Composable
private fun Modifier.attentionRing(color: Color?): Modifier {
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(color) {
        pulse.snapTo(0f)
        if (color == null) return@LaunchedEffect
        repeat(3) {
            pulse.animateTo(1f, tween(350))
            pulse.animateTo(0.35f, tween(450))
        }
        pulse.animateTo(1f, tween(350))
    }
    if (color == null) return this
    return drawBehind {
        val alpha = pulse.value
        val ringWidth = 3.dp.toPx()
        val ringRadius = size.minDimension / 2f + 1.dp.toPx() + ringWidth / 2f
        val haloWidth = 8.dp.toPx()
        drawCircle(color.copy(alpha = 0.22f * alpha), radius = ringRadius + ringWidth / 2f + haloWidth / 2f, style = Stroke(haloWidth))
        drawCircle(color.copy(alpha = alpha), radius = ringRadius, style = Stroke(ringWidth))
    }
}

@Composable
private fun ExtendedFabMenuItem(
    label: String,
    iconRes: Int,
    iconTint: Color,
    isHovered: Boolean,
    itemHeight: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.05f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "item_scale"
    )

    Surface(
        modifier = modifier
            .wrapContentSize(align = Alignment.CenterStart, unbounded = true)
            .requiredHeight(itemHeight),
        shape = CircleShape,
        color = if (isHovered) Color(0xFF3F4246) else Color(0xFF252729),
        border = BorderStroke(
            width = if (isHovered) 1.5.dp else 1.dp,
            color = if (isHovered) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.25f)
        ),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .height(itemHeight)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(itemHeight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = if (isHovered) iconTint else iconTint.copy(alpha = 0.85f),
                    modifier = Modifier.size(iconSize)
                )
            }

            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}