package com.antest1.gotobrowser.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.antest1.gotobrowser.Browser.WebViewManager;
import com.antest1.gotobrowser.BuildConfig;
import com.antest1.gotobrowser.Helpers.BackPressCloseHandler;
import com.antest1.gotobrowser.Helpers.GotoVersionCheck;
import com.antest1.gotobrowser.Helpers.KcEnUtils;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.R;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import static com.antest1.gotobrowser.Constants.ACTION_SHOWKEYBOARD;
import static com.antest1.gotobrowser.Constants.ACTION_SHOWPANEL;
import static com.antest1.gotobrowser.Constants.CONN_DMM;
import static com.antest1.gotobrowser.Constants.GITHUBAPI_ROOT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_ENDPOINT;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_GADGET;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD;
import static com.antest1.gotobrowser.Constants.PREF_ALTER_METHOD_PROXY;
import static com.antest1.gotobrowser.Constants.PREF_CONNECTOR;
import static com.antest1.gotobrowser.Constants.PREF_DMM_ID;
import static com.antest1.gotobrowser.Constants.PREF_DMM_PASS;
import static com.antest1.gotobrowser.Constants.PREF_KEYBOARD;
import static com.antest1.gotobrowser.Constants.PREF_LATEST_URL;
import static com.antest1.gotobrowser.Constants.PREF_BROADCAST;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAIEN;
import static com.antest1.gotobrowser.Constants.PREF_PANELSTART;
import static com.antest1.gotobrowser.Constants.PREF_SILENT;
import static com.antest1.gotobrowser.Constants.PREF_TP_DISCLAIMED;
import static com.antest1.gotobrowser.Constants.URL_LIST;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.clearApplicationCache;
import static com.antest1.gotobrowser.Helpers.KcUtils.getRetrofitAdapter;

public class EntranceActivity extends AppCompatActivity {
    private BackPressCloseHandler backPressCloseHandler;
    private SharedPreferences sharedPref;
    private TextView selectButton;
    private VersionDatabase versionTable;
    private GotoVersionCheck appCheck;
    private boolean kcanotifyInstalledFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isTaskRoot()) finish();
        setContentView(R.layout.activity_entrance);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        appCheck = getRetrofitAdapter(getApplicationContext(), GITHUBAPI_ROOT).create(GotoVersionCheck.class);
        KcUtils.requestLatestAppVersion(this, appCheck, true);

        kcanotifyInstalledFlag = KcUtils.isKcanotifyInstalled(getApplicationContext());

        versionTable = new VersionDatabase(getApplicationContext(), null, VERSION_TABLE_VERSION);
        backPressCloseHandler = new BackPressCloseHandler(this);
        sharedPref = getSharedPreferences(getString(R.string.preference_key), Context.MODE_PRIVATE);
        SettingsActivity.setInitialSettings(sharedPref);

        SharedPreferences.Editor editor = sharedPref.edit();

        ImageView settingsButton = findViewById(R.id.icon_setting);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(EntranceActivity.this, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        ImageView msgButton = findViewById(R.id.icon_manual);
        msgButton.setOnClickListener(v -> {
            String url = getString(R.string.manual_link);
            CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
            intentBuilder.setShowTitle(true);
            CustomTabColorSchemeParams params = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorSettingsBackground))
                    .build();
            intentBuilder.setDefaultColorSchemeParams(params);
            intentBuilder.setUrlBarHidingEnabled(true);

            final CustomTabsIntent customTabsIntent = intentBuilder.build();
            final List<ResolveInfo> customTabsApps = getPackageManager().queryIntentActivities(customTabsIntent.intent, 0);
            if (customTabsApps.size() > 0) {
                customTabsIntent.launchUrl(EntranceActivity.this, Uri.parse(url));
            } else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });

        SwitchCompat silentSwitch = findViewById(R.id.switch_silent);
        silentSwitch.setChecked(sharedPref.getBoolean(PREF_SILENT, false));
        silentSwitch.setOnCheckedChangeListener((buttonView, isChecked)
                -> editor.putBoolean(PREF_SILENT, isChecked).apply());

        SwitchCompat broadcastSwitch = findViewById(R.id.switch_broadcast);
        broadcastSwitch.setChecked(sharedPref.getBoolean(PREF_BROADCAST, false));
        broadcastSwitch.setOnCheckedChangeListener((buttonView, isChecked)
                -> {
                    editor.putBoolean(PREF_BROADCAST, isChecked).apply();
                    if (kcanotifyInstalledFlag && !isChecked) showKcanotifyBroadcastSetDialog();
                }
        );

        SwitchCompat gadgetSwitch = findViewById(R.id.switch_gadget);
        gadgetSwitch.setChecked(sharedPref.getBoolean(PREF_ALTER_GADGET, false));
        gadgetSwitch.setOnCheckedChangeListener((buttonView, isChecked)
                -> editor.putBoolean(PREF_ALTER_GADGET, isChecked).apply()
        );

        CheckBox showControlPanelCheckbox = findViewById(R.id.layout_control);
        showControlPanelCheckbox.setChecked(sharedPref.getBoolean(PREF_PANELSTART, false));
        showControlPanelCheckbox.setOnCheckedChangeListener((buttonView, isChecked)
                -> editor.putBoolean(PREF_PANELSTART, isChecked).apply());

        CheckBox showKeyboardCheckbox = findViewById(R.id.layout_keyboard);
        showKeyboardCheckbox.setChecked(sharedPref.getBoolean(PREF_KEYBOARD, true));
        showKeyboardCheckbox.setOnCheckedChangeListener((buttonView, isChecked)
                -> editor.putBoolean(PREF_KEYBOARD, isChecked).apply());

        selectButton = findViewById(R.id.connector_select);
        selectButton.setOnClickListener(v -> showConnectorSelectionDialog());
        String connector = sharedPref.getString(PREF_CONNECTOR, CONN_DMM);
        silentSwitch.setEnabled(CONN_DMM.equals(connector));
        selectButton.setText(connector);

        TextView autoCompleteButton = findViewById(R.id.webview_autocomplete);
        autoCompleteButton.setOnClickListener(v -> showAutoCompleteDialog());

        TextView clearButton = findViewById(R.id.webview_clear);
        clearButton.setOnClickListener(v -> showCacheClearDialog());

        TextView startButton = findViewById(R.id.webview_start);
        startButton.setOnClickListener(v -> {
            String pref_connector = sharedPref.getString(PREF_CONNECTOR, CONN_DMM);
            if (!pref_connector.equals(CONN_DMM)) {
                showThirdPartyConnectorDialog();
            } else {
                startBrowserActivity();
            }
        });

        TextView versionText = findViewById(R.id.version_info);
        versionText.setText(String.format(Locale.US,
                getString(R.string.version_format), BuildConfig.VERSION_NAME));

        TextView copyrightText = findViewById(R.id.copyright);
        copyrightText.setText(String.format(Locale.US,
                getString(R.string.copyright_format),Calendar.getInstance().get(Calendar.YEAR)));

        WebViewManager.clearKcCacheProxy();

        if (kcanotifyInstalledFlag && !sharedPref.getBoolean(PREF_BROADCAST, false)) {
            showKcanotifyBroadcastSetDialog();
        }

        KcEnUtils enUtils = new KcEnUtils();
        if (sharedPref.getBoolean(PREF_MOD_KANTAIEN, false)) {
            String availableVersion = enUtils.checkKantaiEnUpdateEntrance(this);
            if (availableVersion != null) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle(R.string.settings_mod_kantaien_enable);
                alertDialogBuilder
                        .setCancelable(false)
                        .setMessage(String.format(Locale.US, this.getString(R.string.setting_latest_download), availableVersion))
                        .setPositiveButton(R.string.action_ok,
                                (dialog, id) -> {
                                    enUtils.requestPatchUpdateEntrance(this);
                                    dialog.dismiss();
                                })
                        .setNegativeButton(R.string.action_cancel,
                                (dialog, id) -> dialog.cancel());
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SwitchCompat gadgetSwitch = findViewById(R.id.switch_gadget);
        gadgetSwitch.setChecked(sharedPref.getBoolean(PREF_ALTER_GADGET, false));
    }

    @Override
    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showConnectorSelectionDialog() {
        SwitchCompat silentSwitch = findViewById(R.id.switch_silent);
        final String[] listItems = getResources().getStringArray(R.array.connector_list);
        int connector_idx = -1;
        String connector1 = sharedPref.getString(PREF_CONNECTOR, CONN_DMM);
        for (int i = 0; i < listItems.length; i++) {
            if (listItems[i].equals(connector1)) {
                connector_idx = i;
                break;
            }
        }
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(EntranceActivity.this);
        mBuilder.setTitle(getString(R.string.select_server));
        mBuilder.setSingleChoiceItems(listItems, connector_idx, (dialogInterface, i) -> {
            silentSwitch.setEnabled(i==0);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(PREF_CONNECTOR, listItems[i]);
            editor.putString(PREF_LATEST_URL, URL_LIST[i]);
            editor.apply();
            selectButton.setText(listItems[i]);
            KcUtils.showToast(getApplicationContext(), URL_LIST[i]);
            dialogInterface.dismiss();
        });
        AlertDialog mDialog = mBuilder.create();
        mDialog.show();
    }

    private void showAutoCompleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(EntranceActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.login_form, null);
        final EditText formEmail = dialogView.findViewById(R.id.input_id);
        final EditText formPassword = dialogView.findViewById(R.id.input_pw);
        formEmail.setText(sharedPref.getString(PREF_DMM_ID, ""));
        formPassword.setText(sharedPref.getString(PREF_DMM_PASS, ""));
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.text_save, (dialog, which) -> {
            String login_id = formEmail.getText().toString();
            String login_password = formPassword.getText().toString();
            sharedPref.edit().putString(PREF_DMM_ID, login_id).apply();
            sharedPref.edit().putString(PREF_DMM_PASS, login_password).apply();
            dialog.dismiss();
        });
        builder.setNegativeButton(R.string.text_cancel, (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showCacheClearDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(EntranceActivity.this);
        alertDialogBuilder.setTitle(R.string.cache_clear_text);
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(getString(R.string.clearcache_msg))
                .setPositiveButton(R.string.action_ok,
                        (dialog, id) -> {
                            clearBrowserCache();
                            KcUtils.showToast(getApplicationContext(), R.string.cache_cleared_toast);
                            dialog.dismiss();
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void showKcanotifyBroadcastSetDialog() {
        SwitchCompat broadcastSwitch = findViewById(R.id.switch_broadcast);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(EntranceActivity.this);
        alertDialogBuilder.setTitle(getString(R.string.kcanotify_broadcast_dialog_title));
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(String.format(Locale.US, getString(R.string.kcanotify_broadcast_dialog_message),
                        getString(R.string.mode_broadcast), getString(R.string.action_ok)))
                .setPositiveButton(R.string.action_ok,
                        (dialog, id) -> {
                            broadcastSwitch.setChecked(true);
                            dialog.dismiss();
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    private void clearBrowserCache() {
        WebView webview = new WebView(getApplicationContext());
        webview.clearCache(true);
        versionTable.clearVersionDatabase();
        String cache_dir = KcUtils.getAppCacheFileDir(getApplicationContext(), "/cache/");
        String patched_cache_dir = KcUtils.getAppCacheFileDir(getApplicationContext(), "/_patched_cache/");
        clearApplicationCache(getApplicationContext(), getCacheDir());
        clearApplicationCache(getApplicationContext(), new File(cache_dir));
        clearApplicationCache(getApplicationContext(), new File(patched_cache_dir));
    }

    private void startBrowserActivity() {
        String pref_connector = sharedPref.getString(PREF_CONNECTOR, CONN_DMM);
        Intent intent = new Intent(EntranceActivity.this, BrowserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(WebViewManager.OPEN_KANCOLLE);

        String options = "";
        CheckBox showControlPanelCheckbox = findViewById(R.id.layout_control);
        CheckBox showKeyboardCheckbox = findViewById(R.id.layout_keyboard);
        if (showControlPanelCheckbox.isChecked()) options = options.concat(ACTION_SHOWPANEL);
        if (showKeyboardCheckbox.isChecked()) options = options.concat(ACTION_SHOWKEYBOARD);
        intent.putExtra("options", options);

        String login_id = sharedPref.getString(PREF_DMM_ID, "");
        String login_password = sharedPref.getString(PREF_DMM_PASS, "");
        intent.putExtra("login_id", login_id);
        intent.putExtra("login_pw", login_password);

        boolean prefAlterGadget = sharedPref.getBoolean(PREF_ALTER_GADGET, false);
        boolean isProxyMethod = sharedPref.getString(PREF_ALTER_METHOD, "").equals(PREF_ALTER_METHOD_PROXY);
        String alterEndpoint = sharedPref.getString(PREF_ALTER_ENDPOINT, "");

        if (prefAlterGadget && isProxyMethod && pref_connector.equals(CONN_DMM)) {
            WebViewManager.setKcCacheProxy(alterEndpoint, () -> {
                        startActivity(intent);
                        finish();
                    },
                    () -> {
                        KcUtils.showToast(getApplicationContext(), R.string.setting_alter_method_proxy_error_toast);
                    });
        } else {
            startActivity(intent);
            finish();
        }
    }

    private void showThirdPartyConnectorDialog() {
        boolean disclaimed = sharedPref.getBoolean(PREF_TP_DISCLAIMED, false);
        if (disclaimed) {
            startBrowserActivity();
            return;
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(EntranceActivity.this);
        alertDialogBuilder.setTitle("Disclaimer");
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(getString(R.string.thirdpartyconnector_msg))
                .setPositiveButton(R.string.action_ok,
                        (dialog, id) -> {
                            sharedPref.edit().putBoolean(PREF_TP_DISCLAIMED, true).apply();
                            dialog.dismiss();
                            startBrowserActivity();
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
