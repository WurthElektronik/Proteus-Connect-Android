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

import com.eisos.android.terminal.database.profiles.Profile;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONBuilder {

    public static final String PROFILE_NAME = "profile_name";
    public static final String PROFILE_DESC = "profile_desc";
    public static final String PROFILE_ENCODING = "profile_encoding";
    public static final String PROFILE_COMMAND = "profile_command";
    public static final String FILE_NAME = "ProteusConnect_profile_export.json";
    public static final String FOLDER_NAME = "ProteusConnect";
    public static final String FILE_IDENTIFIER = "id";
    public static final String FILE_IDENTIFIER_EXTRA = "com.eisos.android.terminal.profile.json";

    public static JSONObject buildFileIdentifier() {
        try {
            JSONObject obj = new JSONObject();
            obj.put(FILE_IDENTIFIER, FILE_IDENTIFIER_EXTRA);
            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject buildProfileEntry(Profile profile) {
        try {
            JSONObject obj = new JSONObject();
            obj.put(PROFILE_NAME, profile.getName());
            obj.put(PROFILE_DESC, profile.getDescription());
            obj.put(PROFILE_ENCODING, profile.getEncodingFormat());
            obj.put(PROFILE_COMMAND, profile.getCommand());
            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Profile convertJSONToProfile(JSONObject obj, int position) {
        try {
            String name = obj.getString(PROFILE_NAME);
            String desc = obj.getString(PROFILE_DESC);
            String encoding = obj.getString(PROFILE_ENCODING);
            String command = obj.getString(PROFILE_COMMAND);
            Profile profile = new Profile(name, position, encoding);
            profile.setDescription(desc);
            profile.setCommand(command);
            return profile;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
