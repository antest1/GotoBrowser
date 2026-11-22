package com.antest1.gotobrowser.ui.component

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.antest1.gotobrowser.Activity.SettingsViewModel
import com.antest1.gotobrowser.Helpers.chs.ChsConfig
import com.antest1.gotobrowser.Helpers.chs.ChsPaths
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.ui.theme.SettingsTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ChsSettingsSectionTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<ComponentActivity>? = null
    private lateinit var app: Application
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(
                app,
                FirebaseOptions.Builder()
                    .setApplicationId("1:000000000000:android:0000000000000000")
                    .setProjectId("test")
                    .setApiKey("test")
                    .build()
            )
        }
        ChsConfig.init(app)
        viewModel = SettingsViewModel(app)
    }

    @After
    fun tearDown() {
        scenario?.close()
        ChsConfig.setEnabled(false)
        ChsPaths.getRootDir(app).deleteRecursively()
    }

    private fun showSection() {
        scenario = ActivityScenario.launch(ComponentActivity::class.java).also { s ->
            s.onActivity { a ->
                a.setContent {
                    SettingsTheme {
                        ChsSettingsSection(
                            viewModel = viewModel,
                            onChsToggled = {},
                            onSettingChanged = {}
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun testDownloadDialogRecommendsIncrementalUpdateWhenDataIsInstalled() {
        ChsConfig.setEnabled(true)
        File(ChsPaths.getTextDictDir(app), "sample.json").apply {
            parentFile?.mkdirs()
            writeText("""{"a":"b"}""")
        }
        showSection()

        compose.onNodeWithText(app.getString(R.string.settings_mod_chs_download)).performClick()

        // The installed-data check runs on Dispatchers.IO, which compose idling does not track.
        val recommendedPatch = app.getString(R.string.settings_mod_chs_download_patch_recommended)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(recommendedPatch).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(recommendedPatch).assertIsDisplayed()
    }

    @Test
    fun testExpandedRowsAndDownloadDialog() {
        ChsConfig.setEnabled(true)
        showSection()

        val downloadTitle = app.getString(R.string.settings_mod_chs_download)
        val aboutTitle = app.getString(R.string.settings_mod_chs_about)

        compose.onNodeWithText(downloadTitle).assertIsDisplayed()
        compose.onNodeWithText(aboutTitle).assertIsDisplayed()

        // Click download and verify dialog title and recommended full-archive option
        compose.onNodeWithText(downloadTitle).performClick()
        compose.waitForIdle()

        val dialogTitle = app.getString(R.string.settings_mod_chs_download_dialog_title)
        val recommendedArchive = app.getString(R.string.settings_mod_chs_download_archive_recommended)
        compose.onNodeWithText(dialogTitle).assertIsDisplayed()
        compose.onNodeWithText(recommendedArchive).assertIsDisplayed()

        // Dismiss dialog
        compose.onNodeWithText(app.getString(R.string.action_cancel)).performClick()
        compose.waitForIdle()

        // Click about and verify about dialog content
        compose.onNodeWithText(aboutTitle).performClick()
        compose.waitForIdle()
        val dialogContent = app.getString(R.string.settings_mod_chs_about_dialog_text)
        compose.onNodeWithText(dialogContent).assertIsDisplayed()
    }
}
