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
package com.eisos.android.terminal.customLayout;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.eisos.android.R;

import java.io.Serializable;

public class ScanListItem extends ConstraintLayout implements Serializable {

    public static final int CONNECTING = 0;
    public static final int CONNECTED = 1;
    public static final int DISCONNECTED = -1;
    private int ID;
    private static int ID_COUNTER = 0;
    private String deviceName, deviceAddress, rssiText;
    private int connectionState;
    private int rssi;
    private BluetoothDevice device;
    private int imgResourceRssi, imgResourceConState;
    private int color;
    private float alpha;
    private boolean updating;
    private boolean menuVisibility = true;

    /**
     * @param context the ApplicationContext
     * @param device  the bluetooth device
     */
    public ScanListItem(Context context, BluetoothDevice device) {
        super(context);
        this.device = device;
        String name = device.getName();
        if(name == null) {
            this.deviceName = "-";
        }else {
            this.deviceName = name;
        }
        this.deviceAddress = device.getAddress();
        this.color = R.color.colorPrimary;
        this.alpha = 1.0f;
        this.updating = false;
        ID_COUNTER++;
        ID = ID_COUNTER;
        setConnectionState(DISCONNECTED);
    }

    public int getID() {
        return this.ID;
    }

    public void setRSSI(int rssi) {
        this.rssi = rssi;
        rssiText = (rssi + " dBm");

        // Threshold of the rssi values
        if (rssi > -60) {
            imgResourceRssi = R.drawable.ic_signal_full;
        } else if (rssi > -70) {
            imgResourceRssi = R.drawable.ic_signal_3;
        } else if (rssi > -80) {
            imgResourceRssi = R.drawable.ic_signal_2;
        } else if (rssi > -110) {
            imgResourceRssi = R.drawable.ic_signal_1;
        }
    }

    public void setSignalOutOfRange() {
        imgResourceRssi = R.drawable.ic_signal_off;
    }

    public void setRssiColor(int color) {
        this.color = color;
    }

    public void setRssiAlpha(float alpha) {
        this.alpha = alpha;
    }

    public int getRssiColor() {
        return this.color;
    }

    public float getRssiAlpha() {
        return this.alpha;
    }

    public int getRssi() {
        return this.rssi;
    }

    public void setItemUpdateStatus(boolean value) {
        this.updating = value;
    }

    public boolean isItemUpdating() {
        return this.updating;
    }

    public int getImgResourceRssi() {
        return imgResourceRssi;
    }

    public int getImgResourceConState() {
        return imgResourceConState;
    }

    public String getRssiText() {
        return rssiText;
    }

    public BluetoothDevice getDevice() {
        return this.device;
    }

    /**
     * @return The current state of the connection
     */
    public int getConnectionState() {
        return this.connectionState;
    }

    /**
     * Changes the name of the device
     * @param name The name of the device
     */
    public void setDeviceName(String name) {
        this.deviceName = name;
    }

    /**
     * @return The name of the device
     */
    public String getDeviceName() {
        return this.deviceName;
    }

    /**
     * @return The MAC address of the device
     */
    public String getDeviceAddress() {
        return this.deviceAddress;
    }

    /**
     * @param state The value of which will be set (Disconnected, Connecting or Connected)
     */
    public void setConnectionState(int state) {
        this.connectionState = state;
        switch (state) {
            case DISCONNECTED:
                setConnectionStateIcon(R.drawable.ic_info_red);
                break;
            case CONNECTING:
                setConnectionStateIcon(R.drawable.ic_sync_red);
                break;
            case CONNECTED:
                setConnectionStateIcon(R.drawable.ic_check_red);
                break;
        }
    }

    /**
     * @param resource The image of the connection state
     */
    private void setConnectionStateIcon(int resource) {
        this.imgResourceConState = resource;
    }

    public void setMenuVisibility(boolean value) {
        menuVisibility = value;
    }

    public boolean getMenuVisibility() {
        return this.menuVisibility;
    }
}
