package com.antest1.gotobrowser.ui.component

import android.app.Application
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.antest1.gotobrowser.Activity.PanelButton
import com.antest1.gotobrowser.Browser.GameGeometry
import com.antest1.gotobrowser.Browser.ToolbarEdgeSwipe
import com.antest1.gotobrowser.Browser.ToolbarEdgeSwipeLayout
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.ui.theme.GotobrowserTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The edge swipe over the game end to end, as the browser screen assembles it: the root
 * layout holds the game with the toolbar's overlay on top, and the toolbar tells the root
 * where the swipe can start.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class ToolbarEdgeSwipeRevealTest {
    private companion object {
        // 1340x800 tablet: the game fills the screen but for about 3px on each side.
        const val NO_MARGIN = "sw800dp-w1340dp-h800dp-land-mdpi"

        // Wide phone: about 100dp of margin on each side of the game.
        const val WIDE_MARGIN = "sw411dp-w891dp-h411dp-land-mdpi"
    }

    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<ComponentActivity>? = null
    private val edgeSwipe = ToolbarEdgeSwipe()
    private val visible = mutableStateOf(false)
    private val swipeToReveal = mutableStateOf(true)
    private lateinit var root: ToolbarEdgeSwipeLayout

    /** What the game under the overlay received. */
    private val gameEvents = mutableListOf<Int>()

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun show(qualifiers: String) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        scenario = ActivityScenario.launch(ComponentActivity::class.java).also { s ->
            s.onActivity { activity ->
                root = ToolbarEdgeSwipeLayout(activity, edgeSwipe).apply {
                    addView(View(activity).apply {
                        setOnTouchListener { _, event -> gameEvents.add(event.actionMasked); true }
                    })
                    addView(ComposeView(activity).apply {
                        setContent {
                            GotobrowserTheme {
                                ControlLayout(
                                    visible = visible.value,
                                    onVisibleChange = { visible.value = it },
                                    onRefreshClick = {},
                                    onSettingsClick = {},
                                    swipeToReveal = swipeToReveal.value,
                                    edgeSwipe = edgeSwipe
                                ) {
                                    PanelButton(id = R.drawable.volume_off, onClick = {})
                                }
                            }
                        }
                    })
                }
                activity.setContentView(root)
            }
        }
        settle()
    }

    private fun settle() {
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
    }

    private val downTime = SystemClock.uptimeMillis()
    private var eventTime = downTime

    private fun touch(action: Int, x: Float, y: Float) {
        eventTime += 10
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        root.dispatchTouchEvent(event)
        event.recycle()
    }

    private fun swipeInFromTheLeftEdge() {
        touch(MotionEvent.ACTION_DOWN, 10f, 400f)
        touch(MotionEvent.ACTION_MOVE, 60f, 402f)
        touch(MotionEvent.ACTION_MOVE, 140f, 402f)
        touch(MotionEvent.ACTION_MOVE, 220f, 402f)
        touch(MotionEvent.ACTION_UP, 220f, 402f)
        settle()
    }

    @Test
    fun theHiddenToolbarPutsTheStripOverTheGameWhereThereIsNoMargin() {
        show(NO_MARGIN)
        assertEquals(GameGeometry.Rect(0f, 0f, 24f, 800f), edgeSwipe.strip)
        assertTrue(edgeSwipe.fromLeft)
    }

    @Test
    fun aSwipeInOverTheGameShowsTheToolbar() {
        show(NO_MARGIN)
        swipeInFromTheLeftEdge()

        assertTrue(visible.value)
        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_CANCEL), gameEvents)
        // Shown, the toolbar leaves that edge alone again.
        assertNull(edgeSwipe.strip)
    }

    @Test
    fun aTapOnTheStripPassesThroughTheOverlayToTheGame() {
        show(NO_MARGIN)
        touch(MotionEvent.ACTION_DOWN, 10f, 400f)
        touch(MotionEvent.ACTION_UP, 10f, 400f)
        settle()

        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP), gameEvents)
        assertFalse(visible.value)
    }

    @Test
    fun theStripComesBackWhenTheToolbarIsHiddenAgain() {
        show(NO_MARGIN)
        swipeInFromTheLeftEdge()
        assertNull(edgeSwipe.strip)

        visible.value = false
        settle()
        assertEquals(GameGeometry.Rect(0f, 0f, 24f, 800f), edgeSwipe.strip)
    }

    @Test
    fun turningTheSwipeSettingOffLeavesTheEdgeToTheGame() {
        show(NO_MARGIN)
        swipeToReveal.value = false
        settle()
        assertNull(edgeSwipe.strip)

        swipeInFromTheLeftEdge()
        assertFalse(visible.value)
        assertFalse(gameEvents.contains(MotionEvent.ACTION_CANCEL))

        swipeToReveal.value = true
        settle()
        assertEquals(GameGeometry.Rect(0f, 0f, 24f, 800f), edgeSwipe.strip)
    }

    @Test
    fun aScreenWithAMarginGetsNoStripOverTheGame() {
        show(WIDE_MARGIN)
        assertNull(edgeSwipe.strip)
    }

    @Test
    fun theStripGoesAwayWithTheScreen() {
        show(NO_MARGIN)
        assertEquals(GameGeometry.Rect(0f, 0f, 24f, 800f), edgeSwipe.strip)

        scenario?.close()
        scenario = null
        assertNull(edgeSwipe.strip)
    }
}
