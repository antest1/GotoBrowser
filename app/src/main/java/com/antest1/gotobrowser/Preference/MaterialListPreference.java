package com.antest1.gotobrowser.Preference;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import androidx.preference.DialogPreference;
import androidx.preference.ListPreferenceDialogFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jspecify.annotations.NonNull;

public class MaterialListPreference extends ListPreferenceDialogFragmentCompat {

    private int whichButtonClicked = DialogInterface.BUTTON_NEGATIVE;
    private boolean onDialogClosedWasCalledFromOnDismiss = false;

    @androidx.annotation.NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireActivity();
        DialogPreference preference = getPreference();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(preference.getDialogTitle())
                .setIcon(preference.getDialogIcon())
                .setPositiveButton(preference.getPositiveButtonText(), this)
                .setNegativeButton(preference.getNegativeButtonText(), this);

        View contentView = onCreateDialogView(context);
        if (contentView != null) {
            onBindDialogView(contentView);
            builder.setView(contentView);
        } else {
            builder.setMessage(preference.getDialogMessage());
        }

        onPrepareDialogBuilder(builder);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        whichButtonClicked = which;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        onDialogClosedWasCalledFromOnDismiss = true;
        super.onDismiss(dialog);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (onDialogClosedWasCalledFromOnDismiss) {
            onDialogClosedWasCalledFromOnDismiss = false;
            super.onDialogClosed(whichButtonClicked == DialogInterface.BUTTON_POSITIVE);
        } else {
            super.onDialogClosed(positiveResult);
        }
    }
}
