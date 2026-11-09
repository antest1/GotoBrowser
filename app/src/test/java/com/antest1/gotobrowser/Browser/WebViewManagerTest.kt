package com.antest1.gotobrowser.Browser

import com.antest1.gotobrowser.Constants.CONN_DMM
import com.antest1.gotobrowser.Constants.CONN_KANMOE
import com.antest1.gotobrowser.Constants.CONN_OOI
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewManagerTest {
    @Test
    fun ooiConnectorsPostTheStoredCredentials() {
        assertTrue(WebViewManager.postsLogin(CONN_OOI, false))
        assertTrue(WebViewManager.postsLogin(CONN_KANMOE, false))
    }

    @Test
    fun dmmOpensItsStartPageWithoutPosting() {
        assertFalse(WebViewManager.postsLogin(CONN_DMM, false))
    }

    @Test
    fun afterLogoutTheLoginFormIsShownInstead() {
        assertFalse(WebViewManager.postsLogin(CONN_OOI, true))
        assertFalse(WebViewManager.postsLogin(CONN_KANMOE, true))
        assertFalse(WebViewManager.postsLogin(CONN_DMM, true))
    }
}
