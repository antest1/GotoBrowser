package com.antest1.gotobrowser.ui.component

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antest1.gotobrowser.Activity.SettingsViewModel
import com.antest1.gotobrowser.Helpers.chs.ChsConfig
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.ui.theme.SettingsTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
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
    }

    @Test
    fun testExpandedRowsAndDownloadDialog() {
        ChsConfig.setBoolean(ChsConfig.KEY_ENABLED, true)

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

        val downloadTitle = app.getString(R.string.settings_mod_chs_download)
        val aboutTitle = app.getString(R.string.settings_mod_chs_about)

        compose.onNodeWithText(downloadTitle).assertIsDisplayed()
        compose.onNodeWithText(aboutTitle).assertIsDisplayed()

        // 点击下载，验证弹窗标题与初次下载推荐全量包选项
        compose.onNodeWithText(downloadTitle).performClick()
        compose.waitForIdle()

        val dialogTitle = app.getString(R.string.settings_mod_chs_download_dialog_title)
        val recommendedArchive = app.getString(R.string.settings_mod_chs_download_archive_recommended)
        compose.onNodeWithText(dialogTitle).assertIsDisplayed()
        compose.onNodeWithText(recommendedArchive).assertIsDisplayed()

        // 取消弹窗
        compose.onNodeWithText(app.getString(R.string.action_cancel)).performClick()
        compose.waitForIdle()

        // 点击关于，验证关于弹窗内容显示
        compose.onNodeWithText(aboutTitle).performClick()
        compose.waitForIdle()
        val dialogContent = app.getString(R.string.settings_mod_chs_about_dialog_text)
        compose.onNodeWithText(dialogContent).assertIsDisplayed()
    }
}
