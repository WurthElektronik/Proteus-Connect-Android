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

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "devices")
public class FavouriteDevice {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "device_name")
    public String device_name;

    @ColumnInfo(name = "device_address")
    public String device_address;

    public FavouriteDevice(String device_name, String device_address) {
        this.device_name = device_name;
        this.device_address = device_address;
    }

    public int getUid() {
        return uid;
    }

    public String getDeviceName() {
        return device_name;
    }

    public String getDeviceAddress() {
        return device_address;
    }
}
