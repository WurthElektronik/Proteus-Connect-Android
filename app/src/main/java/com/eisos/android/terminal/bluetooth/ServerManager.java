/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

package com.eisos.android.terminal.bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.ble.BleServerManager;

public class ServerManager extends BleServerManager {
    private final static UUID UART_SERVICE_UUID = UUID.fromString("6E400001-C352-11E5-953D-0002A5D5C51B");
    private final static UUID UART_RX_CHARACTERISTIC_UUID = UUID.fromString("6E400002-C352-11E5-953D-0002A5D5C51B");
    private final static UUID UART_TX_CHARACTERISTIC_UUID = UUID.fromString("6E400003-C352-11E5-953D-0002A5D5C51B");

    public ServerManager(@NonNull final Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected List<BluetoothGattService> initializeServer() {
        return Collections.singletonList(
                service(UART_SERVICE_UUID,
                        characteristic(UART_RX_CHARACTERISTIC_UUID,
                                BluetoothGattCharacteristic.PROPERTY_WRITE // properties
                                        | BluetoothGattCharacteristic.PROPERTY_NOTIFY
                                        | BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS,
                                BluetoothGattCharacteristic.PERMISSION_WRITE, // permissions
                                cccd(), reliableWrite(), description("Some description", false) // descriptors
                        ))
        );
    }
}
