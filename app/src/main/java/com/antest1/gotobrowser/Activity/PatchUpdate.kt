package com.antest1.gotobrowser.Activity

/** An update of an enabled third-party patch, found by [SettingsViewModel.checkPatchUpdates]. */
data class PatchUpdate(
    val kind: Kind,
    /** Patch name shown as the notice's title. */
    val title: String,
    val version: String
) {
    enum class Kind { KCCP, CHS }
}
