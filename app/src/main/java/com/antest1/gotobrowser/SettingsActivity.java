package com.antest1.gotobrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
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

public class SettingsActivity extends AppCompatActivity {
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
        if (sharedPref.getBoolean(PREF_LANDSCAPE, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        }

        exitButton = findViewById(R.id.button_exit);
        exitButton.setOnClickListener(v -> finish());

        updateCheck = getRetrofitAdapter(getApplicationContext(), GITHUBAPI_ROOT).create(SubtitleCheck.class);
        subtitleRepo = getRetrofitAdapter(getApplicationContext(), SUBTITLE_ROOT).create(SubtitleRepo.class);

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
        sharedPref.edit().putString(PREF_SUBTITLE_LOCALE, locale_code).commit();
        adapter.notifyDataSetChanged();
        Toast.makeText(getApplicationContext(), locale_code, Toast.LENGTH_LONG).show();
    };

    SubtitleLocaleAdapter.OnItemClickListener downloader = item -> {
        String commit = item.get("latest_commit").getAsString();
        String path = item.get("download_url").getAsString();
        Toast.makeText(getApplicationContext(), path, Toast.LENGTH_LONG).show();
        Call<JsonObject> call = subtitleRepo.download(commit, path);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                JsonObject data = response.body();
                Toast.makeText(getApplicationContext(), data.toString().substring(0, 100), Toast.LENGTH_LONG).show();
                adapter.notifyDataSetChanged();
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
            Call<JsonArray> call = updateCheck.check(locale_path);
            call.enqueue(new Callback<JsonArray>() {
                @Override
                public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                    JsonArray commitLog = response.body();
                    Log.e("GOTO", commitLog.toString());
                    String latest = commitLog.get(0).getAsJsonObject().get("sha").getAsString();
                    // Log.e("GOTO", locale + " " + latest);

                    JsonObject item = new JsonObject();
                    item.addProperty("locale_code", locale_code);
                    item.addProperty("selected", locale_code.equals(sharedPref.getString(PREF_SUBTITLE_LOCALE, "")));
                    item.addProperty("locale_label", locale_label);
                    item.addProperty("locale_info", String.format(Locale.US, "current: %s / latest: %s", "", latest.substring(0, 7)));
                    item.addProperty("latest_commit", latest);
                    item.addProperty("download_url", locale_path);
                    adapter.addLocaleData(item);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onFailure(Call<JsonArray> call, Throwable t) {
                    Toast.makeText(getApplicationContext(), t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

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
