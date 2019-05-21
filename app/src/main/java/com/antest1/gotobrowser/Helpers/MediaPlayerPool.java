package com.antest1.gotobrowser.Helpers;

import android.media.MediaPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A pool (queue) of audio tracks
 *
 * As soon as a track finishes, it is removed from the queue
 * Once all tracks finished {@link android.media.MediaPlayer.OnCompletionListener} is called
 */
public class MediaPlayerPool {
    private int streamsLimit;
    private LinkedBlockingDeque<MediaPlayer> players;
    private OnAllCompletedListener onAllCompletedListener;

    /**
     * Interface definition for a callback to be invoked when playbacks of all media sources have completed
     */
    public interface OnAllCompletedListener {
        /**
         * Called when the end of a media source is reached during playback.
         *
         * @param pool the MediaPlayerPool that manages the last track that just finished
         * @param lastPlayer the last MediaPlayer that reached the end of the file
         */
        void onAllCompleted(MediaPlayerPool pool, MediaPlayer lastPlayer);
    }

    /**
     * @param maxStreams limits how many streams are allowed to ran, oldest removed when it would overflow
     */
    public MediaPlayerPool(int maxStreams) {
        streamsLimit = maxStreams;
        players = new LinkedBlockingDeque<>();
    }

    /**
     * Stops and releases all managed MediaPlayers and clears the queue
     */
    public void release() {
        for (MediaPlayer player: players) {
            player.stop();
            player.release();
        }

        players.clear();
    }

    public void setStreamsLimit(int limit) {
        streamsLimit = limit;
    }

    public void setOnAllCompletedListener(OnAllCompletedListener onAllCompletitionListener) {
        this.onAllCompletedListener = onAllCompletitionListener;
    }

    public void startAll() {
        List<MediaPlayer> remove_target = new ArrayList<>();
        for (MediaPlayer player: players) {
            try {
                player.start();
            } catch (IllegalStateException e) {
                remove_target.add(player);
            }
        }
        for (MediaPlayer player: remove_target) {
            players.remove(player);
        }
    }

    public boolean isAnyPlaying() {
        for (MediaPlayer player: players) {
            try {
                if (player.isPlaying()) {
                    return true;
                }
            } catch (IllegalStateException e) {
                continue;
            }
        }

        return false;
    }

    public void pauseAll() {
        for (MediaPlayer player: players) {
            player.pause();
        }
    }

    public void stopAll() {
        for (MediaPlayer player: players) {
            player.stop();
        }
    }

    public void resetAll() {
        for (MediaPlayer player: players) {
            player.reset();
        }
    }

    public void setVolumeAll(float volumeLeft, float volumeRight) {
        for (MediaPlayer player: players) {
            player.setVolume(volumeLeft, volumeRight);
        }
    }

    public MediaPlayer getOldest() {
        return players.peekFirst();
    }

    public void addToPool(MediaPlayer player) {
        player.setOnCompletionListener(mp -> {
            players.remove(player);

            if( players.isEmpty() && onAllCompletedListener != null ) {
                onAllCompletedListener.onAllCompleted(this, player);
            }
        });

        if( players.size() >= streamsLimit ) {
            removeOldestTrack();
        }

        players.add(player);
        player.start();
    }

    private void removeOldestTrack() {
        MediaPlayer oldestPlayer = getOldest();
        try {
            oldestPlayer.stop();
            oldestPlayer.release();
        } catch (IllegalStateException e) {
            // possibly already released: do nothing
        }
        players.removeFirst();
    }
}
