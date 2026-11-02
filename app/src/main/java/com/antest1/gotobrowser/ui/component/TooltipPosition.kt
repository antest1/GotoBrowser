package com.antest1.gotobrowser.ui.component

import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Gap between a tooltip and the button it describes. The Material default (4dp) sits under
 * the thumb that is long-pressing the button; 16dp clears the fingertip while keeping the
 * tooltip visibly attached to its button.
 */
val OverlayTooltipSpacing: Dp = 16.dp

/**
 * Tooltip placement for buttons in the adaptive floating toolbar.
 *
 * In landscape the bar is vertical, so an above/below tooltip would cover the pressed icon
 * and its neighbour; place it beside the bar instead, on [landscapeSide] (the side facing
 * away from the screen edge the bar hugs). In portrait the bar is horizontal and the
 * Material default (above) is fine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberToolbarTooltipPositionProvider(
    landscapeSide: TooltipAnchorPosition
): PopupPositionProvider {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return TooltipDefaults.rememberTooltipPositionProvider(
        positioning = if (isLandscape) landscapeSide else TooltipAnchorPosition.Above,
        spacingBetweenTooltipAndAnchor = OverlayTooltipSpacing
    )
}
