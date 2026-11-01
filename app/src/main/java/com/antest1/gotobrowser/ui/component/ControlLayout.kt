package com.antest1.gotobrowser.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.antest1.gotobrowser.R

enum class RefreshOption { QUICK, FULL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlLayout(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    onRefreshClick: () -> Unit,
    onQuickRefreshClick: () -> Unit = onRefreshClick,
    onFullRefreshClick: () -> Unit = onRefreshClick,
    selectedRefreshOption: RefreshOption = RefreshOption.QUICK,
    onRefreshOptionChange: (RefreshOption) -> Unit = {},
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    toolbarContent: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main Toolbar (Adaptive)
        AdaptiveFloatingToolbar(
            visible = visible,
            onVisibleChange = onVisibleChange,
            fab = {
                OneTouchRefreshFab(
                    selectedOption = selectedRefreshOption,
                    onSelectedOptionChange = onRefreshOptionChange,
                    onQuickRefreshClick = onQuickRefreshClick,
                    onFullRefreshClick = onFullRefreshClick
                )
            },
            content = toolbarContent
        )

        // Top Right Buttons (Settings, Help) - Smaller & Subtle
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onHelpClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painterResource(id = R.drawable.help_icon),
                        contentDescription = "Help",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(stringResource(R.string.settings_menu_tooltip)) } },
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painterResource(id = R.drawable.settings),
                            contentDescription = stringResource(R.string.settings_menu_tooltip),
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OneTouchRefreshFab(
    selectedOption: RefreshOption,
    onSelectedOptionChange: (RefreshOption) -> Unit,
    onQuickRefreshClick: () -> Unit,
    onFullRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isTouching by remember { mutableStateOf(false) }
    var hoveredOption by remember { mutableStateOf<RefreshOption?>(null) }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val itemStepDp = 64.dp
    val itemStepPx = with(density) { itemStepDp.toPx() }
    val fabSizeDp = 56.dp
    val fabSizePx = with(density) { fabSizeDp.toPx() }

    val bottomOption = selectedOption
    val topOption = if (selectedOption == RefreshOption.QUICK) RefreshOption.FULL else RefreshOption.QUICK

    fun calculateHoveredOption(localY: Float): RefreshOption? {
        val topYStart = -itemStepPx
        val slop = itemStepPx * 0.4f

        return when {
            localY >= (topYStart - slop) && localY < -itemStepPx / 2f -> topOption
            localY >= -itemStepPx / 2f && localY <= (fabSizePx + slop) -> bottomOption
            else -> null
        }
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
            .pointerInput(selectedOption) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        isTouching = true
                        hoveredOption = selectedOption
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        var currentHover: RefreshOption? = selectedOption

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
                            onSelectedOptionChange(finalOption)
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
                .glassyStyle(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (selectedOption == RefreshOption.QUICK) R.drawable.quick_refresh_icon else R.drawable.refresh_icon
                ),
                contentDescription = stringResource(R.string.menu_tooltip_refresh),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
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
                    topOption to -itemStepDp * expandAnim,
                    bottomOption to 0.dp
                ).forEach { (option, yOffset) ->
                    ExtendedFabMenuItem(
                        label = stringResource(if (option == RefreshOption.QUICK) R.string.menu_quick_refresh else R.string.menu_full_refresh),
                        iconRes = if (option == RefreshOption.QUICK) R.drawable.quick_refresh_icon else R.drawable.refresh_icon,
                        iconTint = Color.White,
                        isHovered = hoveredOption == option,
                        modifier = Modifier.offset(y = yOffset)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtendedFabMenuItem(
    label: String,
    iconRes: Int,
    iconTint: Color,
    isHovered: Boolean,
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
            .requiredHeight(56.dp),
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
                .height(56.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = if (isHovered) iconTint else iconTint.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
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
