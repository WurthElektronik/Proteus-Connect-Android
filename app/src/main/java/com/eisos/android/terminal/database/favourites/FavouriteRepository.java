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
import android.os.AsyncTask;

import com.eisos.android.terminal.database.ProteusConnectDB;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class FavouriteRepository {
    private FavouriteDao favouriteDao;
    private List<FavouriteDevice> allDevices;

    public FavouriteRepository(Application application) {
        ProteusConnectDB db = ProteusConnectDB.getInstance(application);
        favouriteDao = db.favouriteDao();
        updateList();
    }

    public void insert(FavouriteDevice favouriteDevice) {
        new InsertTask(favouriteDao).execute(favouriteDevice);
    }

    public void delete(FavouriteDevice favouriteDevice) {
        new DeleteTask(favouriteDao).execute(favouriteDevice);
    }

    public List<FavouriteDevice> getAll() {
        return allDevices;
    }

    public void updateList() {
        try {
            allDevices =  new ReadTask(favouriteDao).execute().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class InsertTask extends AsyncTask<FavouriteDevice, Void, Void> {
        private FavouriteDao favouriteDao;

        public InsertTask(FavouriteDao favouriteDao) {
            this.favouriteDao = favouriteDao;
        }

        @Override
        protected Void doInBackground(FavouriteDevice... favouriteDevices) {
            favouriteDao.insert(favouriteDevices[0]);
            return null;
        }
    }

    private class DeleteTask extends AsyncTask<FavouriteDevice, Void, Void> {
        private FavouriteDao favouriteDao;

        public DeleteTask(FavouriteDao favouriteDao) {
            this.favouriteDao = favouriteDao;
        }

        @Override
        protected Void doInBackground(FavouriteDevice... favouriteDevices) {
            favouriteDao.delete(favouriteDevices[0]);
            return null;
        }
    }

    private class ReadTask extends AsyncTask<Void, Void, List<FavouriteDevice>> {

        private FavouriteDao favouriteDao;

        public ReadTask(FavouriteDao favouriteDao) {
            this.favouriteDao = favouriteDao;
        }

        @Override
        protected List<FavouriteDevice> doInBackground(Void... voids) {
            return favouriteDao.getAll();
        }
    }
}
