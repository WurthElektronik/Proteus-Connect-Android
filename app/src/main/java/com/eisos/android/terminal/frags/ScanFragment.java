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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eisos.android.R;
import com.eisos.android.terminal.MainActivity;
import com.eisos.android.terminal.adapter.ItemAdapter;
import com.eisos.android.terminal.bluetooth.UARTLocalLogContentProvider;
import com.eisos.android.terminal.bluetooth.services.BleMulticonnectProfileService;
import com.eisos.android.terminal.bluetooth.services.UARTService;
import com.eisos.android.terminal.customLayout.ScanListItem;
import com.eisos.android.terminal.database.favourites.FavouriteDevice;
import com.eisos.android.terminal.database.favourites.FavouriteViewModel;
import com.eisos.android.terminal.dialogs.ReqPhyStartDialog;
import com.eisos.android.terminal.frags.interfaces.OnDeviceSelectedListener;
import com.eisos.android.terminal.utils.CustomLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import no.nordicsemi.android.ble.PhyRequest;
import no.nordicsemi.android.ble.response.PhyResult;
import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LocalLogSession;
import no.nordicsemi.android.log.Logger;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class ScanFragment<E extends BleMulticonnectProfileService.LocalBinder> extends Fragment implements OnDeviceSelectedListener {

    public static final String TAG = "ScanFragment";
    private SwipeRefreshLayout srLayout;
    private RecyclerView recyclerView;
    private Spinner sortSpinner;
    private ItemAdapter itemAdapter;
    private BluetoothLeScannerCompat bleScanner;
    private TextView tvScan;
    private ProgressBar progressBar;
    private final static ParcelUuid mUuid = ParcelUuid.fromString("6E400001-C352-11E5-953D-0002A5D5C51B");
    private static ScanFragment scanFragment;
    private ScanCallback scanCallback, scanCallback2;
    private ScanSettings settings, settings2;
    private boolean isScanning = false;
    protected static final int REQUEST_ENABLE_BT = 1;
    private static String SORT_FILTER;
    private static final String sortByDefault = "Default";
    private static final String sortByName = "Sort by Name";
    private static final String sortByAddress = "Sort by Address";
    private static final String sortByRSSI = "Sort by RSSI";
    private Intent service;
    private E mBinder;
    private List<BluetoothDevice> mManagedDevices;
    public final static int MAX_ALLOWED_CONS = 5;
    public static final int REQ_PHY = 11;
    public static final String EXTRA_PHY = "com.eisos.android.terminal.bluetooth.EXTRA_PHY";
    public static final String EXTRA_PHY_OPTIONS = "com.eisos.android.terminal.bluetooth.EXTRA_PHY_OPTIONS";
    private List<ScanListItem> connectedDevices;
    private FavouriteViewModel mFavModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        mManagedDevices = new ArrayList<>();
        itemAdapter = new ItemAdapter(getActivity().getApplicationContext(), this);
        connectedDevices = new ArrayList<>();
        SORT_FILTER = sortByDefault;
        mFavModel = new ViewModelProvider(this).get(FavouriteViewModel.class);
        initView(view);
        // Check if device supports Ble
        hasDeviceBLE();
        /*
         * In comparison to BleProfileServiceReadyActivity this activity always starts the service when started.
         * Connecting to a device is done by calling mBinder.connect(BluetoothDevice) method, not startService(...) like there.
         */
        // Start Service
        service = new Intent(getActivity(), UARTService.class);
        getActivity().startService(service);
        bindActivityToService(getActivity());
        scan();
        return view;
    }

    public static ScanFragment getFragment() {
        return scanFragment;
    }

    public void bindActivityToService(Activity activity) {
        if (service != null) {
            activity.bindService(service, mServiceConnection, 0);
        }
    }

    public void unbindActivityFromService(Activity activity) {
        if (service != null) {
            activity.unbindService(mServiceConnection);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        bindActivityToService(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
        stopScan();
        unbindActivityFromService(getActivity());
    }

    /**
     * Listens to the connection state of a device.
     * If device connects successfully the {@link #onDeviceConnected} method
     * will be called
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            final E bleService = mBinder = (E) service;
            TerminalFragment.getFragment().setService(mBinder);
            mManagedDevices.addAll(bleService.getManagedDevices());

            // and notify user if device is connected
            for (final BluetoothDevice device : mManagedDevices) {
                if (bleService.isConnected(device)) {
                    onDeviceConnected(device);
                }
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mBinder = null;
            Log.d(CustomLogger.TAG, "Service disconnected");
        }
    };

    protected void showBLEDialog() {
        final Intent enableBLE = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBLE, REQUEST_ENABLE_BT);
    }

    @SuppressLint("MissingPermission")
    protected boolean isBLEEnabled() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    protected void hasDeviceBLE() {
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            new Handler().post(() -> Toast.makeText(getActivity().getApplicationContext(),
                    getString(R.string.no_ble), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == getActivity().RESULT_OK) {
                if (service != null) {
                    getActivity().stopService(service);
                }
                hasDeviceBLE();
                /*
                 * In comparison to BleProfileServiceReadyActivity this activity always starts the service when started.
                 * Connecting to a device is done by calling mBinder.connect(BluetoothDevice) method, not startService(...) like there.
                 */
                // Start Service
                service = new Intent(getActivity(), UARTService.class);
                getActivity().startService(service);
                bindActivityToService(getActivity());
                scan();
            } else {
                // If bluetooth request has been denied -> show info message
                new Handler().post(() -> Toast.makeText(getActivity().getApplicationContext(), R.string.bluetoothNotEnabled, Toast.LENGTH_LONG).show());
            }
        } else if (requestCode == REQ_PHY) {
            if (resultCode == getActivity().RESULT_OK) {
                int preferredPhy = data.getIntExtra(EXTRA_PHY, PhyResult.PHY_LE_1M);
                int preferredOption = data.getIntExtra(EXTRA_PHY_OPTIONS, PhyRequest.PHY_OPTION_NO_PREFERRED);
                ScanListItem item = (ScanListItem) data.getSerializableExtra(ReqPhyStartDialog.EXTRA_DEVICE);
                mBinder.setDeviceSettings(item.getDevice(), false, preferredPhy, preferredOption);
                connectDevice(item);
            }
        }
    }

    private void initView(View view) {
        srLayout = view.findViewById(R.id.refreshLayout);
        srLayout.setColorSchemeColors(getActivity().getResources().getColor(R.color.colorPrimary));
        srLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                scan();
                srLayout.setRefreshing(false);
            }
        });

        recyclerView = view.findViewById(R.id.recyclerView);
        // Disable animation for notifyItemChanged()
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        recyclerView.setAdapter(itemAdapter);

        scanFragment = this;
        tvScan = view.findViewById(R.id.tv_scan);
        tvScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScanning) {
                    stopScan();
                } else {
                    scan();
                }
            }
        });

        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        sortSpinner = view.findViewById(R.id.spinner_sort);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sortOption = (String) parent.getItemAtPosition(position);
                sortList(sortOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * Starts scan and adds items to ArrayList and Layout
     */
    public void scan() {
        if (isScanning) {
            stopScan();
        }

        // Clear list with items
        itemAdapter.clearDevices();
        addConnectedDevices();

        // Check if bluetooth is enabled
        if (!isBLEEnabled()) {
            showBLEDialog();
            return;
        }

        // Request permission if not already granted
        String[] permissions = MainActivity.getActivity().getPermissions();
        if (ContextCompat.checkSelfPermission(getActivity(), permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), permissions, 1);
        }
        progressBar.setVisibility(View.VISIBLE);
        addFavouriteDevices();
        tvScan.setText(R.string.stopScanning);
        initScan();
    }

    private void addFavouriteDevices() {
        List<FavouriteDevice> devices = mFavModel.getAll();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bAdapter = bluetoothManager.getAdapter();

        if (devices != null) {
            for (FavouriteDevice device : devices) {
                BluetoothDevice bleDevice = bAdapter.getRemoteDevice(device.getDeviceAddress());
                // Check if device is connected
                // if true than add old ScanListItem to list
                if(connectedDevices.size() > 0) {
                    boolean found = false;
                    for (ScanListItem item : connectedDevices) {
                        if (item.getDeviceAddress().equals(bleDevice.getAddress())) {
                            itemAdapter.addListItem(item);
                            found = true;
                            break;
                        }
                    }
                    if(!found) {
                        ScanListItem listItem = new ScanListItem(getContext(), bleDevice);
                        listItem.setDeviceName(device.getDeviceName());
                        itemAdapter.addListItem(listItem);
                    }
                } else {
                    ScanListItem listItem = new ScanListItem(getContext(), bleDevice);
                    listItem.setDeviceName(device.getDeviceName());
                    itemAdapter.addListItem(listItem);
                }
            }
        }
    }

    public void addDeviceToFavourites(ScanListItem item) {
        List<FavouriteDevice> devices = mFavModel.getAll();
        boolean inList = false;
        for(FavouriteDevice device : devices) {
            if (device.getDeviceAddress().equals(item.getDeviceAddress())) {
                inList = true;
                break;
            }
        }

        if(!inList) {
            mFavModel.insert(new FavouriteDevice(item.getDeviceName(), item.getDeviceAddress()));
            mFavModel.updateList();
            Log.d(CustomLogger.TAG, "Device " + item.getDeviceAddress() + " added to favourites");
        }
    }

    public void deleteDeviceFromFavourites(ScanListItem item) {
        List<FavouriteDevice> devices = mFavModel.getAll();
        FavouriteDevice deviceDelete = null;
        for(FavouriteDevice device : devices) {
            if (device.getDeviceAddress().equals(item.getDeviceAddress())) {
                deviceDelete = device;
                break;
            }
        }

        if(deviceDelete != null) {
            mFavModel.delete(deviceDelete);
            mFavModel.updateList();
            Log.d(CustomLogger.TAG, "Device " + deviceDelete.getDeviceAddress() + " deleted from favourites");
        }
    }

    public boolean isDeviceFavourite(ScanListItem item) {
        List<FavouriteDevice> devices = mFavModel.getAll();
        for(FavouriteDevice device : devices) {
            if(device.getDeviceAddress().equals(item.getDeviceAddress())) {
                return true;
            }
        }
        return false;
    }

    private void addConnectedDevices() {
        if (connectedDevices.size() > 0) {
            for (ScanListItem item : connectedDevices) {
                if (!isDeviceFavourite(item)) {
                    itemAdapter.addListItem(item);
                }
            }
        }
    }

    /**
     * Initializes and starts the scan
     */
    private void initScan() {

        settings = new ScanSettings.Builder()
                .setLegacy(false)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // Receive all signals
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setReportDelay(1000L) // Show results after 1 sec
                .setUseHardwareBatchingIfSupported(false)
                .build();
        final List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(mUuid).build());
        scanCallback = new ScanCallback() {

            @Override
            public void onBatchScanResults(@NonNull List<ScanResult> results) {
                addDevices(results);
            }
        };

        settings2 = new ScanSettings.Builder()
                .setLegacy(false)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_MATCH_LOST) // No signal anymore
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setReportDelay(0L)
                .setUseHardwareBatchingIfSupported(false)
                .build();
        scanCallback2 = new ScanCallback() {

            @Override
            @SuppressLint("MissingPermission")
            public void onScanResult(int callbackType, @NonNull ScanResult result) {
                final ScanListItem scanListItem = new ScanListItem(getActivity(), result.getDevice());
                deviceNotAvailable(scanListItem);
            }

        };
        bleScanner = BluetoothLeScannerCompat.getScanner();
        bleScanner.startScan(filters, settings, scanCallback);
        bleScanner.startScan(filters, settings2, scanCallback2);
        isScanning = true;
    }

    public void stopScan() {
        tvScan.setText(R.string.scan);
        progressBar.setVisibility(View.GONE);
        isScanning = false;
        if (scanCallback != null && scanCallback2 != null) {
            bleScanner.stopScan(scanCallback);
            bleScanner.stopScan(scanCallback2);
            itemAdapter.setAllItemsNotUpdating(true);
            scanCallback = null;
            scanCallback2 = null;
        }
    }

    /**
     * Adds the scanned devices to the ArrayList.
     * If a device is already in the list, update the RSSI value
     *
     * @param results the ScanResults which shall be added
     */
    private void addDevices(List<ScanResult> results) {
        for (ScanResult sr : results) {
            boolean inList = false;
            ScanListItem device = new ScanListItem(getActivity().getApplicationContext(), sr.getDevice());
            for (ScanListItem item : itemAdapter.getItems()) {
                if (item.getDeviceAddress().equals(device.getDeviceAddress())) {
                    item.setItemUpdateStatus(true);
                    item.setRSSI(sr.getRssi());
                    inList = true;
                    itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(item));
                    break;
                }
            }

            if (!inList) {
                device.setRSSI(sr.getRssi());
                itemAdapter.addListItem(device);
            }
        }

        // Sort the ArrayList dependent of the sort filter
        if (SORT_FILTER != sortByDefault) {
            sortList(SORT_FILTER);
        }
    }

    /**
     * Disables a device on the displayed ListView
     *
     * @param device The DeviceItem which shall get deleted
     */
    private void deviceNotAvailable(ScanListItem device) {
        for (ScanListItem item : itemAdapter.getItems()) {
            if (item.getDeviceAddress().equals(device.getDeviceAddress())) {
                item.setSignalOutOfRange();
                item.setItemUpdateStatus(false);
                itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(item));
                return;
            }
        }
    }

    /**
     * Builds a connection to a com.eisos.android.ble device
     *
     * @param item The Device you want to connect to
     */
    public void connectDevice(ScanListItem item) {
        stopScan();
        if (item.getConnectionState() == ScanListItem.CONNECTED) {
            MainActivity.getActivity().getBtmNavView().setSelectedItemId(R.id.item_terminal);
            TerminalFragment.getFragment().selectTab(item.getDevice());
            return;
        } else if (item.getConnectionState() == ScanListItem.CONNECTING) {
            return;
        }

        final ScanListItem device = item;
        device.setConnectionState(ScanListItem.CONNECTING);
        itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(device));
        if (mBinder.getManagedDevices().size() < MAX_ALLOWED_CONS) {
            onDeviceSelected(item.getDevice());
        } else {
            new Handler().post(() -> Toast.makeText(getActivity().getApplicationContext(), "You can max. connect to " + MAX_ALLOWED_CONS + " devices", Toast.LENGTH_SHORT).show());
            device.setConnectionState(ScanListItem.DISCONNECTED);
            itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(device));
        }
    }

    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        ILogSession logSession = Logger.newSession(getActivity().getApplicationContext(), null, device.getAddress(), device.getName());
        // If nRF Logger is not installed we may want to use local logger
        if (logSession == null) {
            logSession = LocalLogSession.newSession(getActivity().getApplicationContext(), UARTLocalLogContentProvider.AUTHORITY_URI, device.getAddress(), device.getName());
            Log.d(CustomLogger.TAG, "LocalLogSession created");
        }
        mBinder.connect(device, logSession);
    }

    public void onAutoConnectClicked(ScanListItem item) {
        mBinder.setDeviceSettings(item.getDevice(), true);
        connectDevice(item);
    }

    public void onPreferredPhyClicked(ScanListItem item) {
        ReqPhyStartDialog dialog = new ReqPhyStartDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable(ReqPhyStartDialog.EXTRA_DEVICE, item);
        dialog.setArguments(bundle);
        dialog.setTargetFragment(this, REQ_PHY);
        dialog.show(getFragmentManager(), "ReqPhyStartDialog");
    }

    /**
     * @return A list with the all the items in the scanning list, which are connected
     */
    public ArrayList<ScanListItem> getConnectedListItems() {
        ArrayList<ScanListItem> connectedItems = new ArrayList<>();
        for (ScanListItem item : itemAdapter.getItems()) {
            if (item.getConnectionState() == ScanListItem.CONNECTED) {
                connectedItems.add(item);
            }
        }
        return connectedItems;
    }

    /**
     * @return A list with all connected devices
     */
    public List<BluetoothDevice> getConnectedDevices() {
        if (mBinder != null) {
            return mBinder.getConnectedDevices();
        }
        return null;
    }

    /**
     * Called when a device has been connected
     *
     * @param device The Bluetooth device connected to
     */
    public void onDeviceConnected(BluetoothDevice device) {
        MainActivity.getActivity().getMenu().setGroupVisible(R.id.menu_group_one, true);
        setConnectionState(device.getAddress(), ScanListItem.CONNECTED);
        for (ScanListItem item : itemAdapter.getItems()) {
            if (item.getDeviceAddress().equals(device.getAddress())) {
                connectedDevices.add(item);
                item.setMenuVisibility(false);
            }
        }
    }

    /**
     * Called when a device has been disconnected
     *
     * @param device The Bluetooth device which has been disconnected
     */
    public void onDeviceDisconnected(BluetoothDevice device) {
        for (ScanListItem item : itemAdapter.getItems()) {
            if (item.getDeviceAddress().equals(device.getAddress())) {
                connectedDevices.remove(item);
                item.setConnectionState(ScanListItem.DISCONNECTED);
                item.setMenuVisibility(true);
                itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(item));
                break;
            }
        }
    }

    public E getService() {
        return this.mBinder;
    }

    public ArrayList<ScanListItem> getScannedItems() {
        return this.itemAdapter.getItems();
    }

    /**
     * Set the current state of a device in the list
     *
     * @param state The current state of the device in the list
     */
    public void setConnectionState(String address, int state) {
        for (ScanListItem item : itemAdapter.getItems()) {
            if (item.getDeviceAddress().equals(address)) {
                item.setConnectionState(state);
                new Handler(Looper.getMainLooper()).post(() ->
                        itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(item)));
            }
        }
    }

    /**
     * Sorts the ArrayList with the detected devices
     *
     * @param sortOption The sorting parameter by which the ArrayList will be sorted
     */
    private void sortList(String sortOption) {
        switch (sortOption) {
            case sortByDefault:
                sortByDefault();
                break;
            case sortByName:
                sortByName();
                break;
            case sortByAddress:
                sortByAddress();
                break;
            case sortByRSSI:
                sortByRssi();
                break;
        }
    }

    /**
     * Sorts the scanning list by the item id
     */
    private void sortByDefault() {
        SORT_FILTER = sortByDefault;
        Collections.sort(itemAdapter.getItems(), new Comparator<ScanListItem>() {
            @Override
            public int compare(ScanListItem o1, ScanListItem o2) {
                return String.valueOf(o1.getID()).compareTo(String.valueOf(o2.getID()));
            }
        });
        itemAdapter.notifyDataSetChanged();
    }

    /**
     * Sorts the scanning list by the item name
     */
    private void sortByName() {
        SORT_FILTER = sortByName;
        Collections.sort(itemAdapter.getItems(), new Comparator<ScanListItem>() {
            @Override
            public int compare(ScanListItem o1, ScanListItem o2) {
                return o1.getDeviceName().compareTo(o2.getDeviceName());
            }
        });
        itemAdapter.notifyDataSetChanged();
    }

    /**
     * Sorts the scanning list by the item address
     */
    private void sortByAddress() {
        SORT_FILTER = sortByAddress;
        Collections.sort(itemAdapter.getItems(), new Comparator<ScanListItem>() {
            @Override
            public int compare(ScanListItem o1, ScanListItem o2) {
                return o1.getDeviceAddress().compareTo(o2.getDeviceAddress());
            }
        });
        itemAdapter.notifyDataSetChanged();
    }

    /**
     * Sorts the scanning list by the rssi value
     */
    private void sortByRssi() {
        SORT_FILTER = sortByRSSI;
        Collections.sort(itemAdapter.getItems(), new Comparator<ScanListItem>() {
            @Override
            public int compare(ScanListItem o1, ScanListItem o2) {
                return String.valueOf(o1.getRssi()).compareTo(String.valueOf(o2.getRssi()));
            }
        });
        itemAdapter.notifyDataSetChanged();
    }
}
