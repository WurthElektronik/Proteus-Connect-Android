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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.eisos.android.R;
import com.eisos.android.terminal.frags.DeviceInstanceFragment;

/**
 * Dialog for requesting a new MTU
 */
public class ReqMtuDialog extends DialogFragment {

    private EditText mtuField;
    private Button btnRequest, btnCancel;
    private ImageButton btnDelete;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.dialogMtuTitle));
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_request_mtu, null);
        mtuField = dialogView.findViewById(R.id.et_mtu);
        btnDelete = dialogView.findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mtuField.getText().clear();
            }
        });
        btnRequest = dialogView.findViewById(R.id.btn_request);
        btnRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean check = checkInput(mtuField.getText().toString());
                if(!check) {
                    return;
                }
                Intent data = new Intent();
                data.putExtra(DeviceInstanceFragment.EXTRA_MTU, Integer.valueOf(mtuField.getText().toString()));
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
                dismiss();
            }
        });
        btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        final AlertDialog dialog = builder.setView(dialogView).create();
        return dialog;
    }

    public boolean checkInput(String msg) {
        if(TextUtils.isEmpty(msg.trim())) {
            this.mtuField.setError(getString(R.string.dialogMtuFieldEmpty));
            return false;
        } else if(Integer.valueOf(msg) < 23 ||Integer.valueOf(msg) > 517) {
            this.mtuField.setError(getString(R.string.dialogMtuWrongValue));
            return false;
        }
        return true;
    }
}
