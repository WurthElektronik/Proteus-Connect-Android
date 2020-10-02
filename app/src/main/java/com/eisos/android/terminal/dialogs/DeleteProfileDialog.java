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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.eisos.android.R;
import com.eisos.android.terminal.adapter.ProfileAdapter;
import com.eisos.android.terminal.database.profiles.Profile;
import com.eisos.android.terminal.profiles.ProfileActivity;

public class DeleteProfileDialog extends DialogFragment {

    private ProfileAdapter adapter;
    private int position;
    private Profile profile;
    private ProfileActivity profileActivity;

    public DeleteProfileDialog(ProfileAdapter adapter, int position, Profile profile,
                               ProfileActivity profileActivity) {
        this.adapter = adapter;
        this.position = position;
        this.profile = profile;
        this.profileActivity = profileActivity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.deleteProfileDialogTitle));
        builder.setMessage(getResources().getString(R.string.deleteProfileDialogMsg1) +
                " \"" + profile.getName() + "\"" + getResources().getString(R.string.deleteProfileDialogMsg2));
        builder.setPositiveButton(R.string.deleteProfileDialogBtn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                adapter.deleteProfile(position, profile);

                if(profileActivity.getProfileListFragment().canListScroll()) {
                    profileActivity.setFabVisibility(true);
                }

                if(adapter.getItemCount() < 1) {
                    profileActivity.getProfileListFragment().checkAdapterList();
                }
                dismiss();
            }
        });
        builder.setNegativeButton(R.string.dialogBtnCancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        Drawable drawable = getResources().getDrawable(R.drawable.ic_alert);
        drawable.setTint(getResources().getColor(R.color.colorPrimary));
        builder.setIcon(drawable);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface di) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                        getResources().getColor(R.color.colorPrimary));
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                        getResources().getColor(R.color.colorPrimary));
            }
        });
        return dialog;
    }
}
