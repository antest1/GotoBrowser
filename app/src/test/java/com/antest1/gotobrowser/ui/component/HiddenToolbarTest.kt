package com.antest1.gotobrowser.ui.component

import android.app.Application
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.antest1.gotobrowser.Activity.PanelButton
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.ui.theme.GotobrowserTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The hidden toolbar leaves nothing on screen: the controls are drawn on black and counted. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class HiddenToolbarTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @get:Rule
    val captures = TemporaryFolder()

    private var scenario: ActivityScenario<ComponentActivity>? = null
    private val visible = mutableStateOf(true)

    /** Colour of the ring around the refresh button, drawn outside it; null for no ring. */
    private val attention = mutableStateOf<Color?>(null)

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun show(qualifiers: String) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        scenario = ActivityScenario.launch(ComponentActivity::class.java).also { s ->
            s.onActivity { activity ->
                activity.setContent {
                    GotobrowserTheme {
                        Box(Modifier.fillMaxSize().background(Color.Black))
                        ControlLayout(
                            visible = visible.value,
                            onVisibleChange = { visible.value = it },
                            onRefreshClick = {},
                            onSettingsClick = {},
                            refreshAttention = attention.value
                        ) {
                            PanelButton(id = R.drawable.volume_off, onClick = {})
                            PanelButton(id = R.drawable.camera_icon, onClick = {})
                        }
                    }
                }
            }
        }
        settle()
    }

    private fun settle() {
        compose.mainClock.advanceTimeBy(4_000)
        compose.waitForIdle()
    }

    /** Pixels of the screen that are not the black background. */
    private fun litPixels(): Int {
        val file = File(captures.root, "screen.png")
        captureScreenRoboImage(file.path)
        val bitmap = BitmapFactory.decodeFile(file.path)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { (it shr 16 and 0xFF) + (it shr 8 and 0xFF) + (it and 0xFF) > 24 }
    }

    private fun assertNothingLeftWhenHidden(qualifiers: String) {
        show(qualifiers)
        // The capture does see the controls while they are shown.
        assertTrue(litPixels() > 0)
        visible.value = false
        settle()
        assertEquals(0, litPixels())
    }

    // Tablet tier (68dp refresh button on a 64dp bar) at 300dpi, where the button is 127.5px.
    @Test
    fun landscapeTabletAtAHalfPixelSize() = assertNothingLeftWhenHidden("sw640dp-w1066dp-h640dp-land-300dpi")

    // In portrait the bar's own height decides: phone tier (56dp) at 450dpi, where it is 157.5px.
    @Test
    fun portraitPhoneAtAHalfPixelSize() = assertNothingLeftWhenHidden("sw411dp-w411dp-h891dp-port-450dpi")

    // Large tablet tier (80dp button on a 72dp bar).
    @Test
    fun landscapeLargeTablet() = assertNothingLeftWhenHidden("sw800dp-w1280dp-h800dp-land-mdpi")

    @Test
    fun landscapePhone() = assertNothingLeftWhenHidden("sw411dp-w891dp-h411dp-land-420dpi")

    @Test
    fun theAttentionRingLeavesWithTheButton() {
        attention.value = Color.Red
        assertNothingLeftWhenHidden("sw640dp-w1066dp-h640dp-land-300dpi")
    }

    @Test
    fun theAttentionRingLeavesWithTheButtonInPortrait() {
        attention.value = Color.Red
        assertNothingLeftWhenHidden("sw411dp-w411dp-h891dp-port-420dpi")
    }

    @Test
    fun aRingThatComesUpWhileHiddenStaysOffScreen() {
        assertNothingLeftWhenHidden("sw411dp-w891dp-h411dp-land-420dpi")
        attention.value = Color.Red
        settle()
        assertEquals(0, litPixels())
    }
}
