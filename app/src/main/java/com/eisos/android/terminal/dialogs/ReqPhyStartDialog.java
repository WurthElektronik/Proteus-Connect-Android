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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.eisos.android.R;
import com.eisos.android.terminal.customLayout.ScanListItem;
import com.eisos.android.terminal.frags.ScanFragment;

import no.nordicsemi.android.ble.PhyRequest;

public class ReqPhyStartDialog extends DialogFragment implements View.OnClickListener {
    private Button btnOk, btnCancel;
    private CheckBox cbLe1M, cbLe2M, cbCoded;
    private CheckBox previousCheckedItem;
    private RadioGroup radioGroup;
    private RadioButton rdBtnS2, rdBtnS8, rdBtnNoPref;
    private int preferredOption;
    public final static String EXTRA_DEVICE = "com.eisos.android.terminal.dialogs.DEVICE";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_phy, null);
        int preferredPhy = PhyRequest.PHY_LE_1M_MASK;
        preferredOption = PhyRequest.PHY_OPTION_NO_PREFERRED;
        btnOk = dialogView.findViewById(R.id.btn_ok);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent data = new Intent();
                ScanListItem item = (ScanListItem) getArguments().getSerializable(EXTRA_DEVICE);
                if(cbLe1M.isChecked()) {
                    data.putExtra(ScanFragment.EXTRA_PHY, PhyRequest.PHY_LE_1M_MASK);
                    data.putExtra(ScanFragment.EXTRA_PHY_OPTIONS, 0);
                } else if(cbLe2M.isChecked()) {
                    data.putExtra(ScanFragment.EXTRA_PHY, PhyRequest.PHY_LE_2M_MASK);
                    data.putExtra(ScanFragment.EXTRA_PHY_OPTIONS, 0);
                } else if (cbCoded.isChecked()) {
                    data.putExtra(ScanFragment.EXTRA_PHY, PhyRequest.PHY_LE_CODED_MASK);
                    data.putExtra(ScanFragment.EXTRA_PHY_OPTIONS, preferredOption);
                }
                // ScanListItem
                data.putExtra(EXTRA_DEVICE, item);

                // Pass ScanListItem to ScanFragment
                getTargetFragment().onActivityResult(ScanFragment.REQ_PHY, Activity.RESULT_OK, data);
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
        cbLe1M = dialogView.findViewById(R.id.cb_Le1M);
        cbLe1M.setOnClickListener(this);
        cbLe2M = dialogView.findViewById(R.id.cb_Le2M);
        cbLe2M.setOnClickListener(this);
        cbCoded = dialogView.findViewById(R.id.cb_coded);
        cbCoded.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioGroup.setVisibility(View.VISIBLE);
                if (previousCheckedItem != null) {
                    previousCheckedItem.setChecked(false);
                }

                previousCheckedItem = (CheckBox) v;
                previousCheckedItem.setChecked(true);
            }
        });
        radioGroup = dialogView.findViewById(R.id.radio_group);
        rdBtnS2 = dialogView.findViewById(R.id.rdBtn_s2);
        rdBtnS2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rdBtnS2.setChecked(true);
                preferredOption = PhyRequest.PHY_OPTION_S2;
            }
        });
        rdBtnS8 = dialogView.findViewById(R.id.rdBtn_s8);
        rdBtnS8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rdBtnS8.setChecked(true);
                preferredOption = PhyRequest.PHY_OPTION_S8;
            }
        });
        rdBtnNoPref = dialogView.findViewById(R.id.rdBtn_no_pref);
        rdBtnNoPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rdBtnNoPref.setChecked(true);
                preferredOption = PhyRequest.PHY_OPTION_NO_PREFERRED;
            }
        });

        // Set phy option
        if(preferredOption == PhyRequest.PHY_OPTION_S2) {
            rdBtnS2.setChecked(true);
        } else if(preferredOption == PhyRequest.PHY_OPTION_S8) {
            rdBtnS8.setChecked(true);
        } else if(preferredOption == PhyRequest.PHY_OPTION_NO_PREFERRED) {
            rdBtnNoPref.setChecked(true);
        }

        // Set phy mode
        if(preferredPhy == PhyRequest.PHY_LE_1M_MASK) {
            cbLe1M.setChecked(true);
            previousCheckedItem = cbLe1M;
            radioGroup.setVisibility(View.GONE);
        } else if(preferredPhy == PhyRequest.PHY_LE_2M_MASK) {
            cbLe2M.setChecked(true);
            previousCheckedItem = cbLe2M;
            radioGroup.setVisibility(View.GONE);
        } else if (preferredPhy == PhyRequest.PHY_LE_CODED_MASK) {
            cbCoded.setChecked(true);
            previousCheckedItem = cbCoded;
            radioGroup.setVisibility(View.VISIBLE);
        }
        final AlertDialog dialog = builder.setView(dialogView).create();
        return dialog;
    }

    @Override
    public void onClick(View v) {
        radioGroup.setVisibility(View.GONE);
        if (previousCheckedItem != null) {
            previousCheckedItem.setChecked(false);
        }

        previousCheckedItem = (CheckBox) v;
        previousCheckedItem.setChecked(true);
    }
}
