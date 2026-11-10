package com.antest1.gotobrowser.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.antest1.gotobrowser.ui.theme.SettingsTheme

/** A button of a [MessageDialog]. The dialog is closed before [onClick] runs. */
class DialogButton @JvmOverloads constructor(
    val text: String,
    val onClick: Runnable = Runnable {}
)

/**
 * A title / message / buttons dialog requested from outside the composition
 * (WebView callbacks, resource loading, update checks) and drawn by
 * [MessageDialogHost] as a Material 3 dialog in the settings sheet's theme.
 *
 * Every dialog needs an explicit answer: back press and outside taps are
 * ignored, like the non-cancelable framework dialogs these replace.
 *
 * Deliberately not a data class: two identical requests (e.g. the same load
 * error reported twice) are separate dialogs and must be removed separately.
 */
class MessageDialog @JvmOverloads constructor(
    val title: String,
    val message: String,
    val confirm: DialogButton,
    val dismiss: DialogButton? = null,
    val neutral: DialogButton? = null,
    /**
     * Answered with [dismiss] when the app enters picture-in-picture: a confirmation that
     * pauses the game while it waits would otherwise leave the game frozen in the PiP window.
     */
    val cancelOnPip: Boolean = false
)

/** Closes the dialogs marked [MessageDialog.cancelOnPip], running their dismiss action. */
fun MutableList<MessageDialog>.cancelForPip() {
    val cancelled = filter { it.cancelOnPip }
    removeAll(cancelled)
    cancelled.forEach { it.dismiss?.onClick?.run() }
}

/**
 * Draws the pending [dialogs]. Each one is removed from the list when one of its
 * buttons is pressed. Later entries open in later windows, so they stack on top.
 */
@Composable
fun MessageDialogHost(dialogs: MutableList<MessageDialog>) {
    if (dialogs.isEmpty()) return
    SettingsTheme {
        dialogs.forEach { dialog ->
            key(dialog) {
                MessageDialogContent(dialog) { button ->
                    dialogs.remove(dialog)
                    button.onClick.run()
                }
            }
        }
    }
}

@Composable
private fun MessageDialogContent(dialog: MessageDialog, onClick: (DialogButton) -> Unit) {
    val confirm = dialog.confirm
    val dismiss = dialog.dismiss
    val neutral = dialog.neutral

    @Composable
    fun ButtonOf(button: DialogButton) {
        TextButton(onClick = { onClick(button) }) { Text(button.text) }
    }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(dialog.title) },
        // Messages may carry a long URL or path; keep the buttons reachable.
        text = { Text(dialog.message, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = {
            if (neutral == null) {
                ButtonOf(confirm)
            } else {
                // Three actions with sentence-length labels do not fit on one row,
                // so stack them end-aligned, the primary action first.
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ButtonOf(confirm)
                    dismiss?.let { ButtonOf(it) }
                    ButtonOf(neutral)
                }
            }
        },
        dismissButton = if (neutral == null && dismiss != null) {
            { ButtonOf(dismiss) }
        } else {
            null
        }
    )
}

@Preview(name = "Message dialog - two buttons")
@Composable
private fun MessageDialogPreview() {
    MessageDialogHost(
        mutableListOf(
            MessageDialog(
                title = "Full Refresh",
                message = "Reload the whole page?",
                confirm = DialogButton("Confirm"),
                dismiss = DialogButton("Cancel")
            )
        )
    )
}

@Preview(name = "Message dialog - stacked buttons")
@Composable
private fun MessageDialogStackedPreview() {
    MessageDialogHost(
        mutableListOf(
            MessageDialog(
                title = "Failed to download a game asset file",
                message = "A network issue occurred while the game client was trying to download a resource file.\n" +
                        "Do you want to retry the download?\n/kcs2/img/common/common_main.png",
                confirm = DialogButton("Retry the download"),
                dismiss = DialogButton("Do nothing and never ask again"),
                neutral = DialogButton("Do nothing")
            )
        )
    )
}
