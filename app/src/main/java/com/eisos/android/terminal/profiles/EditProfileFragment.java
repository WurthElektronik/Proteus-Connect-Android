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

package com.eisos.android.terminal.profiles;

import android.app.Application;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eisos.android.R;
import com.eisos.android.terminal.adapter.ProfileAdapter;
import com.eisos.android.terminal.database.profiles.Profile;

public class EditProfileFragment extends Fragment {

    private Application application;
    private ProfileAdapter profileAdapter;
    private ProfileActivity profileActivity;
    private EditText etName, etDescription, etCommand;
    private Spinner spinnerEncoding;
    private Button btnEdit;
    private int position;
    private Profile profile;

    public EditProfileFragment(Application application, ProfileAdapter profileAdapter) {
        this.application = application;
        this.profileAdapter = profileAdapter;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_profile, container, false);
        profileActivity = (ProfileActivity) getActivity();
        position = getArguments().getInt("position");
        profile = profileAdapter.getProfiles().get(position);

        etName = view.findViewById(R.id.et_profile_name);
        etName.setText(profile.getName());
        etDescription = view.findViewById(R.id.et_description);
        String desc = profile.getDescription();
        if(desc.equals(getString(R.string.descriptionPlaceholder))) {
            etDescription.setHint(desc);
        } else {
            etDescription.setText(desc);
        }
        spinnerEncoding = view.findViewById(R.id.spinner_encoding);
        String encoding = profile.getEncodingFormat();
        if(encoding.equals(getString(R.string.encodingFormatAscii))) {
            spinnerEncoding.setSelection(0);
        } else {
            spinnerEncoding.setSelection(1);
        }
        etCommand = view.findViewById(R.id.et_command);
        etCommand.setText(String.valueOf(profile.getCommand()));

        btnEdit = view.findViewById(R.id.btn_create);
        btnEdit.setText(getString(R.string.editProfileBtn));
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEditClicked();
            }
        });
        return view;
    }

    private void onEditClicked() {
        if (checkInput()) {
            profile.setName(etName.getText().toString());
            String desc = etDescription.getText().toString();
            if(desc.trim().isEmpty()) {
                desc = getString(R.string.descriptionPlaceholder);
            }
            profile.setDescription(desc);
            String encoding = (String) spinnerEncoding.getSelectedItem();
            profile.setEncodingFormat(encoding);
            profile.setCommand(etCommand.getText().toString());
            profileAdapter.updateProfile(position, profile);
            profileActivity.onProfileEdited();
        }
    }

    private boolean checkInput() {
        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError(application.getString(R.string.fieldRequired));
            etName.requestFocus();
            return false;
        } else if (etCommand.getText().toString().isEmpty()) {
            etCommand.setError(application.getString(R.string.fieldRequired));
            etCommand.requestFocus();
            return false;
        }
        return true;
    }
}
