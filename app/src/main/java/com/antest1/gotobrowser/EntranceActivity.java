package com.antest1.gotobrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import static com.antest1.gotobrowser.Constants.ACTION_WITHLC;
import static com.antest1.gotobrowser.Constants.PREF_ADJUSTMENT;
import static com.antest1.gotobrowser.Constants.PREF_CONNECTOR;
import static com.antest1.gotobrowser.Constants.PREF_DMM_ID;
import static com.antest1.gotobrowser.Constants.PREF_DMM_PASS;
import static com.antest1.gotobrowser.Constants.PREF_LANDSCAPE;
import static com.antest1.gotobrowser.Constants.PREF_LATEST_URL;
import static com.antest1.gotobrowser.Constants.PREF_PADDING;
import static com.antest1.gotobrowser.Constants.PREF_SILENT;
import static com.antest1.gotobrowser.Constants.RESIZE_OSAPI;
import static com.antest1.gotobrowser.Constants.URL_LIST;

public class EntranceActivity extends AppCompatActivity {
    private BackPressCloseHandler backPressCloseHandler;
    private TextView startButton, selectButton, clearButton, autoCompleteButton, versionText;
    private Switch landscapeSwitch, adjustmentSwitch, silentSwitch;
    private CheckBox manualControlCheckbox;
    private boolean manual_use = false;
    private String login_id = "";
    private String login_password = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrance);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        final SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_key), Context.MODE_PRIVATE);
        if (sharedPref.getBoolean(PREF_LANDSCAPE, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        backPressCloseHandler = new BackPressCloseHandler(this);

        landscapeSwitch = findViewById(R.id.switch_landscape);
        landscapeSwitch.setChecked(sharedPref.getBoolean(PREF_LANDSCAPE, false));
        landscapeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ApplySharedPref")
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sharedPref.edit().putBoolean(PREF_LANDSCAPE, isChecked).commit();
            }
        });

        adjustmentSwitch = findViewById(R.id.switch_adjustment);
        adjustmentSwitch.setChecked(sharedPref.getBoolean(PREF_ADJUSTMENT, false));
        adjustmentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ApplySharedPref")
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sharedPref.edit().putBoolean(PREF_ADJUSTMENT, isChecked).commit();
            }
        });

        silentSwitch = findViewById(R.id.switch_silent);
        silentSwitch.setChecked(sharedPref.getBoolean(PREF_SILENT, false));
        silentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ApplySharedPref")
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sharedPref.edit().putBoolean(PREF_SILENT, isChecked).commit();
            }
        });

        final String[] listItems = getResources().getStringArray(R.array.connector_list);
        selectButton = findViewById(R.id.connector_select);
        String connector = sharedPref.getString(PREF_CONNECTOR, null);
        if (connector != null) {
            selectButton.setText(connector);
        } else {
            selectButton.setText(getString(R.string.select_server));
        }

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int connector_idx = -1;
                String connector = sharedPref.getString(PREF_CONNECTOR, null);
                for (int i = 0; i < listItems.length; i++) {
                    if (listItems[i].equals(connector)) {
                        connector_idx = i;
                        break;
                    }
                }
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(EntranceActivity.this);
                mBuilder.setTitle("Select Connector");
                mBuilder.setSingleChoiceItems(listItems, connector_idx, new DialogInterface.OnClickListener() {
                    @SuppressLint("ApplySharedPref")
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sharedPref.edit().putString(PREF_CONNECTOR, listItems[i]).commit();
                        sharedPref.edit().putString(PREF_LATEST_URL, URL_LIST[i]).apply();
                        selectButton.setText(listItems[i]);
                        Toast.makeText(getApplicationContext(), URL_LIST[i], Toast.LENGTH_LONG).show();
                        dialogInterface.dismiss();
                    }
                });
                AlertDialog mDialog = mBuilder.create();
                mDialog.show();
            }
        });

        login_id = sharedPref.getString(PREF_DMM_ID, "");
        login_password = sharedPref.getString(PREF_DMM_PASS, "");
        autoCompleteButton = findViewById(R.id.webview_autocomplete);
        autoCompleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(EntranceActivity.this);
                View dialogView = getLayoutInflater().inflate(R.layout.login_form, null);
                final CheckBox saveData = dialogView.findViewById(R.id.chkbox_account_pref);
                final EditText formEmail = dialogView.findViewById(R.id.input_id);
                final EditText formPassword = dialogView.findViewById(R.id.input_pw);
                formEmail.setText(sharedPref.getString(PREF_DMM_ID, ""));
                formPassword.setText(sharedPref.getString(PREF_DMM_PASS, ""));
                builder.setView(dialogView);
                builder.setPositiveButton(R.string.text_save, new DialogInterface.OnClickListener() {
                    @SuppressLint("ApplySharedPref")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        login_id = formEmail.getText().toString();
                        login_password = formPassword.getText().toString();
                        if (saveData.isChecked()) {
                            sharedPref.edit().putString(PREF_DMM_ID, login_id).commit();
                            sharedPref.edit().putString(PREF_DMM_PASS, login_password).commit();
                        }
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.text_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        clearButton = findViewById(R.id.webview_clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webview = new WebView(getApplicationContext());
                webview.clearCache(true);
                String cache_dir = getApplicationContext().getFilesDir().getAbsolutePath().concat("/cache/");
                clearApplicationCache(getApplicationContext(), getCacheDir());
                clearApplicationCache(getApplicationContext(), new File(cache_dir));
                Toast.makeText(getApplicationContext(), getString(R.string.cache_cleared_toast), Toast.LENGTH_LONG).show();
            }
        });

        manualControlCheckbox = findViewById(R.id.layout_control);
        manualControlCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                manual_use = isChecked;
            }
        });

        startButton = findViewById(R.id.webview_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String connector = sharedPref.getString(PREF_CONNECTOR, null);
                if (connector == null) {
                    Toast.makeText(getApplicationContext(), getString(R.string.select_server_toast), Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(EntranceActivity.this, FullscreenActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (manualControlCheckbox.isChecked()) intent.setAction(ACTION_WITHLC);
                    intent.putExtra("login_id", login_id);
                    intent.putExtra("login_pw", login_password);
                    startActivity(intent);
                    finish();
                }
            }
        });
        versionText = findViewById(R.id.version_info);
        versionText.setText(String.format(Locale.US, getString(R.string.version_format), BuildConfig.VERSION_NAME));
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

    public void clearApplicationCache(Context context, File file) {
        File dir = null;
        if (file == null) {
            dir = context.getCacheDir();
        } else {
            dir = file;
        }
        if (dir == null) return;
        File[] children = dir.listFiles();
        try {
            for (File child : children)
                if (child.isDirectory()) clearApplicationCache(context, child);
                else child.delete();
        } catch (Exception e) {
        }
    }
}
