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

package com.eisos.android.terminal.adapter;

import android.app.Application;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eisos.android.R;
import com.eisos.android.terminal.database.profiles.Profile;
import com.eisos.android.terminal.database.profiles.ProfileViewModel;
import com.eisos.android.terminal.dialogs.DeleteProfileDialog;
import com.eisos.android.terminal.profiles.ProfileActivity;

import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileHolder> {

    private ProfileViewModel profileViewModel;
    private Application application;
    private FragmentManager fm;
    private List<Profile> profiles;
    private ProfileActivity profileActivity;
    private ProfileAdapter profileAdapter;

    public ProfileAdapter(Application application, ProfileActivity profileActivity,
                          FragmentManager fm) {
        this.profileAdapter = this;
        this.application = application;
        this.fm = fm;
        this.profileActivity = profileActivity;
        this.profileViewModel = new ProfileViewModel(application);
        profiles = profileViewModel.getAll();
    }

    @NonNull
    @Override
    public ProfileHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(this.application)
                .inflate(R.layout.profile_card_option1, parent, false);
        return new ProfileHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileHolder holder, int position) {
        final Profile profile = profiles.get(position);
        holder.profileName.setText(profile.getName());
        holder.profileDesc.setText(profile.getDescription());
        holder.profileEncoding.setText(application.getString(R.string.encodingDialogTitle) + ": " + profile.getEncodingFormat());
        holder.profileCommand.setText(application.getString(R.string.profileCommand) + ": "
                + profile.getCommand());
        holder.tvSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileActivity.onProfileSelected(profile);
            }
        });
        holder.tvEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileActivity.openEditProfileScreen(position);
            }
        });
        holder.tvDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteProfileDialog dialog = new DeleteProfileDialog(profileAdapter, position,
                        profile, profileActivity);
                dialog.show(fm, "DeleteProfileDialog");
            }
        });
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    public List<Profile> getProfiles() {
        return this.profiles;
    }

    public void createProfile(Profile profile) {
        profileViewModel.insert(profile);
        profileViewModel.updateList();
        profiles = profileViewModel.getAll();
        notifyItemInserted(profiles.size()-1);
    }

    public void updateProfile(int position, Profile profile) {
        profileViewModel.update(profile);
        profileViewModel.updateList();
        profiles = profileViewModel.getAll();
        notifyItemChanged(position);
    }

    public void deleteProfile(int position, Profile profile) {
        profileViewModel.delete(profile);
        profileViewModel.updateList();
        profiles = profileViewModel.getAll();
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    class ProfileHolder extends RecyclerView.ViewHolder {

        public TextView profileName, profileDesc, profileEncoding, profileCommand, tvSelect, tvEdit, tvDelete;

        public ProfileHolder(@NonNull View itemView) {
            super(itemView);
            this.profileName = itemView.findViewById(R.id.profile_name);
            this.profileDesc = itemView.findViewById(R.id.profile_description);
            this.profileEncoding = itemView.findViewById(R.id.profile_panel);
            this.profileCommand = itemView.findViewById(R.id.profile_rgbw_value);
            this.tvSelect = itemView.findViewById(R.id.tv_select);
            this.tvEdit = itemView.findViewById(R.id.tv_edit);
            this.tvDelete = itemView.findViewById(R.id.tv_delete);
        }
    }
}
