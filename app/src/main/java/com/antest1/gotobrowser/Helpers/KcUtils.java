package com.antest1.gotobrowser.Helpers;

import android.media.MediaPlayer;

public class KcUtils {
    public static boolean checkIsPlaying (MediaPlayer player) {
        try {
            return player.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
