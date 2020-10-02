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

package com.eisos.android.terminal.bluetooth.gpio;

public class GpioPin {

    // The specific PIN_ID {0x01 - 0x06}
    private byte id;
    // Input or Output
    private byte function;
    // Input {No pull, pull up, pull down}, Output {high, low}
    private byte value;

    public GpioPin(byte id, byte function, byte value) {
        this.id = id;
        this.function = function;
        this.value = value;
    }

    public byte getID() {
        return this.id;
    }

    public byte getFunction() {
        return function;
    }

    public byte getValue() {
        return value;
    }
}
