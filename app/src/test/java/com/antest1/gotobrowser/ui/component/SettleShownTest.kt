package com.antest1.gotobrowser.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When a swipe brings the toolbar out or puts it away (landscape numbers, px = dp here). */
class SettleShownTest {
    // Hidden 56 off-screen, docked 24 in: 80 to travel, so a quarter is 20.
    private val hidden = -56f
    private val docked = 24f
    private val fling = 400f

    private fun reveal(moved: Float, velocity: Float = 0f) =
        settleShown(hidden + moved, hidden, docked, wasVisible = false, velocity = velocity, flingVelocity = fling)

    private fun hide(moved: Float, velocity: Float = 0f) =
        settleShown(docked - moved, hidden, docked, wasVisible = true, velocity = velocity, flingVelocity = fling)

    @Test
    fun aQuarterOfTheWayIsEnoughToBringItOut() {
        assertFalse(reveal(19f))
        assertTrue(reveal(20f))
    }

    @Test
    fun aQuarterOfTheWayIsEnoughToPutItAway() {
        // hide() returns whether the bar stays shown.
        assertTrue(hide(19f))
        assertFalse(hide(21f))
    }

    @Test
    fun aFlingDecidesOnItsOwn() {
        assertTrue(reveal(6f, velocity = 500f))
        assertFalse(hide(4f, velocity = -500f))
    }

    @Test
    fun aFlingTheOtherWayUndoesTheDrag() {
        assertFalse(reveal(60f, velocity = -500f))
        assertTrue(hide(60f, velocity = 500f))
    }

    @Test
    fun aSlowReleaseIsJudgedByDistanceOnly() {
        assertTrue(reveal(30f, velocity = -100f))
        assertFalse(reveal(10f, velocity = 300f))
    }

    @Test
    fun theSettleAnimationKeepsTheReleaseSpeedTowardItsTarget() {
        assertEquals(800f, settleVelocity(800f, shown = true, max = 1500f))
        assertEquals(-800f, settleVelocity(-800f, shown = false, max = 1500f))
    }

    @Test
    fun aFastReleaseIsCappedAndAReleaseAwayStartsAtRest() {
        assertEquals(1500f, settleVelocity(5000f, shown = true, max = 1500f))
        assertEquals(0f, settleVelocity(-300f, shown = true, max = 1500f))
        assertEquals(0f, settleVelocity(300f, shown = false, max = 1500f))
    }
}
