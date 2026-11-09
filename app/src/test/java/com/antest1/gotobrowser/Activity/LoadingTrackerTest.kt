package com.antest1.gotobrowser.Activity

import android.app.Application
import com.antest1.gotobrowser.Activity.LoadingTracker.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class LoadingTrackerTest {
    private val loginPage = "https://ooi.moe/"
    private val gamePage = "https://ooi.moe/kancolle"

    /** The login form is on screen: its page committed, so nothing is loading. */
    private fun onLoginForm() = LoadingTracker().apply {
        onPageStarted(loginPage, false)
        onPageShown(loginPage, false)
        assertEquals(Stage.IDLE, snapshot().stage)
    }

    @Test
    fun submittingAFormShowsConnectingBeforeTheAnswer() {
        val tracker = onLoginForm()
        tracker.onMainFrameRequest(loginPage)
        assertEquals(Stage.CONNECTING, tracker.snapshot().stage)
    }

    @Test
    fun theGamePageThenCarriesTheLoadOn() {
        val tracker = onLoginForm()
        tracker.onMainFrameRequest(loginPage)
        tracker.onMainFrameRequest(gamePage)
        tracker.onPageStarted(gamePage, false)
        tracker.onPageShown(gamePage, true)
        tracker.onRequest("https://ooi.moe/kcs2/js/main.js")
        assertEquals(Stage.DOWNLOADING, tracker.snapshot().stage)
    }

    @Test
    fun anOrdinaryAnswerEndsTheLoad() {
        val tracker = onLoginForm()
        tracker.onMainFrameRequest(loginPage)
        tracker.onPageStarted(loginPage, false)
        tracker.onPageShown(loginPage, false)
        assertEquals(Stage.IDLE, tracker.snapshot().stage)
    }

    @Test
    fun aRedirectKeepsTheLoadAndItsFailure() {
        val tracker = onLoginForm()
        tracker.onMainFrameRequest(loginPage)
        tracker.onFailure(LoadingTracker.Failure.Http(502, "HTTP 502", gamePage))
        tracker.onMainFrameRequest(gamePage)
        assertEquals(Stage.CONNECTING, tracker.snapshot().stage)
        assertNotNull(tracker.snapshot().failure)
    }

    @Test
    fun aBlankPageIsIgnored() {
        val tracker = onLoginForm()
        tracker.onMainFrameRequest("about:blank")
        assertEquals(Stage.IDLE, tracker.snapshot().stage)
    }

    @Test
    fun leavingARunningGameShowsConnecting() {
        val tracker = LoadingTracker()
        tracker.onPageStarted(gamePage, false)
        tracker.onRequest("https://ooi.moe/kcsapi/api_start2/getData")
        assertEquals(Stage.READY, tracker.snapshot().stage)
        tracker.onMainFrameRequest(loginPage)
        assertEquals(Stage.CONNECTING, tracker.snapshot().stage)
    }
}
