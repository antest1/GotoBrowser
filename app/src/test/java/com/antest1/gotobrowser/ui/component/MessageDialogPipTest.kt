package com.antest1.gotobrowser.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageDialogPipTest {
    @Test
    fun onlyMarkedDialogsAreCancelledWithTheirDismissAction() {
        val ran = mutableListOf<String>()
        val confirmation = MessageDialog(
            "Refresh", "Refresh the game?",
            confirm = DialogButton("OK") { ran.add("refresh") },
            dismiss = DialogButton("Cancel") { ran.add("resume") },
            cancelOnPip = true
        )
        val error = MessageDialog(
            "Error", "Could not load the page",
            confirm = DialogButton("OK") { ran.add("error-ok") },
            dismiss = DialogButton("Close") { ran.add("error-close") }
        )
        val dialogs = mutableListOf(confirmation, error)

        dialogs.cancelForPip()

        assertEquals(listOf(error), dialogs)
        assertEquals(listOf("resume"), ran)
    }

    @Test
    fun aMarkedDialogWithoutDismissIsJustClosed() {
        val dialogs = mutableListOf(MessageDialog("Title", "Message", DialogButton("OK"), cancelOnPip = true))
        dialogs.cancelForPip()
        assertEquals(emptyList<MessageDialog>(), dialogs)
    }
}
