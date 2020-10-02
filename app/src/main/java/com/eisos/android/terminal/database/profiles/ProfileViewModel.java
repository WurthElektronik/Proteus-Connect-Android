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

package com.eisos.android.terminal.database.profiles;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.List;

public class ProfileViewModel extends AndroidViewModel {

    private ProfileRepository profileRepository;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        profileRepository = new ProfileRepository(application);
    }

    public void insert(Profile profile) {
        profileRepository.insert(profile);
    }

    public void update(Profile profile) {
        profileRepository.update(profile);
    }

    public void delete(Profile profile) {
        profileRepository.delete(profile);
    }

    public List<Profile> getAll() {
        return profileRepository.getAll();
    }

    public void updateList() {
        profileRepository.updateList();
    }
}
