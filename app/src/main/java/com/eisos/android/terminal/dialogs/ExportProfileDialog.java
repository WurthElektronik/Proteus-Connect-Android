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


import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.eisos.android.R;
import com.eisos.android.terminal.database.profiles.Profile;
import com.eisos.android.terminal.profiles.JSONBuilder;
import com.eisos.android.terminal.profiles.ProfileListFragment;
import com.eisos.android.terminal.utils.CustomLogger;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ExportProfileDialog extends DialogFragment {

    public static final String FILE_PATH = "Download/ProteusConnect/";
    private CheckBox checkBoxAll;
    private Button btnCancel, btnExport;
    private List<Profile> profileList;
    private LinearLayout profileLayout;
    private ProfileListFragment profileListFragment;
    private ArrayList<CheckBox> checkBoxes;
    private ArrayList<Profile> selectedProfiles;

    public ExportProfileDialog(List<Profile> profileList, ProfileListFragment profileListFragment) {
        this.profileList = profileList;
        this.profileListFragment = profileListFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_export_profile, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        profileLayout = view.findViewById(R.id.profile_list);
        checkBoxes = new ArrayList<>();
        selectedProfiles = new ArrayList<>();
        checkBoxAll = view.findViewById(R.id.cb_all);
        checkBoxAll.setOnClickListener((View v) -> checkAll(checkBoxAll));
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener((View v) -> dismiss());
        btnExport = view.findViewById(R.id.btn_export);
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportProfiles();
            }
        });
        generateCheckBoxes();
        return view;
    }

    private void exportProfiles() {

        JSONArray array = new JSONArray();
        array.put(JSONBuilder.buildFileIdentifier());
        for (Profile profile : selectedProfiles) {
            JSONObject obj = JSONBuilder.buildProfileEntry(profile);
            array.put(obj);
        }

        // If no profile is selected return
        if (array.length() == 1) {
            return;
        }

        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)  + File.separator + JSONBuilder.FOLDER_NAME);
        if(!storageDir.exists()) {
            storageDir.mkdirs();
        }
        File file = new File(storageDir, JSONBuilder.FILE_NAME);
        try {
            FileOutputStream fout = new FileOutputStream(file);
            fout.write(array.toString(2).getBytes());
            fout.flush();
            fout.close();
            dismiss();

            Snackbar snackbar = Snackbar.make(profileListFragment.getView(), getString(R.string.profileExportSuccess)
                    + " " + FILE_PATH, Snackbar.LENGTH_SHORT);
            snackbar.getView().setBackgroundColor(getResources().getColor(R.color.greenSuccess));
            snackbar.show();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(CustomLogger.TAG, "ERROR EXPORTING PROFILE");
            dismiss();

            Snackbar snackbar = Snackbar.make(profileListFragment.getView(), getString(R.string.profileExportError), Snackbar.LENGTH_SHORT);
            snackbar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            snackbar.show();
        }
    }

    private void generateCheckBoxes() {
        int index = 0;
        for (Profile profile : profileList) {
            CheckBox cb = new CheckBox(getContext());
            cb.setTag(index);
            cb.setText(profile.getName());
            cb.setOnClickListener((View v) -> onCheckBoxClicked(cb));
            checkBoxes.add(cb);
            profileLayout.addView(cb);
            index++;
        }
    }

    private void checkAll(CheckBox checkBox) {
        if (checkBox.isChecked()) {
            for (int i = 0; i < checkBoxes.size(); i++) {
                checkBoxes.get(i).setChecked(true);
                selectedProfiles.add(profileList.get(i));
            }
        } else {
            for (CheckBox cb : checkBoxes) {
                cb.setChecked(false);
            }
            selectedProfiles.clear();
        }
    }

    private void onCheckBoxClicked(CheckBox checkBox) {
        Profile profile = profileList.get((int) checkBox.getTag());
        if (checkBox.isChecked()) {
            selectedProfiles.add(profile);
        } else {
            selectedProfiles.remove(profile);
        }
    }
}
