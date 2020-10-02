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

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.eisos.android.R;
import com.eisos.android.terminal.bluetooth.services.UARTService;
import com.eisos.android.terminal.frags.DeviceInstanceFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;

public class ConfigDialog extends DialogFragment implements View.OnClickListener {

    public static final String ACTION_CONFIG = "com.eisos.android.terminal.bluetooth.gpio.ACTION_READ_CONFIG";
    public static final String EXTRA_DATA = "com.eisos.android.terminal.bluetooth.gpio.EXTRA_DATA";
    public static final int RESULT_READ_OK = 1;
    private TabLayout tabLayout;
    private ImageButton btnReadConfig;
    private Button btnCancel, btnConfigAll, btnConfigPin;
    private CheckBox cbPullUp, cbPullDown, cbNoPull, cbHigh, cbLow;
    private TextView tvInfo;
    private CheckBox previousCheckedItem;
    /**
     * Stores the pin states which were temporarily made by the user.
     * When data gets written, this map will overwrite {@link ConfigGPIO#getConfigPinStates()}
     */
    private HashMap<Byte, GpioPin> tmpStates;
    private ConfigGPIO configGPIO;
    private BluetoothDevice mDevice;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View dialogView = inflater.inflate(R.layout.dialog_gpio_config, container, false);
        mDevice = getArguments().getParcelable(UARTService.EXTRA_DEVICE);
        configGPIO = (ConfigGPIO) getArguments().getSerializable(ConfigGPIO.EXTRA_CONFIG);
        tabLayout = dialogView.findViewById(R.id.tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showPinConfig(getIDForName(), false);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        btnReadConfig = dialogView.findViewById(R.id.btn_read_config);
        btnReadConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onReadConfigClicked();
            }
        });
        btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        btnConfigAll = dialogView.findViewById(R.id.btn_config_all);
        btnConfigAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onConfigAllClicked();
            }
        });
        btnConfigPin = dialogView.findViewById(R.id.btn_config_pin);
        btnConfigPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GpioPin pin = createGpioPin(getCheckedItemText());
                if (pin != null) {
                    onConfigPinClicked(pin);
                }
            }
        });
        cbPullUp = dialogView.findViewById(R.id.cb_pull_up);
        cbPullUp.setOnClickListener(this);
        cbPullDown = dialogView.findViewById(R.id.cb_pull_down);
        cbPullDown.setOnClickListener(this);
        cbNoPull = dialogView.findViewById(R.id.cb_no_pull);
        cbNoPull.setOnClickListener(this);
        cbHigh = dialogView.findViewById(R.id.cb_high);
        cbHigh.setOnClickListener(this);
        cbLow = dialogView.findViewById(R.id.cb_low);
        cbLow.setOnClickListener(this);
        tvInfo = dialogView.findViewById(R.id.tv_info);
        tabLayout.getTabAt(0).select();
        showPinConfig(ConfigGPIO.PIN_B1, true);
        return dialogView;
    }

    /**
     * Returns the name of the currently selected CheckBox
     *
     * @return The name of the checked CheckBox
     */
    public String getCheckedItemText() {
        if (cbNoPull.isChecked()) {
            return cbNoPull.getText().toString();
        } else if (cbPullUp.isChecked()) {
            return cbPullUp.getText().toString();
        } else if (cbPullDown.isChecked()) {
            return cbPullDown.getText().toString();
        } else if (cbHigh.isChecked()) {
            return cbHigh.getText().toString();
        } else if (cbLow.isChecked()) {
            return cbLow.getText().toString();
        }
        return null;
    }

    public void showPinConfig(byte pinID, boolean updatePinStates) {
        if (updatePinStates) {
            tmpStates = configGPIO.getConfigPinStates();
        }
        deselectCbs();
        GpioPin pin = tmpStates.get(pinID);

        if (pin == null) {
            return;
        }

        byte function = pin.getFunction();
        byte value = pin.getValue();

        tvInfo.setText(null);
        if (function == ConfigGPIO.FUNCTION_INPUT && value == ConfigGPIO.VALUE_INPUT_NO_PULL) {
            cbNoPull.setChecked(true);
        } else if (function == ConfigGPIO.FUNCTION_INPUT && value == ConfigGPIO.VALUE_INPUT_PULL_UP) {
            cbPullUp.setChecked(true);
        } else if (function == ConfigGPIO.FUNCTION_INPUT && value == ConfigGPIO.VALUE_INPUT_PULL_DOWN) {
            cbPullDown.setChecked(true);
        } else if (function == ConfigGPIO.FUNCTION_OUTPUT && value == ConfigGPIO.VALUE_OUTPUT_LOW) {
            cbLow.setChecked(true);
        } else if (function == ConfigGPIO.FUNCTION_OUTPUT && value == ConfigGPIO.VALUE_OUTPUT_HIGH) {
            cbHigh.setChecked(true);
        } else if (function == ConfigGPIO.FUNCTION_NOT_CONFIGURED && value == ((byte) 0x00)) {
            tvInfo.setText(getString(R.string.noConfigAvailable));
        } else if (function == ConfigGPIO.FUNCTION_NOT_CONFIGURED) {
            tvInfo.setText(getString(R.string.pinNotConfigured));
        }
    }

    private GpioPin createGpioPin(String state) {
        GpioPin pin = null;
        byte GPIO_ID = getIDForName();

        if (state == null) {
            return null;
        }
        String stateLowerCase = state.toLowerCase();
        if (stateLowerCase.contains(getString(R.string.pinValueNoPull))) {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_INPUT, ConfigGPIO.VALUE_INPUT_NO_PULL);
        } else if (stateLowerCase.contains(getString(R.string.pinValuePullUp))) {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_INPUT, ConfigGPIO.VALUE_INPUT_PULL_UP);
        } else if (stateLowerCase.contains(getString(R.string.pinValuePullDown))) {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_INPUT, ConfigGPIO.VALUE_INPUT_PULL_DOWN);
        } else if (stateLowerCase.contains(getString(R.string.pinValueLow))) {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_OUTPUT, ConfigGPIO.VALUE_OUTPUT_LOW);
        } else if (stateLowerCase.contains(getString(R.string.pinValueHigh))) {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_OUTPUT, ConfigGPIO.VALUE_OUTPUT_HIGH);
        }
        return pin;
    }

    private void deselectCbs() {
        cbNoPull.setChecked(false);
        cbPullUp.setChecked(false);
        cbPullDown.setChecked(false);
        cbHigh.setChecked(false);
        cbLow.setChecked(false);
    }

    public void onConfigPinClicked(GpioPin pin) {
        Intent data = new Intent();
        ArrayList<GpioPin> gpioPins = new ArrayList<>();
        gpioPins.add(pin);
        data.putExtra(DeviceInstanceFragment.EXTRA_GPIO_PINS, gpioPins);
        getTargetFragment().onActivityResult(DeviceInstanceFragment.REQ_CONFIG, Activity.RESULT_OK, data);
    }

    public void onConfigAllClicked() {
        Intent data = new Intent();
        ArrayList<GpioPin> gpioPins = new ArrayList<>();
        for (int i = 0; i < tmpStates.size(); i++) {
            GpioPin gpioPin = (GpioPin) tmpStates.values().toArray()[i];
            if (gpioPin.getFunction() != ConfigGPIO.FUNCTION_NOT_CONFIGURED) {
                gpioPins.add(gpioPin);
            }
        }
        if (gpioPins.size() > 0) {
            data.putExtra(DeviceInstanceFragment.EXTRA_GPIO_PINS, gpioPins);
            getTargetFragment().onActivityResult(DeviceInstanceFragment.REQ_CONFIG, Activity.RESULT_OK, data);
        }
    }

    private void onReadConfigClicked() {
        getTargetFragment().onActivityResult(DeviceInstanceFragment.REQ_CONFIG, RESULT_READ_OK, null);
    }

    /**
     * Returns the id of the selected tab
     *
     * @return The selected pin id
     */
    public byte getIDForName() {
        final String name = tabLayout.getTabAt(tabLayout.getSelectedTabPosition()).getText().toString();
        final byte GPIO_ID = configGPIO.getIDForName(name);
        return GPIO_ID;
    }

    /**
     * Saves the states of all successfully read or written pins
     *
     * @param pins The pins which were read or written
     */
    public void saveSuccessfulPinStates(byte[] pins) {
        HashMap<Byte, GpioPin> tmpMap = new HashMap<>();
        for (int i = 0; i < pins.length; i++) {
            if (this.tmpStates.keySet().contains(pins[i])) {
                tmpMap.put(pins[i], this.tmpStates.get(pins[i]));
            }
        }
        configGPIO.saveConfig(tmpMap);
        dismiss();
    }

    @Override
    public void onClick(View v) {
        String state = "none";

        if(((CheckBox) v).isChecked()) {
            deselectCbs();
            state = ((CheckBox) v).getText().toString().toLowerCase();
            ((CheckBox) v).setChecked(true);
        } else {
            deselectCbs();
        }

        GpioPin pin = null;
        byte GPIO_ID = getIDForName();
        if (state.contains(getString(R.string.pinValueNoPull))) {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_INPUT, ConfigGPIO.VALUE_INPUT_NO_PULL);
        } else if (state.contains(getString(R.string.pinValuePullUp))) {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_INPUT, ConfigGPIO.VALUE_INPUT_PULL_UP);
        } else if (state.contains(getString(R.string.pinValuePullDown))) {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_INPUT, ConfigGPIO.VALUE_INPUT_PULL_DOWN);
        } else if (state.contains(getString(R.string.pinValueLow))) {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_OUTPUT, ConfigGPIO.VALUE_OUTPUT_LOW);
        } else if (state.contains(getString(R.string.pinValueHigh))) {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_OUTPUT, ConfigGPIO.VALUE_OUTPUT_HIGH);
        } else if (state.contains("none")) {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_NOT_CONFIGURED, (byte) 0x00);
        }
        tmpStates.put(pin.getID(), pin);
    }
}