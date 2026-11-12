package com.antest1.gotobrowser.Activity

/**
 * Progress of a patch download, published by the downloaders in
 * [com.antest1.gotobrowser.Helpers.KcEnUtils] or [com.antest1.gotobrowser.Helpers.chs.ChsDownloader]
 * and rendered by the settings sheet as a Material 3 dialog under the sheet's theme.
 */
sealed interface PatchDownloadState {
    data object Idle : PatchDownloadState

    /**
     * @param message current step or detail line (file name, transfer speed, ...)
     * @param progress completed fraction in 0..1, or null while the total is unknown
     * @param title optional custom dialog title. When null, defaults to KCCP title for backwards compatibility.
     * @param onCancel optional cancellation callback. When non-null, the dialog renders a Cancel action and handles dismiss.
     */
    data class Running @JvmOverloads constructor(
        val message: String,
        val progress: Float?,
        val title: String? = null,
        val onCancel: (() -> Unit)? = null
    ) : PatchDownloadState
}

