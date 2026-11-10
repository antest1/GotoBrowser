package com.antest1.gotobrowser.ui.component

import com.antest1.gotobrowser.Browser.GameGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Where a swipe can reveal the hidden toolbar, with the navigation bar on either side. */
class RevealStripsTest {
    private val reveal = 24f

    // 2400x1080 landscape phone: the 1800x1080 game leaves 300px on each side.
    private val landscapeGame = GameGeometry.fit(2400f, 1080f)

    private fun landscape(navEdge: Float, game: GameGeometry.Rect = landscapeGame) =
        revealStrips(true, 2400f, 1080f, game, reveal, navEdge)

    @Test
    fun navigationBarOnTheOtherSideKeepsTheStripAtTheEdge() {
        // Regular landscape: the 3-button bar is on the right, nothing on the docking edge.
        val strips = landscape(navEdge = 0f)
        assertEquals(GameGeometry.Rect(0f, 0f, reveal, 1080f), strips.first)
        assertEquals(0f, strips.second.height)
        assertTrue(strips.available)
    }

    @Test
    fun navigationBarOnTheDockingEdgeMovesTheStripPastIt() {
        // Reverse landscape: the 3-button bar (126px) comes to the left, where the toolbar docks.
        val strips = landscape(navEdge = 126f)
        assertEquals(GameGeometry.Rect(126f, 0f, reveal, 1080f), strips.first)
        assertTrue(strips.available)
    }

    @Test
    fun withoutRoomPastTheNavigationBarTheStripsMoveAboveAndBelowTheGame() {
        // A game only 100px from the left edge: 126px of bar leaves no side margin.
        val game = GameGeometry.Rect(100f, 90f, 1500f, 900f)
        val strips = landscape(navEdge = 126f, game = game)
        assertEquals(GameGeometry.Rect(126f, 0f, reveal, 90f), strips.first)
        assertEquals(GameGeometry.Rect(126f, 990f, reveal, 90f), strips.second)
        assertTrue(strips.available)
    }

    @Test
    fun noRoomAnywhereLeavesTheBackButtonToShowTheToolbar() {
        val game = GameGeometry.Rect(100f, 0f, 1800f, 1080f)
        assertFalse(landscape(navEdge = 126f, game = game).available)
    }

    @Test
    fun portraitStripsStartAboveTheNavigationBar() {
        // 1080x2400 portrait: the 1080x648 game sits in the middle, the bar is 126px at the bottom.
        val game = GameGeometry.fit(1080f, 2400f)
        val strips = revealStrips(false, 1080f, 2400f, game, reveal, 126f)
        assertEquals(GameGeometry.Rect(0f, 2400f - reveal - 126f, 1080f, reveal), strips.first)
        assertEquals(0f, strips.second.width)
        assertTrue(strips.available)
    }

    @Test
    fun portraitToolbarSitsAboveTheNavigationBar() {
        // 126px 3-button bar at the bottom, no cutout there.
        assertEquals(126f, toolbarDockInset(false, 0f, 0f, 126f))
        // Multi-window: the bottom inset already holds the visible bar, so it is not added twice.
        assertEquals(126f, toolbarDockInset(false, 0f, 126f, 126f))
    }

    @Test
    fun reverseLandscapeToolbarSitsPastTheNavigationBar() {
        // 3-button bar on the left (126px), wider than an 80px cutout there.
        assertEquals(126f, toolbarDockInset(true, 80f, 0f, 126f))
    }

    @Test
    fun regularLandscapeToolbarOnlyAvoidsTheCutout() {
        // The navigation bar is on the right: nothing on the docking edge.
        assertEquals(80f, toolbarDockInset(true, 80f, 0f, 0f))
    }
}
