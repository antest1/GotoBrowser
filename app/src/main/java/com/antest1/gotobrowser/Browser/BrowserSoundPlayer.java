package com.antest1.gotobrowser.Browser;

import android.media.MediaPlayer;
import android.os.Handler;

import com.antest1.gotobrowser.Helpers.KcUtils;
import com.antest1.gotobrowser.Helpers.MediaPlayerPool;
import com.antest1.gotobrowser.Subtitle.KcSubtitleUtils;
import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class BrowserSoundPlayer {
    public final static String PLAYER_ALL = "all";
    public final static String PLAYER_BGM = "bgm";
    public final static String PLAYER_SE = "se";
    public final static String PLAYER_VOICE = "voice";

    public final static int AUDIO_POOL_LIMIT = 25;

    private static boolean isMuteMode = false;
    private Handler handler;
    private Map<String, MediaPlayerPool> players = new HashMap<>();
    private Map<String, Float> volumes = new HashMap<>();
    private float currentBgmVolume;


    public static boolean ismute() { return isMuteMode; }
    public static void setmute(boolean value) { isMuteMode = value; }
    public static void mute() { setmute(true); }
    public static void unmute() { setmute(false); }

    public BrowserSoundPlayer(Handler subtitle_handler) {
        players.put(PLAYER_BGM, new MediaPlayerPool(1));
        players.put(PLAYER_SE, new MediaPlayerPool(AUDIO_POOL_LIMIT));
        players.put(PLAYER_VOICE, new MediaPlayerPool(1));

        volumes.put(PLAYER_BGM, 1.0f);
        volumes.put(PLAYER_SE, 0.7f);
        volumes.put(PLAYER_VOICE, 0.8f);

        handler = subtitle_handler;
    }

    public boolean shouldAudioLoop(String url) {
        return url.contains("resources/bgm") && !url.contains("fanfare");
    }

    public void setVolume(String player_set, float value) {
        if (PLAYER_ALL.equals(player_set)) {
            for (String key: volumes.keySet()) {
                volumes.put(key, value);
                if (isMuteMode) players.get(key).setVolumeAll(0.0f, 0.0f);
                else players.get(key).setVolumeAll(value, value);
            }
        } else {
            String[] player_list = player_set.split(",");
            for (String key: player_list) {
                if (volumes.containsKey(key)) {
                    volumes.put(key, value);
                    if (isMuteMode) players.get(key).setVolumeAll(0.0f, 0.0f);
                    else players.get(key).setVolumeAll(value, value);
                }
            }
        }
    }

    public void setMuteAll(boolean is_mute) {
        setmute(is_mute);
        for (String key: volumes.keySet()) {
            float volume = is_mute ? 0.0f : volumes.get(key);
            players.get(key).setVolumeAll(volume, volume);
        }
    }

    public void setStreamsLimit(String player_set, int value) {
        try {
            if (PLAYER_ALL.equals(player_set)) {
                for (String key: players.keySet()) {
                    players.get(key).setStreamsLimit(value);
                }
            } else {
                String[] player_list = player_set.split(",");
                for (String key: player_list) {
                    if (players.containsKey(key)) players.get(key).setStreamsLimit(value);
                }
            }
        } catch (NullPointerException e) {
            KcUtils.reportException(e);
        }
    }

    private void set(MediaPlayer player, File file, float volume) {
        if (isMuteMode) volume = 0.0f;
        try {
            String path = file.getAbsolutePath();
            player.setVolume(volume, volume);
            player.setLooping(shouldAudioLoop(path));
            player.setDataSource(path);
            player.prepare();
        } catch (IOException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }
    }

    public void setNextPlay(String player_id, File file, ResourceProcess.SubtitleRunnable sr) {
        MediaPlayerPool player = players.get(player_id);
        player.setOnAllCompletedListener((pool, lastPlayer) -> {
            play(player_id, file);
            handler.post(sr);
            player.setOnAllCompletedListener(null);
        });
    }

    public void play(String player_id, File file) {
        MediaPlayerPool player = players.get(player_id);
        float volume = ismute() ? 0.0f : volumes.get(player_id);
        if (player != null) {
            MediaPlayer audio = new MediaPlayer();
            set(audio, file, volume);
            player.addToPool(audio);
            audio.start();
        }
    }

    public void pause(String player_set) {
        String[] player_list = player_set.split(",");
        for (String key: player_list) {
            if (players.containsKey(key)) {
                players.get(key).pauseAll();
            }
        }
    }

    public void stop(String player_set) {
        String[] player_list = player_set.split(",");
        for (String key: player_list) {
            if (players.containsKey(key)) {
                players.get(key).stopAll();
            }
        }
    }

    public void startAll() {
        for (String key: players.keySet()) {
            MediaPlayerPool player = players.get(key);
            if (!player.isAnyPlaying()) {
                players.get(key).startAll();
            }
        }
    }

    public void pauseAll() {
        for (String key: players.keySet()) {
            MediaPlayerPool player = players.get(key);
            if (player.isAnyPlaying()) {
                players.get(key).pauseAll();
            }
        }
    }

    public void stopAll() {
        for (String key: players.keySet()) {
            players.get(key).stopAll();
        }
    }

    public void releaseAll() {
        for (String key: players.keySet()) {
            players.get(key).release();
        }
    }

    public void stopSound(String url, boolean isOnPractice, int currentMapId, int currentBattleBgmId) {
        String[] STOP_FLAG = {
                "battle_result/battle_result_main.json",
                "battle/battle_main.json",
                "/kcs2/resources/se/217.mp3"
        };

        for (String pattern: STOP_FLAG) {
            if (url.contains(pattern)) {
                boolean fadeout_flag = true;
                boolean shutter_call = url.contains("/se/217.mp3");
                if (shutter_call) {
                    if (isOnPractice) {
                        isOnPractice = false;
                        continue;
                    }
                    JsonObject bgmData = KcSubtitleUtils.getMapBgmGraph(currentMapId);
                    if (bgmData != null) {
                        JsonArray normal_bgm = bgmData.getAsJsonArray("api_map_bgm");
                        boolean normal_diff = normal_bgm.get(0).getAsInt() != normal_bgm.get(1).getAsInt();
                        boolean normal_flag = currentBattleBgmId == normal_bgm.get(0).getAsInt() && normal_diff;

                        JsonArray boss_bgm = bgmData.getAsJsonArray("api_boss_bgm");
                        boolean boss_diff = boss_bgm.get(0).getAsInt() != boss_bgm.get(1).getAsInt();
                        boolean boss_flag = currentBattleBgmId == boss_bgm.get(0).getAsInt() && boss_diff;

                        fadeout_flag = normal_flag || boss_flag;
                    }
                }
                if (fadeout_flag) {
                    fadeBgmOut();
                    break;
                }
            }
        }

        String[] STOP_V_FLAG = {
                "api_req_map",
                "api_get_member/slot_item"
        };
        for (String pattern: STOP_V_FLAG) {
            if (url.contains(pattern)) {
                stop(PLAYER_VOICE);
            }
        }
    }


    public void fadeBgmOut() {
        if (isMuteMode) return;
        final int FADE_DURATION = 720;
        final int FADE_INTERVAL = 80;
        final float MAX_VOLUME = volumes.get(PLAYER_BGM);
        MediaPlayerPool bgm_player = players.get(PLAYER_BGM);
        int numberOfSteps = FADE_DURATION / FADE_INTERVAL;
        final float deltaVolume = MAX_VOLUME / (float) numberOfSteps;
        currentBgmVolume = MAX_VOLUME;

        final Timer timer = new Timer(true);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    if (isMuteMode) currentBgmVolume = 0.0f;
                    bgm_player.setVolumeAll(currentBgmVolume, currentBgmVolume);
                    currentBgmVolume -= deltaVolume;
                    if(currentBgmVolume < 0.0f){
                        stop(PLAYER_BGM);
                        timer.cancel();
                        timer.purge();
                        bgm_player.setVolumeAll(MAX_VOLUME, MAX_VOLUME);
                    }
                } catch (IllegalStateException e) {
                    stop(PLAYER_BGM);
                    timer.cancel();
                    timer.purge();
                    bgm_player.setVolumeAll(MAX_VOLUME, MAX_VOLUME);
                    Crashlytics.logException(e);
                }
            }
        };
        timer.schedule(timerTask, 0, FADE_INTERVAL);
    }
}
