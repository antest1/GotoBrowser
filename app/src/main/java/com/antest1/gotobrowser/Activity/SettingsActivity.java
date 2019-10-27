package com.antest1.gotobrowser.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.antest1.gotobrowser.Browser.WebViewManager;
import com.antest1.gotobrowser.BuildConfig;
import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.VersionDatabase;
import com.antest1.gotobrowser.R;
import com.antest1.gotobrowser.Subtitle.SubtitleCheck;
import com.antest1.gotobrowser.Subtitle.SubtitleLocaleAdapter;
import com.antest1.gotobrowser.Subtitle.SubtitleRepo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.antest1.gotobrowser.Constants.GITHUBAPI_ROOT;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.SUBTITLE_PATH;
import static com.antest1.gotobrowser.Constants.SUBTITLE_ROOT;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;
import static com.antest1.gotobrowser.Helpers.KcUtils.getRetrofitAdapter;

public class SettingsActivity extends AppCompatActivity {
    private VersionDatabase versionTable;
    private TextView subtitleLoading, subtitleLang;
    public RecyclerView subtitleList;
    public SharedPreferences sharedPref;
    private SubtitleLocaleAdapter adapter;
    private SubtitleCheck updateCheck;
    private SubtitleRepo subtitleRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        sharedPref = getSharedPreferences(
                getString(R.string.preference_key), Context.MODE_PRIVATE);

        versionTable = new VersionDatabase(getApplicationContext(), null, VERSION_TABLE_VERSION);
        updateCheck = getRetrofitAdapter(getApplicationContext(), GITHUBAPI_ROOT).create(SubtitleCheck.class);
        subtitleRepo = getRetrofitAdapter(getApplicationContext(), SUBTITLE_ROOT).create(SubtitleRepo.class);

        TextView versionText = findViewById(R.id.version_text);
        versionText.setText(BuildConfig.VERSION_NAME);

        TextView latestCheck = findViewById(R.id.version_update);
        latestCheck.setOnClickListener(v -> requestLatestAppVersion());

        //TextView resourceDown = findViewById(R.id.resource_update);
        //resourceDown.setOnClickListener(v -> openResourceDownloadPage());

        ImageView exitButton = findViewById(R.id.button_exit);
        exitButton.setOnClickListener(v -> finish());

        TextView licenseButton = findViewById(R.id.license_button);
        licenseButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.license_link)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        TextView githubButton = findViewById(R.id.github_button);
        githubButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        subtitleLoading = findViewById(R.id.subtitle_loading);
        subtitleLoading.setVisibility(View.VISIBLE);

        String current_locale = sharedPref.getString(PREF_SUBTITLE_LOCALE, "");
        subtitleLang = findViewById(R.id.subtitle_current_lang);

        if (current_locale == null || current_locale.length() == 0) {
            subtitleLang.setText(String.format(Locale.US,
                    getString(R.string.settings_subtitle_language), "(none)"));
            subtitleLang.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.panel_red));
        } else {
            subtitleLang.setText(String.format(Locale.US,
                    getString(R.string.settings_subtitle_language), current_locale));
            subtitleLang.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
        }

        subtitleList = findViewById(R.id.subtitle_list);
        subtitleList.setLayoutManager(new LinearLayoutManager(this));
        subtitleList.addItemDecoration(new DividerItemDecoration(getApplicationContext(),
                DividerItemDecoration.VERTICAL));

        adapter = new SubtitleLocaleAdapter(this::onLocaleItemSelected, this::onLocaleItemDownload);
        subtitleList.setAdapter(adapter);
        addLocaleToAdapter();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void onLocaleItemSelected(JsonObject item) {
        String locale_code = item.get("locale_code").getAsString();
        subtitleLang.setText(String.format(Locale.US,
                getString(R.string.settings_subtitle_language), locale_code));
        subtitleLang.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
        sharedPref.edit().putString(PREF_SUBTITLE_LOCALE, locale_code).apply();
        adapter.notifyDataSetChanged();
        KcUtils.showToast(getApplicationContext(), locale_code);
    }

    private void onLocaleItemDownload(JsonObject item) {
        String commit = item.get("latest_commit").getAsString();
        String path = item.get("download_url").getAsString();
        Call<JsonObject> call = subtitleRepo.download(commit, path);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                saveQuotesFile(item, response);
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                KcUtils.showToast(getApplicationContext(), t.getLocalizedMessage());
            }
        });
    }

    private void addLocaleToAdapter() {
        String[] subtitle_label = getResources().getStringArray(R.array.subtitle_list);
        for (int i = 0; i < SUBTITLE_LOCALE.length; i++) {
            String[] subtitle_info = {SUBTITLE_LOCALE[i], subtitle_label[i], SUBTITLE_PATH[i]};
            Call<JsonArray> call = updateCheck.check(SUBTITLE_PATH[i]);
            call.enqueue(new Callback<JsonArray>() {
                @Override
                public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                    processLatestCommitLog(response, subtitle_info);
                }
                @Override
                public void onFailure(Call<JsonArray> call, Throwable t) {
                    KcUtils.showToast(getApplicationContext(), t.getLocalizedMessage());
                }
            });
        }
    }

    private void processLatestCommitLog(Response<JsonArray> response, String[] subtitle_info) {
        JsonArray commit_log = response.body();
        if (commit_log != null && !commit_log.isJsonNull()) {
            Log.e("GOTO", response.headers().toString());
            Log.e("GOTO", commit_log.toString());

            adapter.addLocaleData(generateLocaleItem(commit_log, subtitle_info));
            adapter.notifyDataSetChanged();
            if (adapter.getCount() > 0) {
                subtitleLoading.setVisibility(View.GONE);
            } else {
                subtitleLoading.setVisibility(View.VISIBLE);
            }
        } else {
            KcUtils.showToast(getApplicationContext(), "communication error.");
        }
    }

    private JsonObject generateLocaleItem(JsonArray commitLog, String[] subtitle_info) {
        JsonObject item = new JsonObject();
        String code = subtitle_info[0];
        String label = subtitle_info[1];
        String path = subtitle_info[2];

        String filename = String.format(Locale.US, "quotes_%s.json", code);
        String latest = commitLog.get(0).getAsJsonObject().get("sha").getAsString();
        String current_locale = sharedPref.getString(PREF_SUBTITLE_LOCALE, "");

        item.addProperty("locale_code", code);
        item.addProperty("selected", code.equals(current_locale));
        item.addProperty("locale_label", String.format(Locale.US, "%s (%s)", label, code));
        item.addProperty("current_commit", versionTable.getValue(filename));
        item.addProperty("latest_commit", latest);
        item.addProperty("download_url", path);
        return item;
    }

    public void requestLatestAppVersion() {
        Call<JsonObject> call = updateCheck.version();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.e("GOTO", response.headers().toString());
                if (response.code() == 200) {
                    checkAppUpdate(response);
                } else {
                    String message = "HTTP: " + response.code();
                    if (response.code() == 404) message = "No update found.";
                    KcUtils.showToast(getApplicationContext(), message);
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                KcUtils.showToast(getApplicationContext(), t.getLocalizedMessage());
            }
        });
    }

    private void checkAppUpdate(Response<JsonObject> response) {
        JsonObject version_info = response.body();
        if (version_info != null && version_info.has("tag_name")) {
            Log.e("GOTO", version_info.toString());
            String tag = version_info.get("tag_name").getAsString().substring(1);
            String latest_file = version_info.getAsJsonArray("assets")
                    .get(0).getAsJsonObject().get("browser_download_url").getAsString();
            if (BuildConfig.VERSION_NAME.equals(tag)) {
                KcUtils.showToast(getApplicationContext(), R.string.setting_latest_version);
            } else {
                showAppUpdateDownloadDialog(tag, latest_file);
            }
        }
    }

    private void showAppUpdateDownloadDialog(String tag, String latest_file) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                SettingsActivity.this);
        alertDialogBuilder.setTitle(getString(R.string.app_name));
        alertDialogBuilder
                .setCancelable(false)
                .setMessage(String.format(Locale.US, getString(R.string.setting_latest_download), tag))
                .setPositiveButton(R.string.action_ok,
                        (dialog, id) -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(latest_file));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        })
                .setNegativeButton(R.string.action_cancel,
                        (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void saveQuotesFile(JsonObject item, Response<JsonObject> response) {
        String message = "";
        String locale_code = item.get("locale_code").getAsString();
        String commit = item.get("latest_commit").getAsString();
        String filename = String.format(Locale.US, "quotes_%s.json", locale_code);

        JsonObject data = response.body();
        String subtitle_folder = getApplicationContext().getFilesDir().getAbsolutePath().concat("/subtitle/");
        String subtitle_path = subtitle_folder.concat(filename);
        File file = new File(subtitle_folder);
        try {
            if (!file.exists()) file.mkdirs();
            if (data != null) {
                File subtitle_file = new File(subtitle_path);
                FileOutputStream fos = new FileOutputStream(subtitle_file);
                fos.write(data.toString().getBytes());
                fos.close();
                versionTable.putValue(filename, commit);
                adapter.itemCommitUpdate(locale_code);
                adapter.notifyDataSetChanged();
                message = "Saved: quotes_".concat(locale_code).concat(".json");
            } else {
                message = "No data to write: quotes_".concat(locale_code).concat(".json");
            }
        } catch (IOException e) {
            KcUtils.reportException(e);
            message = "IOException while saving quotes_".concat(locale_code).concat(".json");
        } finally {
            KcUtils.showToast(getApplicationContext(), message);
        }
    }

    /*
    public void openResourceDownloadPage() {
        Intent intent = new Intent(SettingsActivity.this, BrowserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(WebViewManager.OPEN_RES_DOWN);
        startActivity(intent);
    }*/
}
