package com.antest1.gotobrowser.Activity

import android.content.Context

/**
 * Seam between the settings UI (Compose) and the async helpers that used to
 * reach into [androidx.preference.PreferenceFragmentCompat] / [androidx.preference.Preference]
 * objects to update summaries and enabled state (patch downloads, subtitle updates, ...).
 *
 * Implementations live on the settings screen and translate these calls into
 * Compose state updates.
 */
interface SettingsStatusHost {
    fun getContext(): Context?

    fun requireContext(): Context?

    fun getString(resId: Int): String

    /** Update the "Download Patch Data" row while the KCCP patch version is checked / downloaded. */
    fun setPatchUpdateStatus(summary: String, enabled: Boolean)

    /** Update the "Download Subtitle Data" row while subtitle data is checked / downloaded. */
    fun setSubtitleUpdateStatus(summary: String, enabled: Boolean)

    /** Point the KCCP "About" row at the currently selected patch repository. */
    fun setPatchGithubInfo(titleResId: Int, url: String)
}
