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
import android.content.res.AssetManager;
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

public class CreateProfileFragment extends Fragment {

    private EditText etName, etDescription, etCommand;
    private Spinner spinnerEncoding;
    private Button btnCreate;
    private ProfileAdapter profileAdapter;
    private Application application;
    private ProfileActivity profileActivity;
    private AssetManager assetManager;

    public CreateProfileFragment(Application application, ProfileAdapter profileAdapter) {
        this.application = application;
        this.profileAdapter = profileAdapter;
        assetManager = application.getResources().getAssets();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_profile, container, false);
        profileActivity = (ProfileActivity) getActivity();
        etName = view.findViewById(R.id.et_profile_name);
        etDescription = view.findViewById(R.id.et_description);
        spinnerEncoding = view.findViewById(R.id.spinner_encoding);
        etCommand = view.findViewById(R.id.et_command);

        btnCreate = view.findViewById(R.id.btn_create);
        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateClicked();
            }
        });

        Bundle bundle = getArguments();
        if(bundle != null) {
            etCommand.setText(String.valueOf(bundle.getInt(ProfileActivity.EXTRA_PROFILE_COMMAND)));
        }
        return view;
    }

    private void onCreateClicked() {
        if (checkInput()) {
            int position = profileAdapter.getItemCount()+1;
            Profile profile = new Profile(etName.getText().toString(), position, spinnerEncoding.getSelectedItem().toString());
            String desc = etDescription.getText().toString();
            if (desc.trim().isEmpty()) {
                desc = getString(R.string.descriptionPlaceholder);
            }
            profile.setDescription(desc);

            String command = etCommand.getText().toString();
            profile.setCommand(command);
            profileAdapter.createProfile(profile);
            profileActivity.onProfileCreated();
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