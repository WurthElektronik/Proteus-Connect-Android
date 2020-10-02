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
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.eisos.android.R;
import com.eisos.android.terminal.frags.DeviceInstanceFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;

public class ReadWriteDialog extends DialogFragment {

    public static final String ACTION_READ_WRITE = "com.eisos.android.terminal.bluetooth.gpio.ACTION_READ_WRITE";
    public static final int RESULT_READ_OK = 1;
    private ImageButton btnReadAll;
    private TabLayout tabLayout;
    private LinearLayout linearLayout;
    private Button btnCancel, btnRead, btnWrite, btnWriteAll;
    private ConfigGPIO configGPIO;
    /**
     * Stores the pin states which were temporarily made by the user.
     * When data gets written, this map will overwrite {@link ConfigGPIO#getConfigPinStates()}
     */
    private HashMap<Byte, GpioPin> tmpStates;
    private Switch mSwitch;
    private ImageView imageView;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        configGPIO = (ConfigGPIO) bundle.getSerializable(ConfigGPIO.EXTRA_CONFIG);
        final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_read_write, null);
        btnReadAll = dialogView.findViewById(R.id.btn_read_all);
        btnReadAll.setOnClickListener((View view) -> {
            onReadAllClicked();
        });
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
        linearLayout = dialogView.findViewById(R.id.linear_layout);
        mSwitch = new Switch(getContext());
        mSwitch.setShowText(true);
        mSwitch.setTextOn(getString(R.string.gpioValueHigh));
        mSwitch.setTextOff(getString(R.string.gpioValueLow));
        mSwitch.setOnClickListener((View view) -> {
            onSwitchClicked(view);
        });

        imageView = new ImageView(requireContext());
        imageView.setImageResource(R.drawable.ic_circle);

        btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener((View view) -> dismiss());
        btnRead = dialogView.findViewById(R.id.btn_read);
        btnRead.setOnClickListener((View view) -> onReadPinClicked());
        btnWrite = dialogView.findViewById(R.id.btn_write);
        btnWrite.setOnClickListener((View view) -> onWritePinClicked());
        btnWriteAll = dialogView.findViewById(R.id.btn_write_all);
        btnWriteAll.setOnClickListener((View view) -> onWriteAllClicked());
        showPinConfig(ConfigGPIO.PIN_B1, true);
        final AlertDialog dialog = builder.setView(dialogView).create();
        return dialog;
    }

    private void onWritePinClicked() {
        Intent data = new Intent();
        ArrayList<GpioPin> gpioPins = new ArrayList<>();
        GpioPin pin = tmpStates.get(getIDForName());
        gpioPins.add(pin);
        data.putExtra(DeviceInstanceFragment.EXTRA_GPIO_PINS, gpioPins);
        getTargetFragment().onActivityResult(DeviceInstanceFragment.REQ_READ_WRITE, Activity.RESULT_OK, data);
    }

    private void onWriteAllClicked() {
        Intent data = new Intent();
        ArrayList<GpioPin> gpioPins = new ArrayList<>();
        for(int i = 0; i < tmpStates.size(); i++) {
            GpioPin pin = (GpioPin) tmpStates.values().toArray()[i];
            if(pin.getFunction() == ConfigGPIO.FUNCTION_OUTPUT) {
                gpioPins.add(pin);
            }
        }
        data.putExtra(DeviceInstanceFragment.EXTRA_GPIO_PINS, gpioPins);
        getTargetFragment().onActivityResult(DeviceInstanceFragment.REQ_READ_WRITE, Activity.RESULT_OK, data);
    }

    private void onReadPinClicked() {
        Intent data = new Intent();
        ArrayList<GpioPin> gpioPins = new ArrayList<>();
        GpioPin pin = configGPIO.getRWPinState(getIDForName());
        gpioPins.add(pin);
        data.putExtra(DeviceInstanceFragment.EXTRA_GPIO_PINS, gpioPins);
        getTargetFragment().onActivityResult(DeviceInstanceFragment.REQ_READ_WRITE, RESULT_READ_OK, data);
    }

    private void onReadAllClicked() {
        Intent data = new Intent();
        ArrayList<GpioPin> gpioPins = new ArrayList<>();
        HashMap<Byte, GpioPin> pinStates = configGPIO.getRwPinStates();
        for (int i = 0; i < pinStates.size(); i++) {
            GpioPin gpioPin = (GpioPin) pinStates.values().toArray()[i];
            gpioPins.add(gpioPin);
        }
        if (gpioPins.size() > 0) {
            data.putExtra(DeviceInstanceFragment.EXTRA_GPIO_PINS, gpioPins);
            getTargetFragment().onActivityResult(DeviceInstanceFragment.REQ_READ_WRITE, RESULT_READ_OK, data);
        }
        configGPIO.setRWValuesReadState(true);
    }

    /**
     * Saves the received pin info
     * @param pins The id of the received pins
     */
    public void saveSuccessfulPinStates(byte[] pins) {
        HashMap<Byte, GpioPin> tmpMap = new HashMap<>();
        for (int i = 0; i < pins.length; i++) {
            if (tmpStates.keySet().contains(pins[i])) {
                tmpMap.put(pins[i], tmpStates.get(pins[i]));
            }
        }
        configGPIO.saveRWValues(tmpMap);
    }

    /**
     * Update the dialog ui
     * @param pinID The id of the pin
     * @param updatePinState Reload the pin states from {@link ConfigGPIO#getRwPinStates()} or
     *                       take the states of the temporary list {@link #tmpStates}
     */
    public void showPinConfig(byte pinID, boolean updatePinState) {
        if (updatePinState) {
            tmpStates = configGPIO.getRwPinStates();
        }
        GpioPin pin = tmpStates.get(pinID);
        if (pin == null) {
            return;
        }

        byte function = pin.getFunction();
        byte value = pin.getValue();

        linearLayout.removeAllViews();
         if(function == ConfigGPIO.FUNCTION_NOT_CONFIGURED && value == (byte) 0x00)  {
            TextView textView = new TextView(getContext());
            textView.setText(getString(R.string.dialogRWNoConfig));
            linearLayout.addView(textView);
            enableReadBtns(false);
            enableWriteBtns(false);
        } else if (!configGPIO.hasRWValuesBeenRead()) {
             TextView textView = new TextView(getContext());
             textView.setText(getString(R.string.dialogRWValuesNotRead));
             linearLayout.addView(textView);
             btnReadAll.setEnabled(true);
             btnReadAll.setColorFilter(getResources().getColor(R.color.colorPrimary));
             btnRead.setEnabled(false);
             btnRead.setTextColor(getResources().getColor(android.R.color.darker_gray));
             enableWriteBtns(false);
         } else if (function == ConfigGPIO.FUNCTION_INPUT && value == ConfigGPIO.VALUE_OUTPUT_LOW) {
            TextView textView = new TextView(getContext());
            textView.setText(getString(R.string.gpioValueLow));
            imageView.setColorFilter(getResources().getColor(R.color.black));
            linearLayout.addView(textView);
            linearLayout.addView(imageView);
            enableReadBtns(true);
            enableWriteBtns(false);
        } else if (function == ConfigGPIO.FUNCTION_INPUT && value == ConfigGPIO.VALUE_OUTPUT_HIGH) {
            TextView textView = new TextView(getContext());
            textView.setText(getString(R.string.gpioValueHigh));
            imageView.setColorFilter(getResources().getColor(R.color.greenSuccess));
            linearLayout.addView(textView);
            linearLayout.addView(imageView);
            enableReadBtns(true);
            enableWriteBtns(false);
        } else if (function == ConfigGPIO.FUNCTION_OUTPUT && value == ConfigGPIO.VALUE_OUTPUT_LOW) {
            mSwitch.setChecked(false);
            TextView textView = new TextView(getContext());
            textView.setText(getString(R.string.gpioFunctionOutput) + ": ");
            linearLayout.addView(textView);
            linearLayout.addView(mSwitch);
            enableReadBtns(true);
            enableWriteBtns(true);
        } else if (function == ConfigGPIO.FUNCTION_OUTPUT && value == ConfigGPIO.VALUE_OUTPUT_HIGH) {
            mSwitch.setChecked(true);
            TextView textView = new TextView(getContext());
            textView.setText(getString(R.string.gpioFunctionOutput) + ": ");
            linearLayout.addView(textView);
            linearLayout.addView(mSwitch);
            enableReadBtns(true);
            enableWriteBtns(true);
        } else if(function == ConfigGPIO.FUNCTION_NOT_CONFIGURED) {
            TextView textView = new TextView(getContext());
            textView.setText(getString(R.string.dialogRWPinNotConfigured));
            linearLayout.addView(textView);
            enableReadBtns(true);
            enableWriteBtns(false);
        }
    }

    private void enableWriteBtns(boolean value) {
        this.btnWrite.setEnabled(value);
        this.btnWriteAll.setEnabled(value);
        if(value) {
            this.btnWrite.setTextColor(getResources().getColor(R.color.colorPrimary));
            this.btnWriteAll.setTextColor(getResources().getColor(R.color.colorPrimary));
        } else {
            this.btnWrite.setTextColor(getResources().getColor(android.R.color.darker_gray));
            this.btnWriteAll.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void enableReadBtns(boolean value) {
        this.btnReadAll.setEnabled(value);
        this.btnRead.setEnabled(value);
        if(value) {
            this.btnReadAll.setColorFilter(getResources().getColor(R.color.colorPrimary));
            this.btnRead.setTextColor(getResources().getColor(R.color.colorPrimary));
        } else {
            this.btnReadAll.setColorFilter(getResources().getColor(android.R.color.darker_gray));
            this.btnRead.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    /**
     * Maps the name of the current selected tab to a pin id
     * @return The pin id of the current selected tab
     */
    public byte getIDForName() {
        final String name = tabLayout.getTabAt(tabLayout.getSelectedTabPosition()).getText().toString();
        final byte GPIO_ID = configGPIO.getIDForName(name);
        return GPIO_ID;
    }

    public void onSwitchClicked(View view) {
        Switch mSwitch = (Switch) view;
        boolean state = mSwitch.isChecked();
        GpioPin pin;
        byte GPIO_ID = getIDForName();
        if (state) {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_OUTPUT, ConfigGPIO.VALUE_OUTPUT_HIGH);
        } else {
            pin = new GpioPin(GPIO_ID, ConfigGPIO.FUNCTION_OUTPUT, ConfigGPIO.VALUE_OUTPUT_LOW);
        }
        this.tmpStates.put(pin.getID(), pin);
    }
}
