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
package com.eisos.android.terminal.frags;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.eisos.android.terminal.utils.CustomLogger;
import com.eisos.android.R;
import com.eisos.android.terminal.bluetooth.UARTLogAdapter;
import com.eisos.android.terminal.bluetooth.UARTManager;
import com.eisos.android.terminal.bluetooth.gpio.ConfigDialog;
import com.eisos.android.terminal.bluetooth.gpio.ConfigGPIO;
import com.eisos.android.terminal.bluetooth.gpio.GpioPin;
import com.eisos.android.terminal.bluetooth.gpio.ReadWriteDialog;
import com.eisos.android.terminal.bluetooth.services.BleMulticonnectProfileService;
import com.eisos.android.terminal.bluetooth.services.UARTService;
import com.eisos.android.terminal.dialogs.ReqMtuDialog;
import com.eisos.android.terminal.dialogs.ReqPhyDialog;
import com.eisos.android.terminal.utils.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import no.nordicsemi.android.ble.PhyRequest;
import no.nordicsemi.android.ble.response.PhyResult;
import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LocalLogSession;
import no.nordicsemi.android.log.LogContract;

/**
 * This class represents a device connection
 */
public class DeviceInstanceFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int REQ_CONFIG = 1;
    public static final int REQ_READ_WRITE = 2;
    public static final int REQ_MTU = 10;
    public static final int REQ_PHY = 11;
    public static final String EXTRA_GPIO_PINS = "com.eisos.android.terminal.bluetooth.EXTRA_GPIO_PINS";
    public static final String EXTRA_MTU = "com.eisos.android.terminal.bluetooth.EXTRA_MTU";
    public static final String EXTRA_PHY = "com.eisos.android.terminal.bluetooth.EXTRA_PHY";
    public static final String EXTRA_PHY_OPTIONS = "com.eisos.android.terminal.bluetooth.EXTRA_PHY_OPTIONS";
    private Toolbar deviceToolbar;
    private ListView listView;
    private EditText msgField;
    private TextView noEntries;
    private Button btnSend;
    private Spinner spinnerLogFilter;
    private String tabTitle;
    private SharedPreferences sharedPrefs;
    private static final int LOG_REQUEST_ID = 1;
    private static final int LOG_SCROLL_NULL = -1;
    private static final int LOG_SCROLLED_TO_BOTTOM = -2;
    private static final String[] LOG_PROJECTION = {LogContract.Log._ID, LogContract.Log.TIME, LogContract.Log.LEVEL, LogContract.Log.DATA};
    private UARTManager mManager;
    private int preferredPhy = PhyRequest.PHY_LE_1M_MASK;
    private int preferredOption = PhyRequest.PHY_OPTION_NO_PREFERRED;
    private ConfigGPIO configGPIO;
    /**
     * The adapter used to populate the list with log entries.
     */
    private CursorAdapter mLogAdapter;
    /**
     * The log session created to log events related with the target device.
     */
    private ILogSession mLogSession;
    /**
     * The last list view position.
     */
    private int mLogScrollPosition;
    private String logFilter;
    private BluetoothDevice mDevice;
    private BleMulticonnectProfileService.LocalBinder mBinder;
    private boolean START = true;
    private boolean controlGlobally;
    private ConfigDialog configDialog;
    private ReadWriteDialog readWriteDialog;

    public DeviceInstanceFragment(BluetoothDevice device, BleMulticonnectProfileService.LocalBinder service) {
        mDevice = device;
        tabTitle = mDevice.getName() + "\n" + mDevice.getAddress();
        mBinder = service;
        BleMulticonnectProfileService.TmpDeviceSettingsHelper settings = mBinder.getDeviceSettings(device);
        if(settings != null) {
            preferredPhy = settings.getPreferredPhy();
            preferredOption = settings.getPreferredOption();
        }
        mManager = mBinder.getUARTManager(device);
        mLogSession = mBinder.getLogSession(device);
    }

    public static DeviceInstanceFragment newInstance(BluetoothDevice device, BleMulticonnectProfileService.LocalBinder service) {
        DeviceInstanceFragment instance = new DeviceInstanceFragment(device, service);
        return instance;
    }

    public String getTabTitle() {
        return this.tabTitle;
    }

    public BluetoothDevice getDevice() {
        return this.mDevice;
    }

    /**
     * Receiver which listens for new connections and responses. If a new device connects,
     * enable the message field and the send button
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(UARTService.EXTRA_DEVICE);
            if (mDevice.getAddress().equals(device.getAddress())) {
                if (intent.getAction().equals(UARTService.BROADCAST_UART_RX)) {
                    btnSend.setEnabled(true);
                    msgField.setEnabled(true);
                } else if (intent.getAction().equals(ConfigDialog.ACTION_CONFIG)) { // GPIO config
                    handleGPIOConfig(intent);
                } else if (intent.getAction().equals(ReadWriteDialog.ACTION_READ_WRITE)) {
                    handleGPIOReadWrite(intent);
                }
            }
        }

        /**
         * Checks the response of {@link ConfigGPIO#AMBER_WRITE_CONFIG_GPIO_RESP}
         * and {@link ConfigGPIO#AMBER_READ_CONFIG_RESP}
         * @param intent The intent of the broadcast
         */
        private void handleGPIOConfig(Intent intent) {
            // Receives the response of the actions READ_CONFIG_RESP and WRITE_CONF_RESP
            String data = intent.getStringExtra(ConfigDialog.EXTRA_DATA);
            byte command = (byte) Integer.parseInt(data.substring(0, 2), 16);
            String blocks = data.substring(2);
            if (command == ConfigGPIO.AMBER_WRITE_CONFIG_GPIO_RESP) {
                String[] blockArray = ConfigGPIO.buildWriteRespBlock(blocks);
                byte[] pinIds = configGPIO.checkResp(blockArray);
                configDialog.saveSuccessfulPinStates(pinIds);
            } else if (command == ConfigGPIO.AMBER_READ_CONFIG_RESP) {
                String[] blockArray = ConfigGPIO.buildReadConfigRespBlock(blocks);
                ArrayList<GpioPin> pins = configGPIO.blocksToGPIOPins(blockArray);
                HashMap<Byte, GpioPin> tmpMap = new HashMap<>();
                for (int i = 0; i < blockArray.length; i++) {
                    tmpMap.put(pins.get(i).getID(), pins.get(i));
                }
                configGPIO.saveConfig(tmpMap);
            }

            // Update dialog view
            if (configDialog != null && configDialog.isAdded()) {
                configDialog.showPinConfig(configDialog.getIDForName(), true);
            }
        }

        /**
         * Checks the response of {@link ConfigGPIO#AMBER_WRITE_RESP}
         * and {@link ConfigGPIO#AMBER_READ_RESP}
         * @param intent The intent of the broadcast
         */
        public void handleGPIOReadWrite(Intent intent) {
            // Receives the response of the actions READ_RESP and WRITE_RESP
            String data = intent.getStringExtra(ConfigDialog.EXTRA_DATA);
            byte command = (byte) Integer.parseInt(data.substring(0, 2), 16);
            String blocks = data.substring(2);
            if (command == ConfigGPIO.AMBER_WRITE_RESP) {
                String[] blockArray = ConfigGPIO.buildWriteRespBlock(blocks);
                byte[] pinIds = configGPIO.checkResp(blockArray);
                readWriteDialog.saveSuccessfulPinStates(pinIds);
            } else if (command == ConfigGPIO.ABMER_LOCAL_WRITE_RESP) {
                String[] blockArray = ConfigGPIO.buildWriteRespBlock(blocks);
                configGPIO.updateRWGPIOPinValue(blockArray);
            } else if (command == ConfigGPIO.AMBER_READ_RESP) {
                String[] blockArray = ConfigGPIO.buildReadRespBlock(blocks);
                ArrayList<GpioPin> pins = configGPIO.updateRWGPIOPinValue(blockArray);
                HashMap<Byte, GpioPin> tmpMap = new HashMap<>();
                for (int i = 0; i < pins.size(); i++) {
                    tmpMap.put(pins.get(i).getID(), pins.get(i));
                }
                configGPIO.saveRWValues(tmpMap);
            }

            // Update dialog ui
            if (readWriteDialog != null && readWriteDialog.isAdded()) {
                readWriteDialog.showPinConfig(readWriteDialog.getIDForName(), true);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feature_terminal, container, false);
        configGPIO = new ConfigGPIO(getActivity().getApplicationContext());
        deviceToolbar = view.findViewById(R.id.device_toolbar);
        deviceToolbar.inflateMenu(R.menu.device_menu);
        deviceToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                menuItemClicked(item);
                return true;
            }
        });

        // Only show GPIO menu for Proteus III boards
        if(!mDevice.getAddress().contains(getString(R.string.proteus3Identifier))) {
            deviceToolbar.getMenu().setGroupVisible(R.id.deviceMenuGroup_gpio, false);
        }
        listView = view.findViewById(R.id.terminal_content);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        spinnerLogFilter = view.findViewById(R.id.spinner_log_filter);
        spinnerLogFilter.setSelection(2); // Select option INFO
        spinnerLogFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterLog((String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        msgField = view.findViewById(R.id.et_command);
        msgField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                onSendClicked();
                return true;
            }
            return false;
        });
        int encoding = sharedPrefs.getInt(Preferences.PREF_ENCODING, 0);
        if (encoding == 0) {
            msgField.setHint(getString(R.string.msgFieldHint) + " (Ascii)");
        } else {
            msgField.setHint(getString(R.string.msgFieldHint) + " (Hex)");
        }
        msgField.setOnClickListener(v -> scrollToBottom());
        noEntries = view.findViewById(R.id.tv_no_entries);
        noEntries.setVisibility(View.GONE);
        btnSend = view.findViewById(R.id.btn_send);
        btnSend.setOnClickListener(v -> onSendClicked());

        if (START) { // Disable UI elements only at the first start
            btnSend.setEnabled(false);
            msgField.setEnabled(false);
            START = false;
            controlGlobally = sharedPrefs.getBoolean(Preferences.PREF_GLOBAL_CONTROL, false);
        }

        // If a bluetooth device does not send an answer back at the beginning to enable UI elements, a Handler will enable
        // the UI elements like the button and the message field after a certain time
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                btnSend.setEnabled(true);
                msgField.setEnabled(true);
            }
        }, 3000);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UARTService.BROADCAST_UART_RX);
        filter.addAction(ConfigDialog.ACTION_CONFIG);
        filter.addAction(ReadWriteDialog.ACTION_READ_WRITE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
        return view;
    }

    /**
     * Logs the terminal by a certain filter option
     *
     * @param filter One of the options in the spinner item
     */
    public void filterLog(String filter) {
        switch (filter) {
            case "DEBUG":
                logFilter = null;
                break;
            case "VERBOSE":
                logFilter = LogContract.Log.LEVEL + "=" + LogContract.Log.Level.INFO + " OR " + LogContract.Log.LEVEL + "=" + LogContract.Log.Level.VERBOSE
                        + " OR " + LogContract.Log.LEVEL + "=" + LogContract.Log.Level.APPLICATION;
                break;
            case "INFO":
                logFilter = LogContract.Log.LEVEL + "=" + LogContract.Log.Level.INFO
                        + " OR " + LogContract.Log.LEVEL + "=" + LogContract.Log.Level.APPLICATION
                        + " OR " + LogContract.Log.LEVEL + "=" + LogContract.Log.Level.WARNING
                        + " OR " + LogContract.Log.LEVEL + "=" + LogContract.Log.Level.ERROR;
                break;
            case "APP":
                logFilter = LogContract.Log.LEVEL + "=" + LogContract.Log.Level.APPLICATION;
                break;
            case "WARNING":
                logFilter = LogContract.Log.LEVEL + "=" + LogContract.Log.Level.WARNING;
                break;
            case "ERROR":
                logFilter = LogContract.Log.LEVEL + "=" + LogContract.Log.Level.ERROR;
                break;
        }
        if (mLogSession != null) {
            getLoaderManager().restartLoader(LOG_REQUEST_ID, null, DeviceInstanceFragment.this);
        }
    }

    public void menuItemClicked(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.deviceMenuItem_config:
                configDialog = new ConfigDialog();
                configDialog.setCancelable(false);
                Bundle bundle = new Bundle();
                bundle.putParcelable(UARTService.EXTRA_DEVICE, mDevice);
                bundle.putSerializable(ConfigGPIO.EXTRA_CONFIG, configGPIO);
                configDialog.setArguments(bundle);
                configDialog.setTargetFragment(this, REQ_CONFIG);
                configDialog.show(getFragmentManager(), "ConfigDialog");
                break;
            case R.id.deviceMenuItem_read_write:
                readWriteDialog = new ReadWriteDialog();
                readWriteDialog.setCancelable(false);
                Bundle bundle2 = new Bundle();
                bundle2.putSerializable(ConfigGPIO.EXTRA_CONFIG, configGPIO);
                readWriteDialog.setArguments(bundle2);
                readWriteDialog.setTargetFragment(this, REQ_READ_WRITE);
                readWriteDialog.show(getFragmentManager(), "ReadWriteDialog");
                break;
            case R.id.deviceMenuItem_read_mtu:
                getMtu();
                break;
            case R.id.deviceMenuItem_req_mtu:
                ReqMtuDialog mtuDialog = new ReqMtuDialog();
                mtuDialog.setTargetFragment(this, REQ_MTU);
                mtuDialog.show(getFragmentManager(), "RequestMtuDialog");
                break;
            case R.id.deviceMenuItem_read_rssi:
                readRssi();
                break;
            case R.id.devicemenuItem_set_phy:
                ReqPhyDialog phyDialog = new ReqPhyDialog();
                phyDialog.setTargetFragment(this, REQ_PHY);
                phyDialog.show(getFragmentManager(), "ReqPhyDialog");
                break;
            case R.id.deviceMenuItem_read_phy:
                readPhy();
                break;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Create the log adapter, initially with null cursor
        mLogAdapter = new UARTLogAdapter(requireContext());
        this.listView.setAdapter(mLogAdapter);
    }

    @Override
    public void onDestroyView() {
        // Destroy LocalLogSession
        if (mLogSession != null && mLogSession instanceof LocalLogSession) {
            LocalLogSession session = (LocalLogSession) mLogSession;
            session.delete();
            Log.d(CustomLogger.TAG, "LocalLogSession deleted");
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public void readRssi() {
        mManager.getRssi().enqueue();
    }

    public void setPhy(int txPhy, int rxPhy, int phyOptions) {
        mManager.setPhy(txPhy, rxPhy, phyOptions).enqueue();
    }

    public void readPhy() {
        mManager.getPhy().enqueue();
    }

    public int getPreferredPhy() {
        return this.preferredPhy;
    }

    public int getPreferredOption() {
        return this.preferredOption;
    }

    public void requestMtu(int mtu) {
        mManager.setMtu(mtu).enqueue();
    }

    public void getMtu() {
        int mtu = mManager.readMtu();
        mManager.log(android.util.Log.INFO, "MTU read: " + mtu);
    }

    /**
     * Sends the writeConfig command
     * @param pins All the pins which should get configured
     */
    public void sendWriteConfigReq(List<GpioPin> pins) {
        if (pins != null && pins.size() > 0) {
            StringBuilder strBuilder = new StringBuilder();
            for (GpioPin gp : pins) {
                String block = configGPIO.buildWriteConfigBlock(gp.getID(), gp.getFunction(), gp.getValue());
                strBuilder.append(block);
            }
            mManager.sendGPIOCommand(ConfigGPIO.AMBER_WRITE_CONFIG_GPIO_REQ, strBuilder.toString());
        }
    }

    /**
     * Sends the readConfig command
     */
    public void sendReadConfigReq() {
        mManager.sendGPIOCommand(ConfigGPIO.AMBER_READ_CONFIG_COMMAND, "");
    }

    /**
     * Sends the writePin command
     * @param pins All the pins which should get written
     */
    public void sendWritePinReq(List<GpioPin> pins) {
        if (pins != null && pins.size() > 0) {
            StringBuilder strBuilder = new StringBuilder();
            for (GpioPin gp : pins) {
                String block = configGPIO.buildWriteReqBlock(gp.getID(), gp.getValue());
                strBuilder.append(block);
            }
            mManager.sendGPIOCommand(ConfigGPIO.AMBER_WRITE_REQ, strBuilder.toString());
        }
    }

    /**
     * Sends the readPin command
     * @param pins
     */
    public void sendReadPinReq(List<GpioPin> pins) {
        if (pins != null && pins.size() > 0) {
            StringBuilder strBuilder = new StringBuilder();
            byte[] ids = new byte[pins.size()];
            for (int i = 0; i < pins.size(); i++) {
                ids[i] = pins.get(i).getID();
            }
            String block = configGPIO.buildReadReqBlock(ids);
            strBuilder.append(block);
            mManager.sendGPIOCommand(ConfigGPIO.AMBER_READ_REQ, strBuilder.toString());
        }
    }

    /**
     * Catches all the results of the Dialogs
     * @param requestCode The operation which is requested
     * @param resultCode The result of the operation
     * @param data Optional data of the result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_CONFIG && resultCode == Activity.RESULT_OK) { // WRITE_CONF_REQ
            List<GpioPin> gpioPins = (List<GpioPin>) data.getSerializableExtra(EXTRA_GPIO_PINS);
            sendWriteConfigReq(gpioPins);
        } else if (requestCode == REQ_CONFIG && resultCode == ConfigDialog.RESULT_READ_OK) { // READ_CONF_REQ
            sendReadConfigReq();
        } else if (requestCode == REQ_READ_WRITE && resultCode == Activity.RESULT_OK) { // WRITE_REQ
            List<GpioPin> gpioPins = (List<GpioPin>) data.getSerializableExtra(EXTRA_GPIO_PINS);
            sendWritePinReq(gpioPins);
        } else if (requestCode == REQ_READ_WRITE && resultCode == ReadWriteDialog.RESULT_READ_OK) { // READ_REQ
            List<GpioPin> gpioPins = (List<GpioPin>) data.getSerializableExtra(EXTRA_GPIO_PINS);
            sendReadPinReq(gpioPins);
        } else if (requestCode == REQ_MTU && resultCode == Activity.RESULT_OK) { // MTU_REQ
            int mtu = data.getIntExtra(EXTRA_MTU, 23);
            requestMtu(mtu);
        } else if (requestCode == REQ_PHY && resultCode == Activity.RESULT_OK) { // PHY_REQ
            preferredPhy = data.getIntExtra(EXTRA_PHY, PhyResult.PHY_LE_1M);
            int txPhy = preferredPhy;
            int rxPhy = preferredPhy;
            preferredOption = data.getIntExtra(EXTRA_PHY_OPTIONS, PhyRequest.PHY_OPTION_NO_PREFERRED);
            setPhy(txPhy, rxPhy, preferredOption);
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        switch (id) {
            case LOG_REQUEST_ID: {
                return new CursorLoader(requireContext(), mLogSession.getSessionEntriesUri(), LOG_PROJECTION, logFilter, null, LogContract.Log.TIME);
            }
        }
        throw new UnsupportedOperationException("Could not create loader with ID " + id);
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<Cursor> loader, final Cursor data) {
        // Here we have to restore the old saved scroll position, or scroll to the bottom if before adding new events it was scrolled to the bottom.
        final ListView list = this.listView;
        final int position = mLogScrollPosition;
        final boolean scrolledToBottom = position == LOG_SCROLLED_TO_BOTTOM || (list.getCount() > 0 && list.getLastVisiblePosition() == list.getCount() - 1);

        mLogAdapter.swapCursor(data);
        if (mLogAdapter.isEmpty()) {
            listView.setVisibility(View.GONE);
            noEntries.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            noEntries.setVisibility(View.GONE);
        }

        if (position > LOG_SCROLL_NULL) {
            list.setSelectionFromTop(position, 0);
        } else {
            if (scrolledToBottom)
                list.setSelection(list.getCount() - 1);
        }
        mLogScrollPosition = LOG_SCROLL_NULL;
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<Cursor> loader) {
        mLogAdapter.swapCursor(null);
    }

    private void scrollToBottom() {
        this.listView.smoothScrollToPosition(mLogAdapter.getCount() - 1);
    }

    /**
     * Controls all the connect devices.
     *
     * @param text The message which shall be send to all devices
     */
    private void controlAllDevices(String text) {
        for (DeviceInstanceFragment tif : TerminalFragment.getFragment().getTerminalInstances()) {
            if (!tif.equals(this)) {
                tif.send(text);
            }
        }
    }

    /**
     * Method which gets called by the send button
     */
    private void onSendClicked() {
        final String text = msgField.getText().toString();
        mManager.send(text);
        if (controlGlobally) {
            controlAllDevices(text);
        }
        msgField.setText(null);
        msgField.requestFocus();
    }

    public void onProfileSelected(String command) {
        mManager.send(command);
        if (controlGlobally) {
            controlAllDevices(command);
        }
    }

    /**
     * Gets only called if {@link #controlGlobally} is set to <code>true</code>
     * @param text
     */
    public void send(String text) {
        mManager.send(text);
        msgField.setText(null);
        msgField.requestFocus();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Preferences.PREF_ENCODING)) { // Ascii or hex
            int encoding = sharedPreferences.getInt(Preferences.PREF_ENCODING, 0);
            if (encoding == 0) {
                msgField.setHint(getString(R.string.msgFieldHint) + " (Ascii)");
            } else {
                msgField.setHint(getString(R.string.msgFieldHint) + " (Hex)");
            }
        } else if (key.equals(Preferences.PREF_GLOBAL_CONTROL)) { // Control all devices
            boolean checked = sharedPreferences.getBoolean(Preferences.PREF_GLOBAL_CONTROL, false);
            if (checked) {
                controlGlobally = true;
            } else {
                controlGlobally = false;
            }
        }
    }
}
