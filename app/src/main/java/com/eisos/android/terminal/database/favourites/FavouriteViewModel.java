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

package com.eisos.android.terminal.database.favourites;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.List;

public class FavouriteViewModel extends AndroidViewModel {

    private FavouriteRepository favouriteRepository;

    public FavouriteViewModel(@NonNull Application application) {
        super(application);
        favouriteRepository = new FavouriteRepository(application);
    }

    public void insert(FavouriteDevice favouriteDevice) {
        favouriteRepository.insert(favouriteDevice);
    }

    public void delete(FavouriteDevice favouriteDevice) {
        favouriteRepository.delete(favouriteDevice);
    }

    public List<FavouriteDevice> getAll() {
        return favouriteRepository.getAll();
    }

    public void updateList() {
        favouriteRepository.updateList();
    }
}
