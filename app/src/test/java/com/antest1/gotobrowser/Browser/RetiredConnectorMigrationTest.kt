package com.antest1.gotobrowser.Browser

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.antest1.gotobrowser.Constants.CONN_DMM
import com.antest1.gotobrowser.Constants.CONN_OOI
import com.antest1.gotobrowser.Constants.PREF_CONNECTOR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RetiredConnectorMigrationTest {
    private fun pref(): SharedPreferences =
        ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("test", Context.MODE_PRIVATE)

    @Test
    fun kancolleMoeMovesToOoi() {
        val pref = pref()
        pref.edit().putString(PREF_CONNECTOR, "kancolle.moe").commit()
        WebViewManager.migrateRetiredConnector(pref)
        assertEquals(CONN_OOI, pref.getString(PREF_CONNECTOR, ""))
    }

    @Test
    fun otherConnectorsAreLeftAlone() {
        val pref = pref()
        pref.edit().putString(PREF_CONNECTOR, CONN_DMM).commit()
        WebViewManager.migrateRetiredConnector(pref)
        assertEquals(CONN_DMM, pref.getString(PREF_CONNECTOR, ""))
    }

    @Test
    fun anUnsetConnectorStaysUnset() {
        val pref = pref()
        WebViewManager.migrateRetiredConnector(pref)
        assertFalse(pref.contains(PREF_CONNECTOR))
    }
}
