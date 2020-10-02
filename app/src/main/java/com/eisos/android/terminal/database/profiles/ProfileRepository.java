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
import android.os.AsyncTask;

import com.eisos.android.terminal.database.ProteusConnectDB;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ProfileRepository {
    private ProfileDao profileDao;
    private List<Profile> allProfiles;

    public ProfileRepository(Application application) {
        ProteusConnectDB db = ProteusConnectDB.getInstance(application);
        profileDao = db.profileDao();
        updateList();
    }

    public void insert(Profile profile) {
        new InsertTask(profileDao).execute(profile);
    }

    public void update(Profile profile) {
        new UpdateTask(profileDao).execute(profile);
    }

    public void delete(Profile profile) {
        new DeleteTask(profileDao).execute(profile);
    }

    public void updateList() {
        try {
            allProfiles =  new ReadTask(profileDao).execute().get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public List<Profile> getAll() {
        return allProfiles;
    }

    private class InsertTask extends AsyncTask<Profile, Void, Void> {
        private ProfileDao profileDao;

        public InsertTask(ProfileDao profileDao) {
            this.profileDao = profileDao;
        }

        @Override
        protected Void doInBackground(Profile... profiles) {
            profileDao.insert(profiles[0]);
            return null;
        }
    }

    private class UpdateTask extends AsyncTask<Profile, Void, Void> {
        private ProfileDao profileDao;

        public UpdateTask(ProfileDao profileDao) {
            this.profileDao = profileDao;
        }

        @Override
        protected Void doInBackground(Profile... profiles) {
            profileDao.update(profiles[0]);
            return null;
        }
    }

    private class DeleteTask extends AsyncTask<Profile, Void, Void> {
        private ProfileDao profileDao;

        public DeleteTask(ProfileDao profileDao) {
            this.profileDao = profileDao;
        }

        @Override
        protected Void doInBackground(Profile... profiles) {
            profileDao.delete(profiles[0]);
            return null;
        }
    }

    private class ReadTask extends AsyncTask<Void, Void, List<Profile>> {

        private ProfileDao profileDao;

        public ReadTask(ProfileDao profileDao) {
            this.profileDao = profileDao;
        }

        @Override
        protected List<Profile> doInBackground(Void... voids) {
            return profileDao.getAll();
        }
    }
}


