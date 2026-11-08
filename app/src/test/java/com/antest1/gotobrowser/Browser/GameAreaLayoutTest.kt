package com.antest1.gotobrowser.Browser

import android.app.Application
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "mdpi")
class GameAreaLayoutTest {
    private fun layout(
        width: Int, height: Int, topDp: Int = 0, bottomDp: Int = 0,
        bias: Float = GameGeometry.BIAS_CENTER
    ): View {
        val area = GameAreaLayout(ApplicationProvider.getApplicationContext())
        val game = View(area.context)
        area.addView(game)
        area.setDividerMargin(topDp, bottomDp)
        area.verticalBias = bias
        area.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        area.layout(0, 0, width, height)
        return game
    }

    @Test
    fun noMarginFillsTheHeight() {
        val game = layout(1000, 400)
        assertEquals(667, game.width)
        assertEquals(400, game.height)
        assertEquals(0, game.top)
    }

    @Test
    fun bottomMarginShrinksTheGameKeepingItsRatio() {
        // Top split: the divider is below the window.
        val game = layout(1000, 400, bottomDp = 24)
        assertEquals(376, game.height)
        assertEquals(627, game.width)
        assertEquals(0, game.top)
        assertEquals(376, game.bottom)
    }

    @Test
    fun topMarginLeavesTheGapAboveTheGame() {
        // Bottom split: the divider is above the window.
        val game = layout(1000, 400, topDp = 24)
        assertEquals(376, game.height)
        assertEquals(627, game.width)
        assertEquals(24, game.top)
        assertEquals(400, game.bottom)
    }

    @Test
    fun marginWithinTheSlackDoesNotShrinkTheGame() {
        val game = layout(400, 1000, topDp = 24)
        assertEquals(400, game.width)
        assertEquals(240, game.height)
        // Centered in the 976px left below the margin.
        assertEquals(24 + (976 - 240) / 2, game.top)
    }

    @Test
    fun topAlignedGameStartsBelowTheMargin() {
        val game = layout(400, 1000, topDp = 24, bias = GameGeometry.BIAS_TOP)
        assertEquals(400, game.width)
        assertEquals(240, game.height)
        assertEquals(24, game.top)
    }
}
