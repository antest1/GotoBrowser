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
    fun noRoomAnywherePutsTheStripOverTheGame() {
        // 1340x800 tablet: the 1333x800 game leaves about 3px on each side.
        val strips = revealStrips(true, 1340f, 800f, GameGeometry.fit(1340f, 800f), reveal, 0f)
        assertFalse(strips.available)
        assertEquals(GameGeometry.Rect(0f, 0f, reveal, 800f), strips.overGame)
        assertEquals(0f, strips.first.height)
        assertEquals(0f, strips.second.height)
    }

    @Test
    fun marginsShorterThanTheStripPutItOverTheGameToo() {
        // 1280x800 tablet: the 1280x768 game leaves 16px above and below, less than the strip.
        val strips = revealStrips(true, 1280f, 800f, GameGeometry.fit(1280f, 800f), reveal, 0f)
        assertFalse(strips.available)
        assertEquals(GameGeometry.Rect(0f, 0f, reveal, 800f), strips.overGame)
        assertEquals(0f, strips.first.height)
        assertEquals(0f, strips.second.height)
    }

    @Test
    fun noStripOverTheGameWithTheNavigationBarOnThatEdge() {
        // Reverse landscape with the 3-button bar on the left: the strip would start 126px
        // in, inside the game. Back is left to show the toolbar.
        val game = GameGeometry.Rect(100f, 0f, 1800f, 1080f)
        val strips = landscape(navEdge = 126f, game = game)
        assertFalse(strips.available)
        assertEquals(0f, strips.overGame.width)
        assertEquals(0f, strips.overGame.height)
    }

    @Test
    fun marginsTooShortKeepTheirStripsWithTheNavigationBarOnThatEdge() {
        // 12px above and below the game: less than the strip, but still what there is to swipe from.
        val game = GameGeometry.Rect(100f, 12f, 1760f, 1056f)
        val strips = landscape(navEdge = 126f, game = game)
        assertFalse(strips.available)
        assertEquals(GameGeometry.Rect(126f, 0f, reveal, 12f), strips.first)
        assertEquals(GameGeometry.Rect(126f, 1068f, reveal, 12f), strips.second)
        assertEquals(0f, strips.overGame.width)
    }

    @Test
    fun roomInAMarginLeavesNoStripOverTheGame() {
        assertEquals(0f, landscape(navEdge = 0f).overGame.width)
        val game = GameGeometry.Rect(100f, 90f, 1500f, 900f)
        assertEquals(0f, landscape(navEdge = 126f, game = game).overGame.width)
        assertEquals(0f, revealStrips(false, 1080f, 2400f, GameGeometry.fit(1080f, 2400f), reveal, 126f).overGame.width)
    }

    @Test
    fun portraitWithoutRoomPutsTheStripOverTheGameAtTheBottom() {
        // A small window the game fills almost to the bottom, with no navigation bar there.
        val game = GameGeometry.Rect(0f, 100f, 500f, 300f)
        val strips = revealStrips(false, 500f, 420f, game, reveal, 0f)
        assertFalse(strips.available)
        assertEquals(GameGeometry.Rect(0f, 420f - reveal, 500f, reveal), strips.overGame)
        assertEquals(0f, strips.first.width)
        assertEquals(0f, strips.second.width)
        // With the navigation bar at the bottom there is none.
        assertEquals(0f, revealStrips(false, 500f, 420f, game, reveal, 40f).overGame.width)
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
    fun hiddenToolbarGoesPastTheWiderOfTheBarAndTheRefreshButton() {
        // Tablet tier at 200dpi: 64dp bar, 68dp button.
        assertEquals(-85f, toolbarHiddenOffset(80f, 85f))
        // Phone tier: both 56dp.
        assertEquals(-56f, toolbarHiddenOffset(56f, 56f))
    }

    @Test
    fun hiddenToolbarOffsetCoversAHalfPixelSizeAsLaidOut() {
        // The same tier at 300dpi: the 127.5px button is laid out 128px wide.
        assertEquals(-128f, toolbarHiddenOffset(120f, 127.5f))
    }

    @Test
    fun hiddenToolbarAlsoTakesWhatTheButtonDrawsAroundItselfOffScreen() {
        // A 12dp ring around the button: 15px at 200dpi, 22.5px at 300dpi (a started pixel counts).
        assertEquals(-100f, toolbarHiddenOffset(80f, 85f, 15f))
        assertEquals(-151f, toolbarHiddenOffset(120f, 127.5f, 22.5f))
    }

    @Test
    fun regularLandscapeToolbarOnlyAvoidsTheCutout() {
        // The navigation bar is on the right: nothing on the docking edge.
        assertEquals(80f, toolbarDockInset(true, 80f, 0f, 0f))
    }
}
