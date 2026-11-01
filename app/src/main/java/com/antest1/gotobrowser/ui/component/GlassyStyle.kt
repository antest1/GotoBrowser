package com.antest1.gotobrowser.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.glassyStyle(shape: Shape): Modifier {
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.5f),
            Color.Gray.copy(alpha = 0.25f),
            Color.LightGray.copy(alpha = 0.5f)
        )
    )
    return this
        .background(Color.DarkGray.copy(alpha = 0.5f), shape)
        .border(BorderStroke(1.dp, borderBrush), shape)
}
