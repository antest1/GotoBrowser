package com.antest1.gotobrowser.Browser

import android.app.Application
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The edge swipe over the game: only a drag in from the strip is taken away from the game. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "mdpi")
class ToolbarEdgeSwipeLayoutTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    // 1340x800 landscape tablet: the game fills the screen, the strip is its left 24px.
    private val edgeSwipe = ToolbarEdgeSwipe().apply { strip = GameGeometry.Rect(0f, 0f, 24f, 800f) }
    private val drags = mutableListOf<Float>()
    private val releases = mutableListOf<Float>()

    /** What the view under the finger (the game) received. */
    private val gameEvents = mutableListOf<Int>()

    private val root = ToolbarEdgeSwipeLayout(context, edgeSwipe).apply {
        addView(View(context).apply {
            setOnTouchListener { _, event -> gameEvents.add(event.actionMasked); true }
        })
        measure(
            View.MeasureSpec.makeMeasureSpec(1340, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, 1340, 800)
    }

    init {
        edgeSwipe.onDrag = { drags.add(it) }
        edgeSwipe.onDragEnd = { releases.add(it) }
    }

    private val downTime = SystemClock.uptimeMillis()
    private var eventTime = downTime

    private fun touch(action: Int, x: Float, y: Float) {
        eventTime += 10
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        root.dispatchTouchEvent(event)
        event.recycle()
    }

    @Test
    fun aSwipeInFromTheStripMovesTheBarAndIsTakenFromTheGame() {
        touch(MotionEvent.ACTION_DOWN, 10f, 400f)
        touch(MotionEvent.ACTION_MOVE, 10f + slop + 1f, 402f)
        touch(MotionEvent.ACTION_MOVE, 60f, 404f)
        touch(MotionEvent.ACTION_MOVE, 100f, 404f)
        touch(MotionEvent.ACTION_UP, 100f, 404f)

        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_CANCEL), gameEvents)
        // The bar follows the finger from where the swipe was recognised.
        assertEquals(100f - (10f + slop + 1f), drags.sum(), 0.01f)
        assertEquals(1, releases.size)
        assertTrue(releases[0] > 0f)
    }

    @Test
    fun aTapOnTheStripGoesToTheGame() {
        touch(MotionEvent.ACTION_DOWN, 10f, 400f)
        touch(MotionEvent.ACTION_MOVE, 12f, 401f)
        touch(MotionEvent.ACTION_UP, 12f, 401f)

        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP), gameEvents)
        assertTrue(drags.isEmpty())
        assertTrue(releases.isEmpty())
    }

    @Test
    fun aDragAlongTheStripStaysWithTheGameEvenIfItTurnsInwardLater() {
        touch(MotionEvent.ACTION_DOWN, 10f, 400f)
        touch(MotionEvent.ACTION_MOVE, 12f, 400f + slop + 1f)
        touch(MotionEvent.ACTION_MOVE, 200f, 400f + slop + 1f)
        touch(MotionEvent.ACTION_UP, 200f, 400f + slop + 1f)

        assertEquals(
            listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP),
            gameEvents
        )
        assertTrue(drags.isEmpty())
        assertTrue(releases.isEmpty())
    }

    @Test
    fun aSwipeStartingPastTheStripStaysWithTheGame() {
        touch(MotionEvent.ACTION_DOWN, 30f, 400f)
        touch(MotionEvent.ACTION_MOVE, 120f, 400f)
        touch(MotionEvent.ACTION_UP, 120f, 400f)

        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP), gameEvents)
        assertTrue(drags.isEmpty())
    }

    @Test
    fun withoutAStripNothingIsTaken() {
        edgeSwipe.strip = null
        touch(MotionEvent.ACTION_DOWN, 10f, 400f)
        touch(MotionEvent.ACTION_MOVE, 120f, 400f)
        touch(MotionEvent.ACTION_UP, 120f, 400f)

        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP), gameEvents)
        assertTrue(drags.isEmpty())
    }

    @Test
    fun theGameCannotKeepASwipeThatStartedOnTheStrip() {
        touch(MotionEvent.ACTION_DOWN, 10f, 400f)
        root.getChildAt(0).parent.requestDisallowInterceptTouchEvent(true)
        touch(MotionEvent.ACTION_MOVE, 80f, 400f)
        touch(MotionEvent.ACTION_MOVE, 120f, 400f)
        touch(MotionEvent.ACTION_UP, 120f, 400f)

        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_CANCEL), gameEvents)
        assertEquals(40f, drags.sum(), 0.01f)
    }

    @Test
    fun aCancelledSwipeSettlesTheBarWithoutSpeed() {
        touch(MotionEvent.ACTION_DOWN, 10f, 400f)
        touch(MotionEvent.ACTION_MOVE, 80f, 400f)
        touch(MotionEvent.ACTION_MOVE, 120f, 400f)
        touch(MotionEvent.ACTION_CANCEL, 120f, 400f)

        assertEquals(listOf(0f), releases)
    }

    @Test
    fun inPortraitTheSwipeGoesUpFromTheBottomStrip() {
        edgeSwipe.strip = GameGeometry.Rect(0f, 776f, 1340f, 24f)
        edgeSwipe.fromLeft = false
        touch(MotionEvent.ACTION_DOWN, 600f, 790f)
        touch(MotionEvent.ACTION_MOVE, 600f, 740f)
        touch(MotionEvent.ACTION_MOVE, 600f, 700f)
        touch(MotionEvent.ACTION_UP, 600f, 700f)

        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_CANCEL), gameEvents)
        assertEquals(40f, drags.sum(), 0.01f)
        assertTrue(releases.single() > 0f)
    }

    @Test
    fun onlyAMoveMostlyInwardPastTheTouchSlopStartsTheSwipe() {
        assertNull(revealSwipeStarts(inward = slop, across = 0f, slop = slop))
        assertEquals(true, revealSwipeStarts(inward = slop + 1f, across = slop, slop = slop))
        // Mostly along the edge, or back toward it.
        assertEquals(false, revealSwipeStarts(inward = slop, across = slop + 1f, slop = slop))
        assertEquals(false, revealSwipeStarts(inward = -slop - 1f, across = 0f, slop = slop))
    }
}
