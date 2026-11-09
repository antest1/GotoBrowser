package com.antest1.gotobrowser.Activity

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Follows the first-screen load so the overlay can tell "still loading" from "stuck".
 *
 * Fed by WebView callbacks (main thread) and shouldInterceptRequest (background threads);
 * the overlay polls [snapshot]. Loading ends when the game sends its first kcsapi request.
 * Pages other than the game page (e.g. the login form) are [Stage.IDLE]: they are not
 * black, so nothing is shown over them.
 */
class LoadingTracker {
    enum class Stage { IDLE, PREPARING, CONNECTING, DOWNLOADING, READY }

    sealed interface Failure {
        /** What failed, e.g. "net::ERR_NAME_NOT_RESOLVED" or "HTTP 403". */
        val code: String
        val url: String?
        val host: String? get() = url?.let { android.net.Uri.parse(it).host }

        data class Network(val errorCode: Int, override val code: String, override val url: String?) : Failure
        data class Http(val statusCode: Int, override val code: String, override val url: String?) : Failure
    }

    data class Snapshot(
        val stage: Stage = Stage.IDLE,
        /** Main-frame progress (0-100), meaningful while [Stage.CONNECTING]. */
        val progress: Int = 0,
        /** Requests seen since the load started, shown while [Stage.DOWNLOADING]. */
        val requestCount: Int = 0,
        /** Seconds without any activity once that exceeds [STALL_MILLIS], otherwise 0. */
        val stalledSeconds: Int = 0,
        val failure: Failure? = null
    ) {
        val isLoading: Boolean get() = stage.isLoading()
    }

    @Volatile private var stage = Stage.IDLE
    @Volatile private var progress = 0
    @Volatile private var failure: Failure? = null
    private val requestCount = AtomicInteger()
    private val lastActivity = AtomicLong(SystemClock.elapsedRealtime())

    /** A (re)load was requested; settings and proxy are applied before the page is opened. */
    @Synchronized
    fun start() = reset(Stage.PREPARING)

    /** The game frame is reloaded in place (quick refresh): no main-frame navigation follows. */
    @Synchronized
    fun onGameFrameReload() = reset(Stage.CONNECTING)

    /** @param isAccountPage a login / logout / region page: shown as is, never covered */
    @Synchronized
    fun onPageStarted(url: String?, isAccountPage: Boolean) {
        if (isBlank(url)) return
        // Chromium may report the failing URL again for its error page; keep that failure.
        if (failure != null && failure?.url == url) return
        if (isAccountPage) {
            reset(Stage.IDLE)
        } else {
            reset(Stage.CONNECTING)
        }
    }

    /**
     * The main frame sent a request for [url]: a form was submitted or a link followed.
     * [onPageStarted] only runs once the server has answered and the new page commits, so
     * without this a slow answer (e.g. a connector logging in to DMM) shows nothing at all.
     * A load already under way is left alone, so a redirect keeps its counts and a failure
     * stays shown. Whatever page arrives then sets the stage as usual.
     */
    @Synchronized
    fun onMainFrameRequest(url: String?) {
        if (isBlank(url) || stage.isLoading()) return
        reset(Stage.CONNECTING)
    }

    /**
     * The main frame committed to [url] (also after a redirect) or finished loading it.
     * Anything but the game page is a normal web page, so loading ends there.
     */
    @Synchronized
    fun onPageShown(url: String?, isGamePage: Boolean) {
        if (isBlank(url) || isGamePage || failure != null) return
        if (stage.isLoading()) stage = Stage.IDLE
    }

    fun onProgressChanged(newProgress: Int) {
        progress = newProgress
        touch()
    }

    fun onRequest(url: String) {
        requestCount.incrementAndGet()
        touch()
        when {
            url.contains("/kcsapi/") && !url.contains("osapi.dmm.com") -> advance(Stage.READY)
            url.contains("/kcs2/") -> advance(Stage.DOWNLOADING)
        }
    }

    /** @return true if the failure belongs to the current load and is now shown inline */
    @Synchronized
    fun onFailure(newFailure: Failure): Boolean {
        if (!stage.isLoading()) return false
        failure = newFailure
        return true
    }

    fun snapshot(): Snapshot {
        val currentStage = stage
        if (!currentStage.isLoading()) return Snapshot(stage = currentStage)
        val currentFailure = failure
        val idle = SystemClock.elapsedRealtime() - lastActivity.get()
        return Snapshot(
            stage = currentStage,
            progress = progress,
            requestCount = requestCount.get(),
            stalledSeconds = if (currentFailure == null && idle >= STALL_MILLIS) (idle / 1000).toInt() else 0,
            failure = currentFailure
        )
    }

    private fun reset(newStage: Stage) {
        stage = newStage
        progress = 0
        failure = null
        requestCount.set(0)
        touch()
    }

    @Synchronized
    private fun advance(target: Stage) {
        if (stage.isLoading() && target > stage) stage = target
    }

    private fun touch() = lastActivity.set(SystemClock.elapsedRealtime())

    private fun isBlank(url: String?) = url == null || url.startsWith("about:blank")

    companion object {
        const val STALL_MILLIS = 20_000L

        private fun Stage.isLoading() = this == Stage.PREPARING || this == Stage.CONNECTING || this == Stage.DOWNLOADING
    }
}
