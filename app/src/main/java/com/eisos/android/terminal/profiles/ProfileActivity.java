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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.eisos.android.R;
import com.eisos.android.terminal.adapter.ProfileAdapter;
import com.eisos.android.terminal.database.profiles.Profile;
import com.eisos.android.terminal.dialogs.ExportProfileDialog;
import com.eisos.android.terminal.dialogs.ImportFileErrorDialog;
import com.eisos.android.terminal.frags.DeviceInstanceFragment;
import com.eisos.android.terminal.frags.ScanFragment;
import com.eisos.android.terminal.frags.TerminalFragment;
import com.eisos.android.terminal.utils.CustomLogger;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {


    public static final String ACTION_ADD_PROFILE = "com.eisos.android.terminal.profiles.ACTION_ADD_PROFILE";
    public static final String EXTRA_PROFILE_COMMAND = "com.eisos.android.terminal.profiles.EXTRA_PROFILE_COMMAND";
    public static final String EXTRA_PROFILE_ENCODING = "com.eisos.android.terminal.profiles.EXTRA_PROFILE_ENCODING";
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private ProfileListFragment profileListFragment;
    private ProfileAdapter profileAdapter;
    private Fragment currentlyVisibleFragment;
    private static final int REQ_PROFILE_IMPORT = 1;
    private static final int REQ_PERMISSION_IMPORT = 1;
    private static final int REQ_PERMISSION_EXPORT = 2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(profileListFragment != null && profileListFragment.isVisible()) {
                    onBackPressed();
                } else if (currentlyVisibleFragment != null && currentlyVisibleFragment instanceof CreateProfileFragment) {
                    switchBackFromCreateProfile();
                } else if (currentlyVisibleFragment != null && currentlyVisibleFragment instanceof EditProfileFragment) {
                    switchBackFromEditProfile();
                }
            }
        });
        toolbar.inflateMenu(R.menu.profile_menu);
        toolbar.getOverflowIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.profile_import:
                        if(hasImportPermission()) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/*");
                            startActivityForResult(intent, REQ_PROFILE_IMPORT);
                        }
                        break;
                    case R.id.profile_export:
                        if(hasExportPermission()) {
                            ExportProfileDialog dialog = new ExportProfileDialog(profileAdapter.getProfiles(), profileListFragment);
                            dialog.show(getSupportFragmentManager(), "ExportProfileDialog");
                        }
                        break;
                }
                return true;
            }
        });

        profileAdapter = new ProfileAdapter(getApplication(), this, getSupportFragmentManager());

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCreateProfileScreen();
            }
        });
        initFragment();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQ_PROFILE_IMPORT && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            // Get real path
            // Index of wrong path starts after Download/. That's why we add +9 -> length of Download/
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +
                     uri.getPath().substring(uri.getPath().indexOf("Download")+8);
            File file = new File(path);
            ReadFileTask task = new ReadFileTask(file);
            task.execute();
        }
    }

    private class ReadFileTask extends AsyncTask<Void, Void, Void> {

        private File file;
        private String response;

        public ReadFileTask(File file) {
            String extension = file.getPath().substring(file.getPath().lastIndexOf("."));
            if(extension.equals(".json")) {
                this.file = file;
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                if(file == null) {
                    return null;
                }
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
                }
                bufferedReader.close();
                response = stringBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(response != null && !response.isEmpty()) {
                try {
                    JSONArray array = new JSONArray(response);
                    // Check if file belongs to the app
                    try {
                        JSONObject obj = array.getJSONObject(0);
                        String id = obj.getString(JSONBuilder.FILE_IDENTIFIER);
                        if (id == null || id.isEmpty() || !id.equals(JSONBuilder.FILE_IDENTIFIER_EXTRA)) {
                            throw new JSONException("id is not valid");
                        }
                    } catch (JSONException e) {
                        Log.d(CustomLogger.TAG, "File format is wrong!");
                        ImportFileErrorDialog dialog = new ImportFileErrorDialog();
                        dialog.show(getSupportFragmentManager(), "ImportFailedDialog");
                        return;
                    }

                    int count = profileAdapter.getItemCount()+1;
                    for (int i = 1; i < array.length(); i++) {
                        JSONObject profileObj = array.getJSONObject(i);
                        Profile profile = JSONBuilder.convertJSONToProfile(profileObj, count);
                        count++;
                        profileAdapter.createProfile(profile);
                    }
                    Snackbar snackbar = Snackbar.make(profileListFragment.getView(), getString(R.string.profileImportSuccess), Snackbar.LENGTH_SHORT);
                    snackbar.getView().setBackgroundColor(getResources().getColor(R.color.greenSuccess));
                    snackbar.show();

                } catch (JSONException e) {
                    Snackbar snackbar = Snackbar.make(profileListFragment.getView(), getString(R.string.profileImportError), Snackbar.LENGTH_LONG);
                    snackbar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    snackbar.show();
                }
            } else {
                Log.d(CustomLogger.TAG, "STRING OF JSON IS EMPTY");
                Snackbar snackbar = Snackbar.make(profileListFragment.getView(), getString(R.string.profileImportError), Snackbar.LENGTH_LONG);
                snackbar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                snackbar.show();
            }
        }
    }

    private boolean hasExportPermission() {
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSION_EXPORT);
            return false;
        }
        return true;
    }

    private boolean hasImportPermission() {
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSION_IMPORT);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQ_PERMISSION_IMPORT:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }
                break;
            case REQ_PERMISSION_EXPORT:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ExportProfileDialog dialog = new ExportProfileDialog(profileAdapter.getProfiles(), profileListFragment);
                    dialog.show(getSupportFragmentManager(), "ExportProfileDialog");
                }
                break;
        }
    }

    private void initFragment() {
        String action = getIntent().getAction();
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        profileListFragment = new ProfileListFragment(profileAdapter);
        transaction.add(R.id.frag_container, profileListFragment);

        if(action != null && action.equals(ACTION_ADD_PROFILE)) {
            CreateProfileFragment frag = new CreateProfileFragment(getApplication(), profileAdapter);
            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_PROFILE_COMMAND, getIntent().getIntExtra(EXTRA_PROFILE_COMMAND, 0));
            bundle.putInt(EXTRA_PROFILE_ENCODING, getIntent().getIntExtra(EXTRA_PROFILE_ENCODING, 0));
            frag.setArguments(bundle);
            transaction.add(R.id.frag_container, frag);
            transaction.hide(profileListFragment);
            currentlyVisibleFragment = frag;
            fab.hide();
            toolbar.getMenu().setGroupVisible(R.id.profile_item_group, false);
        }
        transaction.commit();
    }

    public void setFabVisibility(boolean value) {
        if(value) {
            fab.show();
        } else {
            fab.hide();
        }
    }

    private void openCreateProfileScreen() {
        fab.hide();
        toolbar.getMenu().setGroupVisible(R.id.profile_item_group, false);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.hide(profileListFragment);
        CreateProfileFragment createProfileFragment = new CreateProfileFragment(getApplication(), profileAdapter);
        currentlyVisibleFragment = createProfileFragment;
        transaction.add(R.id.frag_container, createProfileFragment);
        transaction.commit();
    }

    public void openEditProfileScreen(int position) {
        fab.hide();
        toolbar.getMenu().setGroupVisible(R.id.profile_item_group, false);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.hide(profileListFragment);
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        EditProfileFragment editProfileFragment = new EditProfileFragment(getApplication(), profileAdapter);
        editProfileFragment.setArguments(bundle);
        currentlyVisibleFragment = editProfileFragment;
        transaction.add(R.id.frag_container, editProfileFragment);
        transaction.commit();
    }

    private void switchBackFromCreateProfile() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.remove(currentlyVisibleFragment);
        transaction.show(profileListFragment);
        transaction.commit();
        fab.show();
        toolbar.getMenu().setGroupVisible(R.id.profile_item_group, true);
    }

    private void switchBackFromEditProfile() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.remove(currentlyVisibleFragment);
        transaction.show(profileListFragment);
        transaction.commit();
        fab.show();
        toolbar.getMenu().setGroupVisible(R.id.profile_item_group, true);
    }

    public void onProfileCreated() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.remove(currentlyVisibleFragment);
        transaction.show(profileListFragment);
        transaction.commit();
        fab.show();
        profileListFragment.checkAdapterList();
    }

    public void onProfileEdited() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.remove(currentlyVisibleFragment);
        transaction.show(profileListFragment);
        transaction.commit();
        fab.show();
        profileListFragment.checkAdapterList();
    }

    public void onProfileSelected(Profile profile) {
        ArrayList<DeviceInstanceFragment> fragments = TerminalFragment.getFragment().getTerminalInstances();
        DeviceInstanceFragment fragment = fragments.get(TerminalFragment.getFragment().getSelectedTabPosition());
        fragment.onProfileSelected(profile.getCommand());
        onBackPressed();
    }

    public ProfileListFragment getProfileListFragment() {
        return profileListFragment;
    }

    public ProfileAdapter getAdapter() {
        return profileAdapter;
    }

    @Override
    protected void onPause() {
        super.onPause();
        ScanFragment.getFragment().unbindActivityFromService(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScanFragment.getFragment().bindActivityToService(this);
    }
}
