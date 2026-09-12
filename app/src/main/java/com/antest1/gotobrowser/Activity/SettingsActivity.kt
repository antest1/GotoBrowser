package com.antest1.gotobrowser.Activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT
import com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET
import com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD
import com.antest1.gotobrowser.Constants.PREF_BROADCAST
import com.antest1.gotobrowser.Constants.PREF_CURSOR_MODE
import com.antest1.gotobrowser.Constants.PREF_DEVTOOLS_DEBUG
import com.antest1.gotobrowser.Constants.PREF_DOWNLOAD_RETRY
import com.antest1.gotobrowser.Constants.PREF_KEYBOARD
import com.antest1.gotobrowser.Constants.PREF_LANDSCAPE
import com.antest1.gotobrowser.Constants.PREF_MOD_CRIT
import com.antest1.gotobrowser.Constants.PREF_MOD_FPS
import com.antest1.gotobrowser.Constants.PREF_MOD_KANTAI3D
import com.antest1.gotobrowser.Constants.PREF_MOD_KCCP_LANG_PATCH
import com.antest1.gotobrowser.Constants.PREF_MULTIWIN_MARGIN
import com.antest1.gotobrowser.Constants.PREF_PIP_MODE
import com.antest1.gotobrowser.Constants.PREF_SETTINGS
import com.antest1.gotobrowser.Constants.PREF_SUBTITLE_FONTSIZE
import com.antest1.gotobrowser.Constants.PREF_SUBTITLE_UPDATE
import com.antest1.gotobrowser.Constants.PREF_USE_EXTCACHE
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.ui.theme.GotobrowserTheme

class SettingsActivity : AppCompatActivity() {
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        setContent {
            GotobrowserTheme {
                SettingsScreen(
                    onBack = { finish() },
                    viewModel = viewModel
                )
            }
        }
        createNotificationChannel()
    }

    companion object {
        const val CHANNEL_ID = "gotobrowser_screenshot"

        @JvmStatic
        fun setInitialSettings(sharedPref: SharedPreferences) {
            val editor = sharedPref.edit()
            for (key in PREF_SETTINGS) {
                if (!sharedPref.contains(key)) {
                    when (key) {
                        PREF_LANDSCAPE, PREF_KEYBOARD, PREF_SUBTITLE_UPDATE -> editor.putBoolean(key, true)
                        PREF_ADJUSTMENT, PREF_BROADCAST, PREF_USE_EXTCACHE,
                        PREF_PIP_MODE, PREF_MULTIWIN_MARGIN, PREF_ALTER_GADGET,
                        PREF_DOWNLOAD_RETRY, PREF_MOD_KANTAI3D, PREF_MOD_KCCP_LANG_PATCH,
                        PREF_MOD_FPS, PREF_MOD_CRIT, PREF_DEVTOOLS_DEBUG -> editor.putBoolean(key, false)
                        PREF_CURSOR_MODE -> editor.putString(key, "1")
                        PREF_ALTER_METHOD -> editor.putString(key, "1")
                        PREF_SUBTITLE_FONTSIZE -> editor.putInt(key, 18)
                    }
                }
            }
            editor.apply()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    class SettingsFragment : androidx.preference.PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}

// ViewModel-bound entry point. Delegates rendering to the stateless
// SettingsScreenContent, supplying the real PreferenceFragment as the body.
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel) {
    SettingsScreenContent(
        onBack = onBack,
        content = {
            AndroidView(
                factory = { context ->
                    val frameLayout = android.widget.FrameLayout(context).apply {
                        id = android.view.View.generateViewId()
                    }
                    val activity = context as androidx.fragment.app.FragmentActivity
                    activity.supportFragmentManager.beginTransaction()
                        .replace(frameLayout.id, SettingsActivity.SettingsFragment())
                        .commit()
                    frameLayout
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}

// Stateless scaffold + top bar. The body is a slot so the IDE preview can
// supply representative content (a PreferenceFragment cannot render in Preview).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            content()
        }
    }
}

// A representative settings row, mimicking how a Material preference looks.
@Composable
private fun SettingsSampleRow(
    title: String,
    summary: String? = null,
    switchChecked: Boolean? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (switchChecked != null) {
            Switch(checked = switchChecked, onCheckedChange = {})
        }
    }
}

// Sample list so the preview shows representative content instead of a blank body.
@Composable
private fun SettingsSampleList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsSampleRow(title = "Use landscape mode", switchChecked = true)
        SettingsSampleRow(title = "Show keyboard at start", switchChecked = true)
        SettingsSampleRow(title = "Broadcast to Kcanotify", switchChecked = false)
        SettingsSampleRow(title = "Use external cache", switchChecked = false)
        SettingsSampleRow(title = "Enable picture-in-picture", switchChecked = false)
        SettingsSampleRow(title = "Cursor mode", summary = "1")
        SettingsSampleRow(title = "Subtitle update", switchChecked = true)
        SettingsSampleRow(title = "Subtitle font size", summary = "18")
        SettingsSampleRow(title = "Log level", summary = "1")
        SettingsSampleRow(title = "Reset settings", summary = "Restore all defaults")
        SettingsSampleRow(title = "About", summary = "Version information")
    }
}

@Preview(name = "Settings - Portrait", showBackground = true, widthDp = 411, heightDp = 823)
@Composable
fun SettingsScreenPreview() {
    GotobrowserTheme {
        SettingsScreenContent(
            onBack = {},
            content = { SettingsSampleList() }
        )
    }
}

@Preview(name = "Settings - Landscape", showBackground = true, widthDp = 823, heightDp = 411)
@Composable
fun SettingsScreenLandscapePreview() {
    GotobrowserTheme {
        SettingsScreenContent(
            onBack = {},
            content = { SettingsSampleList() }
        )
    }
}
