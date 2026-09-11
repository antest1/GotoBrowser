package com.antest1.gotobrowser.Activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.antest1.gotobrowser.Browser.WebViewManager
import com.antest1.gotobrowser.BuildConfig
import com.antest1.gotobrowser.Constants.*
import com.antest1.gotobrowser.Helpers.BackPressCloseHandler
import com.antest1.gotobrowser.Helpers.KcEnUtils
import com.antest1.gotobrowser.Helpers.KcUtils
import com.antest1.gotobrowser.R
import com.antest1.gotobrowser.ui.theme.GotobrowserTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class EntranceActivity : ComponentActivity() {
    private lateinit var viewModel: EntranceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isTaskRoot) finish()

        viewModel = ViewModelProvider(this).get(EntranceViewModel::class.java)

        setContent {
            GotobrowserTheme {
                EntranceScreen(viewModel)
            }
        }

        WebViewManager.clearKcCacheProxy()
    }
}

// ViewModel-bound entry point. Observes state and delegates all rendering to the
// stateless EntranceScreenContent, which is what the IDE previews render.
@Composable
fun EntranceScreen(viewModel: EntranceViewModel) {
    val context = LocalContext.current

    val connector by viewModel.connector.observeAsState(CONN_DMM)
    val silentMode by viewModel.silentMode.observeAsState(false)
    val broadcastMode by viewModel.broadcastMode.observeAsState(false)
    val panelStart by viewModel.panelStart.observeAsState(false)

    EntranceScreenContent(
        connector = connector,
        silentMode = silentMode,
        broadcastMode = broadcastMode,
        panelStart = panelStart,
        onConnectorClick = { showConnectorSelectionDialog(context, viewModel) },
        onAutoCompleteClick = { showAutoCompleteDialog(context, viewModel) },
        onSilentChange = { viewModel.setSilentMode(it) },
        onBroadcastChange = { enabled ->
            viewModel.setBroadcastMode(enabled)
            if (viewModel.isKcanotifyInstalled && !enabled) {
                showKcanotifyBroadcastSetDialog(context, viewModel)
            }
        },
        onPanelChange = { viewModel.setPanelStart(it) },
        onStartClick = {
            val prefConnector = viewModel.sharedPref.getString(PREF_CONNECTOR, CONN_DMM)
            if (prefConnector != CONN_DMM) {
                showThirdPartyConnectorDialog(context, viewModel)
            } else {
                startBrowserActivity(context, viewModel)
            }
        },
        onCacheClearClick = { showCacheClearDialog(context, viewModel) },
        onManualClick = { openManual(context) },
        onSettingsClick = { openSettings(context) }
    )
}

// Stateless, side-effect-free layout. All state is passed in and every action is
// surfaced as a callback, so it is fully renderable in an IDE preview.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntranceScreenContent(
    connector: String,
    silentMode: Boolean,
    broadcastMode: Boolean,
    panelStart: Boolean,
    onConnectorClick: () -> Unit,
    onAutoCompleteClick: () -> Unit,
    onSilentChange: (Boolean) -> Unit,
    onBroadcastChange: (Boolean) -> Unit,
    onPanelChange: (Boolean) -> Unit,
    onStartClick: () -> Unit,
    onCacheClearClick: () -> Unit,
    onManualClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Responsive sizing: keep the layout compact enough to fit a short landscape screen.
    val topSpacerHeight = if (isLandscape) 24.dp else 180.dp
    val connectorTextSize = if (isLandscape) 20.sp else 24.sp
    val connectorTextPaddingH = if (isLandscape) 40.dp else 60.dp
    val cardHorizontalMargin = if (isLandscape) 24.dp else 60.dp
    val cardMaxWidth = if (isLandscape) 420.dp else 560.dp
    val cardVerticalPadding = if (isLandscape) 10.dp else 15.dp
    val startButtonHeight = if (isLandscape) 46.dp else 56.dp
    val sectionSpacing = if (isLandscape) 8.dp else 15.dp

    // Using a Surface as the root to ensure a solid background base
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF262933)) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background layer - lowering alpha blends with the dark base,
            // which softens contrast without needing a color transform.
            Image(
                painter = painterResource(id = R.mipmap.background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.4f,
                contentScale = ContentScale.Crop
            )

            Image(
                painter = painterResource(id = R.mipmap.gotland_full),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = if (isLandscape) 0.dp else 80.dp),
                contentScale = if (isLandscape) ContentScale.Fit else ContentScale.Crop,
                alignment = if (isLandscape) Alignment.Center else Alignment.TopCenter
            )

            // Scrollable content (no Scaffold/topBar: the top buttons are overlaid below,
            // so nothing reserves layout space and the artwork is not clipped at the top).
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(topSpacerHeight))

                // Connector Selection Card
                Card(
                    onClick = onConnectorClick,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0x80283593) // colorSiteSelection
                    ),
                    modifier = Modifier.widthIn(min = 200.dp, max = cardMaxWidth)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = connector.uppercase(),
                            color = Color.White,
                            fontSize = connectorTextSize,
                            modifier = Modifier.padding(horizontal = connectorTextPaddingH, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = onAutoCompleteClick,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.passkey_icon),
                                contentDescription = "Autocomplete",
                                tint = Color(0xFFFFC400) // colorAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Switches Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0x40000000)
                    ),
                    modifier = Modifier
                        .widthIn(max = cardMaxWidth)
                        .padding(horizontal = cardHorizontalMargin)
                ) {
                    Column(modifier = Modifier.padding(cardVerticalPadding)) {
                        SwitchItem(
                            text = stringResource(id = R.string.mode_silent),
                            checked = silentMode,
                            onCheckedChange = onSilentChange,
                            enabled = connector == CONN_DMM
                        )
                        SwitchItem(
                            text = stringResource(id = R.string.mode_broadcast),
                            checked = broadcastMode,
                            onCheckedChange = onBroadcastChange
                        )
                        SwitchItem(
                            text = stringResource(id = R.string.mode_show_panel),
                            checked = panelStart,
                            onCheckedChange = onPanelChange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(sectionSpacing))

                // Start Button
                Button(
                    onClick = onStartClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF283593) // colorButton
                    ),
                    shape = CircleShape,
                    modifier = Modifier.height(startButtonHeight).border(2.dp, Color(0xFFFFC400), CircleShape)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("START", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Text(
                    text = stringResource(id = R.string.cache_clear_text),
                    color = Color(0xFFFFC400), // colorAccent
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { onCacheClearClick() }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Top-right buttons (manual / settings) as a lightweight overlay
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onManualClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.help_icon),
                        contentDescription = "Manual",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.settings),
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }

            // Bottom Info Labels
            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                Text(
                    text = String.format(Locale.US, stringResource(id = R.string.version_format), BuildConfig.VERSION_NAME),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
                Text(
                    text = String.format(Locale.US, stringResource(id = R.string.copyright_format), Calendar.getInstance().get(Calendar.YEAR)),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Preview(name = "Entrance - Portrait", showBackground = true, widthDp = 411, heightDp = 823)
@Composable
fun EntranceScreenPreview() {
    GotobrowserTheme {
        EntranceScreenContent(
            connector = CONN_DMM,
            silentMode = false,
            broadcastMode = true,
            panelStart = true,
            onConnectorClick = {},
            onAutoCompleteClick = {},
            onSilentChange = {},
            onBroadcastChange = {},
            onPanelChange = {},
            onStartClick = {},
            onCacheClearClick = {},
            onManualClick = {},
            onSettingsClick = {}
        )
    }
}

@Preview(name = "Entrance - Landscape", showBackground = true, widthDp = 823, heightDp = 411)
@Composable
fun EntranceScreenLandscapePreview() {
    GotobrowserTheme {
        EntranceScreenContent(
            connector = CONN_DMM,
            silentMode = false,
            broadcastMode = true,
            panelStart = true,
            onConnectorClick = {},
            onAutoCompleteClick = {},
            onSilentChange = {},
            onBroadcastChange = {},
            onPanelChange = {},
            onStartClick = {},
            onCacheClearClick = {},
            onManualClick = {},
            onSettingsClick = {}
        )
    }
}

@Composable
fun SwitchItem(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 15.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF333C75),
                checkedTrackColor = Color(0xFFAAB1BF),
                uncheckedThumbColor = Color(0xFFD3D3D3),
                uncheckedTrackColor = Color(0xFF30374A)
            )
        )
    }
}

private fun openSettings(context: Context) {
    val intent = Intent(context, SettingsActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openManual(context: Context) {
    val url = context.getString(R.string.manual_link)
    val intentBuilder = CustomTabsIntent.Builder()
    intentBuilder.setShowTitle(true)
    val params = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(ContextCompat.getColor(context, R.color.colorSettingsBackground))
        .build()
    intentBuilder.setDefaultColorSchemeParams(params)
    intentBuilder.setUrlBarHidingEnabled(true)

    val customTabsIntent = intentBuilder.build()
    val customTabsApps = context.packageManager.queryIntentActivities(customTabsIntent.intent, 0)
    if (customTabsApps.isNotEmpty()) {
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } else {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(browserIntent)
    }
}

private fun showConnectorSelectionDialog(context: Context, viewModel: EntranceViewModel) {
    val listItems = context.resources.getStringArray(R.array.connector_list)
    val currentConnector = viewModel.connector.value
    val connectorIdx = listItems.indexOf(currentConnector)

    MaterialAlertDialogBuilder(context)
        .setTitle(context.getString(R.string.select_server))
        .setSingleChoiceItems(listItems, connectorIdx) { dialog, i ->
            viewModel.setConnector(listItems[i])
            viewModel.sharedPref.edit().putString(PREF_LATEST_URL, URL_LIST[i]).apply()
            KcUtils.showToast(context.applicationContext, URL_LIST[i])
            dialog.dismiss()
        }
        .show()
}

private fun showAutoCompleteDialog(context: Context, viewModel: EntranceViewModel) {
    val dialogView = LayoutInflater.from(context).inflate(R.layout.login_form, null)
    val formEmail = dialogView.findViewById<EditText>(R.id.input_id)
    val formPassword = dialogView.findViewById<EditText>(R.id.input_pw)
    formEmail.setText(viewModel.sharedPref.getString(PREF_DMM_ID, ""))
    formPassword.setText(viewModel.sharedPref.getString(PREF_DMM_PASS, ""))

    MaterialAlertDialogBuilder(context)
        .setView(dialogView)
        .setPositiveButton(R.string.text_save) { dialog, _ ->
            val loginId = formEmail.text.toString()
            val loginPassword = formPassword.text.toString()
            viewModel.sharedPref.edit().putString(PREF_DMM_ID, loginId).apply()
            viewModel.sharedPref.edit().putString(PREF_DMM_PASS, loginPassword).apply()
            dialog.dismiss()
        }
        .setNegativeButton(R.string.text_cancel) { dialog, _ -> dialog.cancel() }
        .show()
}

private fun showCacheClearDialog(context: Context, viewModel: EntranceViewModel) {
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.cache_clear_text)
        .setCancelable(false)
        .setMessage(context.getString(R.string.clearcache_msg))
        .setPositiveButton(R.string.action_ok) { dialog, _ ->
            viewModel.clearBrowserCache()
            KcUtils.showToast(context.applicationContext, R.string.cache_cleared_toast)
            dialog.dismiss()
        }
        .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.cancel() }
        .show()
}

private fun showKcanotifyBroadcastSetDialog(context: Context, viewModel: EntranceViewModel) {
    MaterialAlertDialogBuilder(context)
        .setTitle(context.getString(R.string.kcanotify_broadcast_dialog_title))
        .setCancelable(false)
        .setMessage(String.format(Locale.US, context.getString(R.string.kcanotify_broadcast_dialog_message),
            context.getString(R.string.mode_broadcast), context.getString(R.string.action_ok)))
        .setPositiveButton(R.string.action_ok) { dialog, _ ->
            viewModel.setBroadcastMode(true)
            dialog.dismiss()
        }
        .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.cancel() }
        .show()
}

private fun showThirdPartyConnectorDialog(context: Context, viewModel: EntranceViewModel) {
    val disclaimed = viewModel.sharedPref.getBoolean(PREF_TP_DISCLAIMED, false)
    if (disclaimed) {
        startBrowserActivity(context, viewModel)
        return
    }

    MaterialAlertDialogBuilder(context)
        .setTitle("Disclaimer")
        .setCancelable(false)
        .setMessage(context.getString(R.string.thirdpartyconnector_msg))
        .setPositiveButton(R.string.action_ok) { dialog, _ ->
            viewModel.sharedPref.edit().putBoolean(PREF_TP_DISCLAIMED, true).apply()
            dialog.dismiss()
            startBrowserActivity(context, viewModel)
        }
        .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.cancel() }
        .show()
}

private fun startBrowserActivity(context: Context, viewModel: EntranceViewModel) {
    val sharedPref = viewModel.sharedPref
    val prefConnector = sharedPref.getString(PREF_CONNECTOR, CONN_DMM)
    val prefKeyboardOn = sharedPref.getBoolean(PREF_KEYBOARD, true)
    val prefPanelStart = sharedPref.getBoolean(PREF_PANELSTART, true)
    val intent = Intent(context, BrowserActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.action = WebViewManager.OPEN_KANCOLLE

    var options = ""
    if (prefPanelStart) options = options.plus(ACTION_SHOWPANEL)
    if (prefKeyboardOn) options = options.plus(ACTION_SHOWKEYBOARD)
    intent.putExtra("options", options)

    val loginId = sharedPref.getString(PREF_DMM_ID, "")
    val loginPassword = sharedPref.getString(PREF_DMM_PASS, "")
    intent.putExtra("login_id", loginId)
    intent.putExtra("login_pw", loginPassword)

    val prefAlterGadget = sharedPref.getBoolean(PREF_ALTER_GADGET, false)
    val isProxyMethod = sharedPref.getString(PREF_ALTER_METHOD, "") == PREF_ALTER_METHOD_PROXY
    val alterEndpoint = sharedPref.getString(PREF_ALTER_ENDPOINT, "")

    if (prefAlterGadget && isProxyMethod && prefConnector == CONN_DMM) {
        WebViewManager.setKcCacheProxy(alterEndpoint, {
            context.startActivity(intent)
            if (context is EntranceActivity) context.finish()
        }, {
            KcUtils.showToast(context.applicationContext, R.string.setting_alter_method_proxy_error_toast)
        })
    } else {
        context.startActivity(intent)
        if (context is EntranceActivity) context.finish()
    }
}
