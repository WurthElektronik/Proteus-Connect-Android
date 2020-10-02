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

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.eisos.android.terminal.utils.CustomLogger;
import com.eisos.android.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class ConfigGPIO implements Serializable {

    public static final String EXTRA_CONFIG = "com.eisos.android.terminal.bluetooth.gpio.EXRA_CONFIG";
    private final static byte BLOCK_LENGTH_3 = 0x03;
    private final static byte BLOCK_LENGTH_2 = 0x02;
    // Config
    public final static byte AMBER_CONFIG_GPIO_HEADER_DATA = 0x02;
    public final static byte AMBER_WRITE_CONFIG_GPIO_REQ = 0x28;
    public final static byte AMBER_WRITE_CONFIG_GPIO_RESP = 0x68;
    public final static byte AMBER_READ_CONFIG_COMMAND = 0x2C;
    public final static byte AMBER_READ_CONFIG_RESP = 0x6C;
    // Write
    public final static byte AMBER_WRITE_REQ = 0x29;
    public final static byte AMBER_WRITE_RESP = 0x69;
    // Read
    public final static byte AMBER_READ_REQ = 0x2A;
    public final static byte AMBER_READ_RESP = 0x6A;
    // Input
    public static final byte FUNCTION_INPUT = 0x01;
    public static final byte VALUE_INPUT_NO_PULL = 0x00;
    public static final byte VALUE_INPUT_PULL_DOWN = 0x01;
    public static final byte VALUE_INPUT_PULL_UP = 0x02;
    // Output
    public static final byte FUNCTION_OUTPUT = 0x02;
    public static final byte VALUE_OUTPUT_LOW = 0x00;
    public static final byte VALUE_OUTPUT_HIGH = 0x01;
    // Response Code
    public static final byte SUCCESS = 0x00;
    public static final byte FAILED = 0x01;
    public static final byte NOT_ALLOWED = (byte) 0xFF;
    // Board stats changed
    public static final byte ABMER_LOCAL_WRITE_RESP = (byte) 0xA6;
    public static final byte FUNCTION_NOT_CONFIGURED = 0x00;

    // Proteus III configurable GPIO pins
    public static final int NUMBER_OF_PINS = 6;
    public static final byte PIN_B1 = 0x01;
    public static final byte PIN_B2 = 0x02;
    public static final byte PIN_B3 = 0x03;
    public static final byte PIN_B4 = 0x04;
    public static final byte PIN_B5 = 0x05;
    public static final byte PIN_B6 = 0x06;

    private Context context;
    // Map which holds the config states of the pins
    private HashMap<Byte, GpioPin> confPinStates;
    // Map which holds the volatile states of the rw config
    private HashMap<Byte, GpioPin> rwPinStates;
    private boolean rwValuesRead = false;


    public ConfigGPIO(Context context) {
        this.context = context;
        confPinStates = new HashMap<>();
        confPinStates.put(PIN_B1, new GpioPin(PIN_B1, FUNCTION_NOT_CONFIGURED, (byte) 0x00));
        confPinStates.put(PIN_B2, new GpioPin(PIN_B2, FUNCTION_NOT_CONFIGURED, (byte) 0x00));
        confPinStates.put(PIN_B3, new GpioPin(PIN_B3, FUNCTION_NOT_CONFIGURED, (byte) 0x00));
        confPinStates.put(PIN_B4, new GpioPin(PIN_B4, FUNCTION_NOT_CONFIGURED, (byte) 0x00));
        confPinStates.put(PIN_B5, new GpioPin(PIN_B5, FUNCTION_NOT_CONFIGURED, (byte) 0x00));
        confPinStates.put(PIN_B6, new GpioPin(PIN_B6, FUNCTION_NOT_CONFIGURED, (byte) 0x00));
        rwPinStates = new HashMap<>(confPinStates);
    }

    public void saveConfig(HashMap<Byte, GpioPin> map) {
        for(int i = 0; i < map.size(); i++) {
            Byte pinID =  (Byte) map.keySet().toArray()[i];
            GpioPin pin = (GpioPin) map.values().toArray()[i];
            this.confPinStates.put(pinID, pin);
        }
        rwPinStates = new HashMap<>(confPinStates);
        rwValuesRead = false;
    }

    public GpioPin getConfigPinState(byte pinID) {
        if(confPinStates.containsKey(pinID)) {
            return confPinStates.get(pinID);
        }
        return null;
    }

    public HashMap<Byte, GpioPin> getConfigPinStates() {
        return new HashMap<>(this.confPinStates);
    }

    public boolean hasRWValuesBeenRead() {
        return this.rwValuesRead;
    }

    public void setRWValuesReadState(boolean value) {
        this.rwValuesRead = value;
    }

    /**
     * This method returns the temporary GpioPin Read-Write states
     * @param pinID The pinID which state should be returned
     * @return The GpioPin of the pinID
     */
    public GpioPin getRWPinState(byte pinID) {
        if(rwPinStates.containsKey(pinID)) {
            return rwPinStates.get(pinID);
        }
        return null;
    }

    public HashMap<Byte, GpioPin> getRwPinStates() {
        return new HashMap<>(this.rwPinStates);
    }

    public void saveRWValues(HashMap<Byte, GpioPin> map) {
        for(int i = 0; i < map.size(); i++) {
            Byte pinID =  (Byte) map.keySet().toArray()[i];
            GpioPin pin = (GpioPin) map.values().toArray()[i];
            this.rwPinStates.put(pinID, pin);
        }
    }

    /**
     * Maps the tab title to the pin id
     * @param name The tab title
     * @return The pin id
     */
    public Byte getIDForName(String name) {
        if(name.contains(context.getString(R.string.pinB1))) {
            return PIN_B1;
        } else if(name.contains(context.getString(R.string.pinB2))) {
            return PIN_B2;
        } else if(name.contains(context.getString(R.string.pinB3))) {
            return PIN_B3;
        } else if(name.contains(context.getString(R.string.pinB4))) {
            return PIN_B4;
        } else if(name.contains(context.getString(R.string.pinB5))) {
            return PIN_B5;
        } else if(name.contains(context.getString(R.string.pinB6))) {
            return PIN_B6;
        } else {
            return -1;
        }
    }

    /** Check the result codes of the returned blocks
     *
     * @return The blocks whose results were OK
     * */
    public byte[] checkResp(String[] blocks) {
        byte[] pinIds = new byte[blocks.length];
        for(int i = 0; i < pinIds.length; i++) {
            if(blocks[i] != null && blocks[i].length() == 6) {
                // 0-2 length
                String strPinID = blocks[i].substring(2, 4);
                byte bPinID = (byte) Integer.parseInt(strPinID, 16);
                String strResp = blocks[i].substring(4);
                byte bResp = (byte) Integer.parseInt(strResp, 16);
                if(bResp == ConfigGPIO.SUCCESS) {
                    pinIds[i] = bPinID;
                    Log.d(CustomLogger.TAG, "Pin " + bPinID + " successfully configured.");
                } else if(bResp == ConfigGPIO.FAILED) {
                    new Handler().post(() -> Toast.makeText(context,"Configuring pin "
                            + bPinID + " failed!", Toast.LENGTH_SHORT).show());
                } else if(bResp == ConfigGPIO.NOT_ALLOWED) {
                    new Handler().post(() -> Toast.makeText(context,"Configuring pin "
                            + bPinID + " not allowed!", Toast.LENGTH_SHORT).show());
                }
            }
        }
        return pinIds;
    }

    /** Turns a block array into single {@link GpioPin}
     * @param block Array which holds the blocks of the response
     *              e.g. 020100 (not configured) or 02010100 (configured)
     * @return ArrayList which holds configured GpioPins
     * */
    public ArrayList<GpioPin> blocksToGPIOPins(String[] block) {
        ArrayList<GpioPin> pins = new ArrayList<>();
        for (int i = 0; i < block.length; i++) {
            if(block[i].length() == 4) { // Not configured
                byte pinId = (byte) Integer.parseInt(block[i].substring(0, 2), 16);
                byte function = FUNCTION_NOT_CONFIGURED;
                pins.add(new GpioPin(pinId, function, (byte) 0x99)); // 0x99 to differ between "Default" 0x00
            } else if(block[i].length() == 6) { // Configured
                byte pinId = (byte) Integer.parseInt(block[i].substring(0, 2), 16);
                byte function = (byte) Integer.parseInt(block[i].substring(2, 4), 16);
                byte value = (byte) Integer.parseInt(block[i].substring(4), 16);
                pins.add(new GpioPin(pinId, function, value));
            }
        }
        return pins;
    }

    public ArrayList<GpioPin> updateRWGPIOPinValue(String[] block) {
        ArrayList<GpioPin> pins = new ArrayList<>();
        for(int i = 0; i < block.length; i++) {
            if(block[i] != null && block[i].length() == 6) {
                // 0 - 2 block length
                byte pinId = (byte) Integer.parseInt(block[i].substring(2, 4), 16);
                byte value = (byte) Integer.parseInt(block[i].substring(4), 16);
                GpioPin oldPin = rwPinStates.get(pinId);
                pins.add(new GpioPin(pinId, oldPin.getFunction(), value));
                Log.d(CustomLogger.TAG, "New pin value: " + String.format("%02X", pinId) + ", "
                        + String.format("%02X", oldPin.getFunction())
                        + ", " + String.format("%02X", value));
            }
        }
        return pins;
    }

    /** Build request blocks for {@link #AMBER_WRITE_CONFIG_GPIO_REQ}
     * @param gpio_id The id of the GPIOPin
     * @param function The function of the GPIOPin
     * @param value The value of the GPIOPin
     * @return The write config request block
     * */
    public String buildWriteConfigBlock(byte gpio_id, byte function, byte value) {
        // Block:  LENGTH | GPIO_ID | FUNCTION | VALUE
        String block = new StringBuilder().append(String.format("%02X", BLOCK_LENGTH_3))
                .append(String.format("%02X", gpio_id))
                .append(String.format("%02X", function))
                .append(String.format("%02X", value)).toString();
        Log.d(CustomLogger.TAG, "WriteConfBlock: " + block);
        return block;
    }

    /** Build response blocks for {@link #AMBER_READ_CONFIG_RESP}
     * @param data The complete block data
     * @return A String array with the single blocks
     * */
    public static String[] buildReadConfigRespBlock(String data) {
        String[] values = new String[NUMBER_OF_PINS];
        int x = 0;
        int y = 0;
        for(int i = 0; i < values.length; i++) {
            // Split block in pin info
            byte length = (byte) Integer.parseInt(data.substring(x, x+2), 16);
            if(length == BLOCK_LENGTH_2) { // Pin is not configured
                x += 2; // Skip block length
                y += 6;
                values[i] = data.substring(x, y);
                x += 4; // Go to next block
            } else if(length == BLOCK_LENGTH_3) { // Pin is configured
                x += 2;
                y += 8;
                values[i] = data.substring(x, y);
                x += 6;
            }
        }
        return values;
    }

    /** Build request blocks for {@link #AMBER_WRITE_REQ}
     * @param gpio_id The id of the GPIOPin
     * @param value The value the GPIOPin shall be set to
     * @return The write request block
     * */
    public String buildWriteReqBlock(byte gpio_id, byte value) {
        // Block: LENGTH | GPIO_ID | VALUE
        String block = new StringBuilder().append(String.format("%02X", BLOCK_LENGTH_2))
                .append(String.format("%02X", gpio_id))
                .append(String.format("%02X", value))
                .toString();
        return block;
    }

    /** Build response blocks for {@link #AMBER_WRITE_CONFIG_GPIO_RESP} and {@link #AMBER_WRITE_RESP}
     * @param data The complete block data
     * @return A String array with the single blocks
     * */
    public static String[] buildWriteRespBlock(String data) {
        String[] blocks = new String[NUMBER_OF_PINS];
        // Data gets split into single blocks
        int length = data.length();
        if(length/NUMBER_OF_PINS == 1) {
            blocks[0] = data;
        } else if (length/NUMBER_OF_PINS == 2) {
            blocks[0] = data.substring(0, 6);
            blocks[1] = data.substring(6);
        } else if (length/NUMBER_OF_PINS == 3) {
            blocks[0] = data.substring(0, 6);
            blocks[1] = data.substring(6, 12);
            blocks[2] = data.substring(12);
        } else if (length/NUMBER_OF_PINS == 4) {
            blocks[0] = data.substring(0, 6);
            blocks[1] = data.substring(6, 12);
            blocks[2] = data.substring(12, 18);
            blocks[3] = data.substring(18);
        } else if (length/NUMBER_OF_PINS == 5) {
            blocks[0] = data.substring(0, 6);
            blocks[1] = data.substring(6, 12);
            blocks[2] = data.substring(12, 18);
            blocks[3] = data.substring(18, 24);
            blocks[4] = data.substring(24);
        } else if (length/NUMBER_OF_PINS == 6) {
            blocks[0] = data.substring(0, 6);
            blocks[1] = data.substring(6, 12);
            blocks[2] = data.substring(12, 18);
            blocks[3] = data.substring(18, 24);
            blocks[4] = data.substring(24, 30);
            blocks[5] = data.substring(30);
        }
        return blocks;
    }

    /** Build request blocks for {@link #AMBER_READ_REQ}
     * @param gpio_ids All the id's of the GPIOPins which shall be read
     * @return The read request block
     * */
    public static String buildReadReqBlock(byte... gpio_ids) {
        // Block: LENGTH | GPIO_IDs...
        StringBuilder strBuilder = new StringBuilder();
        String length = String.format("%02X", gpio_ids.length);
        strBuilder.append(length);
        for(byte b : gpio_ids) {
            strBuilder.append(String.format("%02X", b));
        }
        String block = strBuilder.toString();
        return block;
    }

    /** Build response blocks for {@link #AMBER_READ_RESP}
     * @param data The complete block data
     * @return A String array with the single blocks
     * */
    public static String[] buildReadRespBlock(String data) {
        String[] blocks = new String[NUMBER_OF_PINS];
        // Data gets split into single blocks
        int length = data.length();
        if(length/NUMBER_OF_PINS == 1) {
            blocks[0] = data;
        } else if (length/NUMBER_OF_PINS == 2) {
            blocks[0] = data.substring(0, 6);
            blocks[1] = data.substring(6);
        } else if (length/NUMBER_OF_PINS == 3) {
            blocks[0] = data.substring(0, 6);
            blocks[1] = data.substring(6, 12);
            blocks[2] = data.substring(12);
        } else if (length/NUMBER_OF_PINS == 4) {
            blocks[0] = data.substring(0, 6);
            blocks[1] = data.substring(6, 12);
            blocks[2] = data.substring(12, 18);
            blocks[3] = data.substring(18);
        } else if (length/NUMBER_OF_PINS == 5) {
            blocks[0] = data.substring(0, 6);
            blocks[1] = data.substring(6, 12);
            blocks[2] = data.substring(12, 18);
            blocks[3] = data.substring(18, 24);
            blocks[4] = data.substring(24);
        } else if (length/NUMBER_OF_PINS == 6) {
            blocks[0] = data.substring(0, 6);
            blocks[1] = data.substring(6, 12);
            blocks[2] = data.substring(12, 18);
            blocks[3] = data.substring(18, 24);
            blocks[4] = data.substring(24, 30);
            blocks[5] = data.substring(30);
        }
        return blocks;
    }
}
