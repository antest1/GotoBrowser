package com.antest1.gotobrowser.Browser

/**
 * Where the 1200:720 game sits in its container. Shared by GameAreaLayout (which draws the
 * WebView) and the Compose overlays that must line up with it (loading status, subtitle,
 * toolbar reveal strips), so they never disagree about the game's position.
 */
object GameGeometry {
    const val RATIO = 1200f / 720f

    /** Vertical bias, as in Compose's BiasAlignment: -1 = top, 0 = center. */
    const val BIAS_TOP = -1f
    const val BIAS_CENTER = 0f

    data class Rect(val left: Float, val top: Float, val width: Float, val height: Float) {
        val right get() = left + width
        val bottom get() = top + height
    }

    /**
     * Fits the game into the area left after the insets. The game is always centered
     * horizontally; [verticalBias] only matters when the area is narrower than 1200:720,
     * i.e. when there is a margin above and below the game.
     */
    fun fit(
        containerWidth: Float,
        containerHeight: Float,
        verticalBias: Float = BIAS_CENTER,
        insetLeft: Float = 0f,
        insetTop: Float = 0f,
        insetRight: Float = 0f,
        insetBottom: Float = 0f
    ): Rect {
        val availableWidth = (containerWidth - insetLeft - insetRight).coerceAtLeast(1f)
        val availableHeight = (containerHeight - insetTop - insetBottom).coerceAtLeast(1f)
        val width: Float
        val height: Float
        if (availableWidth / availableHeight > RATIO) {
            height = availableHeight
            width = availableHeight * RATIO
        } else {
            width = availableWidth
            height = availableWidth / RATIO
        }
        val left = insetLeft + (availableWidth - width) / 2f
        val top = insetTop + (availableHeight - height) * (1f + verticalBias) / 2f
        return Rect(left, top, width, height)
    }
}
