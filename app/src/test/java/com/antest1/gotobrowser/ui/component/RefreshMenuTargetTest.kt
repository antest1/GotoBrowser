package com.antest1.gotobrowser.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

/** The refresh FAB's long-press menu, opening up (default) or down (FAB above the toolbar). */
class RefreshMenuTargetTest {
    private val fab = 56f
    private val step = 64f

    private fun up(y: Float) = refreshMenuTarget(y, fab, step, expandDown = false)
    private fun down(y: Float) = refreshMenuTarget(y, fab, step, expandDown = true)

    @Test
    fun theFabItselfPicksItsOwnOption() {
        assertEquals(RefreshMenuTarget.TAP, up(fab / 2))
        assertEquals(RefreshMenuTarget.TAP, down(fab / 2))
    }

    @Test
    fun theOtherOptionIsAboveWhenOpeningUp() {
        assertEquals(RefreshMenuTarget.OTHER, up(-step + fab / 2))
        assertEquals(RefreshMenuTarget.TAP, up(step + fab / 2 - 30f))
    }

    @Test
    fun theOtherOptionIsBelowWhenOpeningDown() {
        assertEquals(RefreshMenuTarget.OTHER, down(step + fab / 2))
        assertEquals(RefreshMenuTarget.TAP, down(-step + fab / 2 + 30f))
    }

    @Test
    fun farAwayPicksNothing() {
        assertEquals(null, up(-3 * step))
        assertEquals(null, up(fab + step))
        assertEquals(null, down(fab + 3 * step))
        assertEquals(null, down(-step))
    }

    @Test
    fun bothDirectionsAreMirrorImages() {
        var y = -3 * step
        while (y <= fab + 3 * step) {
            assertEquals("y=$y", up(y), down(fab - y))
            y += 1f
        }
    }
}
