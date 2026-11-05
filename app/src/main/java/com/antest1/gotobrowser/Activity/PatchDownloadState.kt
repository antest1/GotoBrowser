package com.antest1.gotobrowser.Activity

/**
 * Progress of a KCCP patch download, published by the downloaders in
 * [com.antest1.gotobrowser.Helpers.KcEnUtils] and rendered by the settings sheet
 * as a Material 3 dialog under the sheet's theme.
 */
sealed interface PatchDownloadState {
    data object Idle : PatchDownloadState

    /**
     * @param message current step or detail line (file name, transfer speed, ...)
     * @param progress completed fraction in 0..1, or null while the total is unknown
     */
    data class Running(val message: String, val progress: Float?) : PatchDownloadState
}
