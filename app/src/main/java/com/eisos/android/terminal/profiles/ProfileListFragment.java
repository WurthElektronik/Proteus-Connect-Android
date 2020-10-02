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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eisos.android.R;
import com.eisos.android.terminal.adapter.ProfileAdapter;

public class ProfileListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProfileAdapter profileAdapter;
    private ProfileActivity profileActivity;
    private TextView tvNoProfiles;

    public ProfileListFragment(ProfileAdapter profileAdapter) {
        this.profileAdapter = profileAdapter;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_list, container, false);
        profileActivity = (ProfileActivity) getActivity();
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if(dy > 0) {
                    profileActivity.setFabVisibility(false);
                } else if (dy < 0) {
                    profileActivity.setFabVisibility(true);
                }
            }
        });
        recyclerView.setAdapter(profileAdapter);

        tvNoProfiles = view.findViewById(R.id.tv_no_profiles);
        if(profileAdapter.getItemCount() > 0) {
            tvNoProfiles.setVisibility(View.GONE);
        } else {
            tvNoProfiles.setVisibility(View.VISIBLE);
        }
        return view;
    }

    public boolean canListScroll() {
        return (recyclerView.canScrollVertically(1) || recyclerView.canScrollVertically(-1));
    }

    public void checkAdapterList() {
        if(profileAdapter.getItemCount() > 0) {
            tvNoProfiles.setVisibility(View.GONE);
        } else {
            tvNoProfiles.setVisibility(View.VISIBLE);
        }
    }
}
