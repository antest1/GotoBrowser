package com.antest1.gotobrowser.Activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import com.antest1.gotobrowser.Constants.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel) {
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
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}
