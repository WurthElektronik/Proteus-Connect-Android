/*
 * __          ________        _  _____
 * \ \        / /  ____|      (_)/ ____|
 *  \ \  /\  / /| |__      ___ _| (___   ___  ___
 *   \ \/  \/ / |  __|    / _ \ |\___ \ / _ \/ __|
 *    \  /\  /  | |____  |  __/ |____) | (_) \__ \
 *     \/  \/   |______|  \___|_|_____/ \___/|___/
 *
 * Copyright Wuerth Elektronik eiSos 2019
 *
 */
package com.eisos.android.terminal.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.eisos.android.R;
import com.eisos.android.terminal.utils.Preferences;

/**
 * Dialog to choose a character encoding for sending data
 */
public class EncodingDialog extends DialogFragment {

    private SharedPreferences sharedPrefs;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        int checkedItem = sharedPrefs.getInt(Preferences.PREF_ENCODING, 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogCustomEncoding);
        builder.setTitle(R.string.encodingDialogTitle)
                .setSingleChoiceItems(R.array.encodingOptions, checkedItem, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = sharedPrefs.edit();
                        editor.putInt(Preferences.PREF_ENCODING, which);
                        editor.commit();
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }
}
