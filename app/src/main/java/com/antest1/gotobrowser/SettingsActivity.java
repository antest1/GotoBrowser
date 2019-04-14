package com.antest1.gotobrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.antest1.gotobrowser.Constants.GITHUBAPI_ROOT;
import static com.antest1.gotobrowser.Constants.PREF_LANDSCAPE;
import static com.antest1.gotobrowser.Constants.PREF_SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.SUBTITLE_LOCALE;
import static com.antest1.gotobrowser.Constants.SUBTITLE_PATH;
import static com.antest1.gotobrowser.Constants.SUBTITLE_ROOT;
import static com.antest1.gotobrowser.Constants.VERSION_TABLE_VERSION;

public class SettingsActivity extends AppCompatActivity {
    private VersionDatabase versionTable;
    private TextView versionText, latestCheck, subtitleLoading, subtitleLang;
    private TextView licenseButton, githubButton;
    public ImageView exitButton;
    public RecyclerView subtitleList;
    public SharedPreferences sharedPref;
    private SubtitleLocaleAdapter adapter;
    private ArrayList<JsonObject> subtitleItems = new ArrayList<>();
    private final static int CACHE_SIZE_BYTES = 1024 * 1024 * 2;

    private SubtitleCheck updateCheck;
    private SubtitleRepo subtitleRepo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        sharedPref = getSharedPreferences(
                getString(R.string.preference_key), Context.MODE_PRIVATE);

        updateCheck = getRetrofitAdapter(getApplicationContext(), GITHUBAPI_ROOT).create(SubtitleCheck.class);
        subtitleRepo = getRetrofitAdapter(getApplicationContext(), SUBTITLE_ROOT).create(SubtitleRepo.class);

        versionText = findViewById(R.id.version_text);
        versionText.setText(BuildConfig.VERSION_NAME);

        latestCheck = findViewById(R.id.version_update);
        latestCheck.setOnClickListener(v -> checkAppVersion());

        versionTable = new VersionDatabase(getApplicationContext(), null, VERSION_TABLE_VERSION);

        exitButton = findViewById(R.id.button_exit);
        exitButton.setOnClickListener(v -> finish());

        licenseButton = findViewById(R.id.license_button);
        licenseButton.setOnClickListener(v -> {
            Toast.makeText(getApplicationContext(), getString(R.string.license_link), Toast.LENGTH_LONG);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.license_link)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        githubButton = findViewById(R.id.github_button);
        githubButton.setOnClickListener(v -> {
            Toast.makeText(getApplicationContext(), getString(R.string.github_link), Toast.LENGTH_LONG);
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
        subtitleList.addItemDecoration(new DividerItemDecoration(getApplicationContext(),
                DividerItemDecoration.VERTICAL));
        setSubtitleList();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @SuppressLint("ApplySharedPref")
    SubtitleLocaleAdapter.OnItemClickListener selector = item -> {
        String locale_code = item.get("locale_code").getAsString();
        subtitleLang.setText(String.format(Locale.US,
                getString(R.string.settings_subtitle_language), locale_code));
        subtitleLang.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
        sharedPref.edit().putString(PREF_SUBTITLE_LOCALE, locale_code).commit();
        adapter.notifyDataSetChanged();
        Toast.makeText(getApplicationContext(), locale_code, Toast.LENGTH_LONG).show();
    };

    SubtitleLocaleAdapter.OnItemClickListener downloader = item -> {
        String locale_code = item.get("locale_code").getAsString();
        String commit = item.get("latest_commit").getAsString();
        String path = item.get("download_url").getAsString();
        String filename = String.format(Locale.US, "quotes_%s.json", locale_code);
        Call<JsonObject> call = subtitleRepo.download(commit, path);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
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
                        Toast.makeText(getApplicationContext(),
                                "Saved: quotes_".concat(locale_code).concat(".json")
                                , Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "No data to write: quotes_".concat(locale_code).concat(".json")
                                , Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),
                            "IOException while saving quotes_".concat(locale_code).concat(".json")
                            , Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    };

    private void setSubtitleList() {
        if (sharedPref == null) return;
        subtitleItems = new ArrayList<>();
        adapter = new SubtitleLocaleAdapter(subtitleItems, selector, downloader);
        subtitleList.setAdapter(adapter);
        subtitleList.setLayoutManager(new LinearLayoutManager(this));

        String[] subtitle_label = getResources().getStringArray(R.array.subtitle_list);
        for (int i = 0; i < SUBTITLE_LOCALE.length; i++) {
            final String locale_code = SUBTITLE_LOCALE[i];
            final String locale_label = subtitle_label[i];
            final String locale_path = SUBTITLE_PATH[i];
            String filename = String.format(Locale.US, "quotes_%s.json", locale_code);
            Call<JsonArray> call = updateCheck.check(locale_path);
            call.enqueue(new Callback<JsonArray>() {
                @Override
                public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                    JsonArray commitLog = response.body();
                    Log.e("GOTO", response.headers().toString());
                    Log.e("GOTO", commitLog.toString());
                    if (!commitLog.isJsonNull()) {
                        String latest = commitLog.get(0).getAsJsonObject().get("sha").getAsString();
                        // Log.e("GOTO", locale + " " + latest);

                        JsonObject item = new JsonObject();
                        item.addProperty("locale_code", locale_code);
                        item.addProperty("selected", locale_code.equals(sharedPref.getString(PREF_SUBTITLE_LOCALE, "")));
                        item.addProperty("locale_label", String.format(Locale.US, "%s (%s)", locale_label, locale_code));
                        item.addProperty("current_commit", versionTable.getValue(filename));
                        item.addProperty("latest_commit", latest);
                        item.addProperty("download_url", locale_path);
                        adapter.addLocaleData(item);
                        adapter.notifyDataSetChanged();
                        subtitleLoading.setVisibility(adapter.getCount() > 0 ? View.GONE : View.VISIBLE);
                    } else {
                        Toast.makeText(getApplicationContext(), "communication error.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<JsonArray> call, Throwable t) {
                    Toast.makeText(getApplicationContext(), t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void checkAppVersion() {
        Call<JsonObject> call = updateCheck.version();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.e("GOTO", response.headers().toString());
                if (response.code() == 404) {
                    Toast.makeText(getApplicationContext(), "No update found.", Toast.LENGTH_LONG).show();
                } else if (response.code() == 200) {
                    JsonObject version_info = response.body();
                    Log.e("GOTO", version_info.toString());
                    if (version_info.has("tag_name")) {
                        String tag = version_info.get("tag_name").getAsString().substring(1);
                        String latest_file = version_info.getAsJsonArray("assets")
                                .get(0).getAsJsonObject().get("browser_download_url").getAsString();
                        if (BuildConfig.VERSION_NAME.equals(tag)) {
                            Toast.makeText(getApplicationContext(), R.string.setting_latest_version, Toast.LENGTH_LONG).show();
                        } else {
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
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "HTTP: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static Retrofit getRetrofitAdapter(Context context, String baseUrl) {
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        builder.cache(
                new Cache(context.getCacheDir(), CACHE_SIZE_BYTES));
        OkHttpClient client = builder.build();
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder();
        retrofitBuilder
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(baseUrl)
                .client(client);
        return retrofitBuilder.build();
    }
}
