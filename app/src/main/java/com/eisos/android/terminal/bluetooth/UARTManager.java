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
package com.eisos.android.terminal.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.eisos.android.R;
import com.eisos.android.terminal.bluetooth.gpio.ConfigDialog;
import com.eisos.android.terminal.bluetooth.gpio.ConfigGPIO;
import com.eisos.android.terminal.bluetooth.gpio.ReadWriteDialog;
import com.eisos.android.terminal.bluetooth.interfaces.UARTManagerObserver;
import com.eisos.android.terminal.bluetooth.services.UARTService;
import com.eisos.android.terminal.utils.CustomLogger;
import com.eisos.android.terminal.utils.Parser;
import com.eisos.android.terminal.utils.Preferences;
import com.eisos.android.terminal.utils.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import no.nordicsemi.android.ble.MtuRequest;
import no.nordicsemi.android.ble.PhyRequest;
import no.nordicsemi.android.ble.ReadRssiRequest;
import no.nordicsemi.android.ble.WriteRequest;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.log.LogContract;

public class UARTManager extends LoggableBleManager {

    // Wuerth Electronic UUID's
    private final static UUID UART_SERVICE_UUID = UUID.fromString("6E400001-C352-11E5-953D-0002A5D5C51B");
    private final static UUID UART_RX_CHARACTERISTIC_UUID = UUID.fromString("6E400002-C352-11E5-953D-0002A5D5C51B");
    private final static UUID UART_TX_CHARACTERISTIC_UUID = UUID.fromString("6E400003-C352-11E5-953D-0002A5D5C51B");
    private final static byte AMBER_RF_HEADER_TYPE_DATA = 0x01;

    private BluetoothGattCharacteristic mRXCharacteristic, mTXCharacteristic;
    /**
     * A flag indicating whether Long Write can be used. It's set to false if the UART RX
     * characteristic has only PROPERTY_WRITE_NO_RESPONSE property and no PROPERTY_WRITE.
     * If you set it to false here, it will never use Long Write.
     *
     * change this flag if you don't want to use Long Write even with Write Request.
     */
    private boolean mUseLongWrite = true;
    private SharedPreferences sharedPrefs;
    // Server characteristics
    private BluetoothGattCharacteristic serverCharacteristic;

    public UARTManager(final Context context) {
        super(context);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @NonNull
    @Override
    public BleManagerGattCallback getGattCallback() {
        return new UARTManagerGattCallback();
    }

    /**
     * BluetoothGatt callbacks for connection/disconnection, service discovery,
     * receiving indication, etc.
     */
    private class UARTManagerGattCallback extends BleManagerGattCallback {

        @Override
        protected void onServerReady(@NonNull BluetoothGattServer server) {
            // Obtain your server attributes.
            serverCharacteristic = server
                    .getService(UART_SERVICE_UUID)
            .getCharacteristic(UART_RX_CHARACTERISTIC_UUID);
        }

        @Override
        protected void initialize() {
            beginAtomicRequestQueue()
                    .add(enableNotifications(mTXCharacteristic))
                    .enqueue();
            readCharacteristic(mRXCharacteristic);
            setNotificationCallback(mTXCharacteristic)
                    .with((device, data) -> {
                        String text = null;
                        if(StringUtils.isHexadecimal(Parser.bytesToHex(data.getValue()))) {
                            text = Parser.bytesToHex(data.getValue());
                        } else if(StringUtils.isAscii(Parser.bytesToString(data.getValue()))){
                            text = Parser.bytesToString(data.getValue());
                        }
                        final String msg = text;
                        // In rare cases it can happen, that the command is displayed in the log
                        // after the answer
                        // Small delay to display the log entry correct
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                convertOutput(msg);
                            }
                        }, 20);
                        setNotificationCallback(mRXCharacteristic);
                        new DataReceiverHandler().onDataReceived(device, text);
                    });
        }

        private void convertOutput(String text) {
            byte command = (byte) Integer.parseInt(text.substring(2, 4), 16);
            byte respCode = (byte) Integer.parseInt(text.substring(text.length()-2), 16);
            if((command == ConfigGPIO.AMBER_WRITE_CONFIG_GPIO_RESP || command == ConfigGPIO.AMBER_WRITE_RESP) && text.length() == 10) {
                byte pin = (byte) Integer.parseInt(text.substring(4, text.length() - 2), 16);
                String extraMsg = "Pin " + pin + " configured";
                if(respCode == ConfigGPIO.SUCCESS) {
                    extraMsg += " (SUCCESS)";
                    log(LogContract.Log.Level.APPLICATION, extraMsg);
                } else if(respCode == ConfigGPIO.FAILED) {
                    extraMsg += " (FAILED)";
                    log(LogContract.Log.Level.WARNING, extraMsg);
                } else if(respCode == ConfigGPIO.NOT_ALLOWED) {
                    extraMsg += " (NOT ALLOWED)";
                    log(LogContract.Log.Level.WARNING, extraMsg);
                }
            } else if (command == ConfigGPIO.AMBER_READ_RESP && text.length() == 10) {
                byte pin = (byte) Integer.parseInt(text.substring(4, text.length() - 2), 16);
                String extraMsg = "Pin " + pin + " value:";
                if(respCode == ConfigGPIO.VALUE_OUTPUT_LOW) {
                    extraMsg += " (LOW)";
                    log(LogContract.Log.Level.APPLICATION, extraMsg);
                } else if(respCode == ConfigGPIO.VALUE_OUTPUT_HIGH) {
                    extraMsg += " (HIGH)";
                    log(LogContract.Log.Level.APPLICATION, extraMsg);
                } else if(respCode == ConfigGPIO.NOT_ALLOWED) {
                    extraMsg += " (FAILED)";
                    log(LogContract.Log.Level.WARNING, extraMsg);
                }
            } else if((command == ConfigGPIO.AMBER_WRITE_CONFIG_GPIO_RESP || command == ConfigGPIO.AMBER_WRITE_RESP)) {
                String extraMsg = "Configuration of pins changed";
                log(LogContract.Log.Level.APPLICATION,  extraMsg);
            } else if (command == ConfigGPIO.AMBER_READ_CONFIG_RESP) {
                String extraMsg = "Configuration read";
                log(LogContract.Log.Level.APPLICATION, extraMsg);
            } else if (command == ConfigGPIO.AMBER_READ_RESP) {
                String extraMsg = "Pins read";
                log(LogContract.Log.Level.APPLICATION, extraMsg);
            }
            Log.d(CustomLogger.TAG, "Received GPIO command: " + text);
        }

        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(UART_SERVICE_UUID);
            if (service != null) {
                mRXCharacteristic = service.getCharacteristic(UART_RX_CHARACTERISTIC_UUID);
                mTXCharacteristic = service.getCharacteristic(UART_TX_CHARACTERISTIC_UUID);
            }

            boolean writeRequest = false;
            boolean writeCommand = false;
            if (mRXCharacteristic != null) {
                final int rxProperties = mRXCharacteristic.getProperties();
                writeRequest = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
                writeCommand = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0;

                // Set the WRITE REQUEST type when the characteristic supports it.
                // This will allow to send long write (also if the characteristic support it).
                // In case there is no WRITE REQUEST property, this manager will divide texts
                // longer then MTU-3 bytes into up to MTU-3 bytes chunks.
                if (writeRequest)
                    mRXCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                else
                    mUseLongWrite = false;
            }

            return mRXCharacteristic != null && mTXCharacteristic != null && (writeRequest || writeCommand);
        }

        @Override
        protected void onDeviceDisconnected() {
            serverCharacteristic = null;
            mRXCharacteristic = null;
            mTXCharacteristic = null;
            mUseLongWrite = true;
        }
    }

    private class DataReceiverHandler implements UARTManagerObserver {

        // TODO Is the check if data is hex or ascii necessary and useful?
        @Override
        public void onDataReceived(BluetoothDevice device, String data) {
            byte dataHeader = (byte) Integer.parseInt(data.substring(0, 2), 16);
            if (StringUtils.isHexadecimal(data)) { // Hex
                if (AMBER_RF_HEADER_TYPE_DATA == dataHeader) {
                    broadcastResponse(device, data);
                } else if (ConfigGPIO.AMBER_CONFIG_GPIO_HEADER_DATA == dataHeader) {
                    // Cut of data header 02
                    respondGPIO(device, data.substring(2));
                }
            } else if (StringUtils.isAscii(data)) { // Ascii
                if (AMBER_RF_HEADER_TYPE_DATA == dataHeader) {
                    broadcastResponse(device, data);
                }
            }
        }

        private void broadcastResponse(BluetoothDevice device, String data) {
            final Intent broadcast = new Intent(UARTService.BROADCAST_UART_RX);
            broadcast.putExtra(UARTService.EXTRA_DEVICE, device);
            broadcast.putExtra(UARTService.EXTRA_DATA, data.substring(2));
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcast);
        }

        private void respondGPIO(BluetoothDevice device, String data) {
            byte command = (byte) Integer.parseInt(data.substring(0, 2), 16);
            if (ConfigGPIO.AMBER_WRITE_CONFIG_GPIO_RESP == command ||
                    ConfigGPIO.AMBER_READ_CONFIG_RESP == command) {
                Intent broadcast = new Intent(ConfigDialog.ACTION_CONFIG);
                broadcast.putExtra(UARTService.EXTRA_DEVICE, device);
                broadcast.putExtra(ConfigDialog.EXTRA_DATA, data);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcast);
            } else if (ConfigGPIO.AMBER_WRITE_RESP == command ||
                    ConfigGPIO.AMBER_READ_RESP == command || command == ConfigGPIO.ABMER_LOCAL_WRITE_RESP) {
                Intent broadcast = new Intent(ReadWriteDialog.ACTION_READ_WRITE);
                broadcast.putExtra(UARTService.EXTRA_DEVICE, device);
                broadcast.putExtra(ConfigDialog.EXTRA_DATA, data);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcast);
            }
        }

        @Override
        public void onDataSent(BluetoothDevice device, String data) {
            byte dataHeader = (byte) Integer.parseInt(data.substring(0, 2), 16);
            if (AMBER_RF_HEADER_TYPE_DATA == dataHeader) {
                final Intent broadcast = new Intent(UARTService.BROADCAST_UART_TX);
                broadcast.putExtra(UARTService.EXTRA_DEVICE, device);
                broadcast.putExtra(UARTService.EXTRA_DATA, data.substring(2));
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcast);
            }
        }
    }

    public ReadRssiRequest getRssi() {
        return readRssi();
    }

    public PhyRequest setPhy(int txPhy, int rxPhy, int phyOptions) {
        return setPreferredPhy(txPhy, rxPhy, phyOptions);
    }

    public PhyRequest getPhy() {
        return readPhy();
    }

    public MtuRequest setMtu(int mMtu) {
        return requestMtu(mMtu);
    }

    public int readMtu() {
        return getMtu();
    }

    /**
     * Sends the given text to RX characteristic.
     * @param text the text to be sent
     */
    public void send(final String text) {
        // Are we connected?
        if (mRXCharacteristic == null)
            return;

        int charEncoding = sharedPrefs.getInt(Preferences.PREF_ENCODING, 0);

        if (!TextUtils.isEmpty(text.trim())) {
            WriteRequest request = null;

            // ASCII
            if(charEncoding == 0) {
                byte[] b = (text).getBytes(StandardCharsets.US_ASCII);
                byte[] c = new byte[1 + b.length];
                c[0] = AMBER_RF_HEADER_TYPE_DATA;
                System.arraycopy(b, 0, c, 1, b.length);
                request = writeCharacteristic(mRXCharacteristic,  c)
                        .with((device, data) -> log(LogContract.Log.Level.APPLICATION,
                                "\"" + data.getStringValue(2) + "\" sent"));

            }
            // HEX
            else {
                try {
                    request = writeCharacteristic(mRXCharacteristic, Parser.parseHexBinary(String.format("%02X", AMBER_RF_HEADER_TYPE_DATA) + text))
                            .with((device, data) -> log(LogContract.Log.Level.APPLICATION,
                                    "\"" + Parser.bytesToHex(data.getValue()).substring(2) + "\" sent"));
                }catch(IllegalArgumentException e) {
                    new Handler().post(() -> Toast.makeText(getContext(), R.string.hexParsingError, Toast.LENGTH_SHORT).show());
                    return;
                }
            }

            // TODO Implement FailRequest to catch Error while writing
            if (!mUseLongWrite) {
                // This will automatically split the long data into MTU-3-byte long packets.
                request.split();
            }
            request.enqueue();
        }
    }

    public void sendGPIOCommand(byte command, String text) {
        // Are we connected?
        if (mRXCharacteristic == null) {
            return;
        }

        String tmp = "";
        boolean tmpBool = false;
        if(command == ConfigGPIO.AMBER_WRITE_CONFIG_GPIO_REQ) {
            tmpBool = true;
            tmp = "Configuring GPIO pin(s)...";
        } else if(command == ConfigGPIO.AMBER_READ_CONFIG_COMMAND) {
            tmpBool = false;
            tmp = "Reading GPIO configuration...";
        }  else if(command == ConfigGPIO.AMBER_WRITE_REQ) {
            tmpBool = true;
            tmp = "writing pin(s)...";
        }  else if (command == ConfigGPIO.AMBER_READ_REQ) {
            tmpBool = true;
            tmp = "reading pin(s)...";
        }
        final boolean showData = tmpBool;
        final String msg = tmp;
        WriteRequest request = writeCharacteristic(mRXCharacteristic, Parser.parseHexBinary(String.format("%02X", ConfigGPIO.AMBER_CONFIG_GPIO_HEADER_DATA)
                + String.format("%02X", command) + text))
                    .with((device, data) -> log(LogContract.Log.Level.APPLICATION,
                                        buildMessage(data, msg, showData)));

        if (!mUseLongWrite) {
            // This will automatically split the long data into MTU-3-byte long packets.
            request.split();
        }
        request.enqueue();
    }

    public String buildMessage(Data data, String msg, boolean showData) {
        String s = "";
        if(showData) {
            s = "\"" + Parser.bytesToHex(data.getValue()) + "\" " + msg;
        } else {
            s = msg;
        }
        return s;
    }
}
