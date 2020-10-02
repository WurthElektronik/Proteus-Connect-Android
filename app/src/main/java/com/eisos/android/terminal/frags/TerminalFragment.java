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

import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.eisos.android.terminal.MainActivity;
import com.eisos.android.R;
import com.eisos.android.terminal.adapter.TerminalPagerAdapter;
import com.eisos.android.terminal.bluetooth.services.BleMulticonnectProfileService;
import com.eisos.android.terminal.bluetooth.services.UARTService;
import com.eisos.android.terminal.customLayout.ScanListItem;
import com.eisos.android.terminal.dialogs.EncodingDialog;
import com.eisos.android.terminal.utils.Preferences;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

@SuppressWarnings("unchecked")
public class TerminalFragment extends Fragment {

    public static final String TAG = "TerminalFragment";
    private static TerminalFragment terminalFragment;
    private ViewPager viewPager;
    private TerminalPagerAdapter pagerAdapter;
    private FragmentManager fmManager;
    private TabLayout tlDevices;
    private TextView tvNoData;
    private Toolbar toolbar;
    private BleMulticonnectProfileService.LocalBinder mBinder;
    private ArrayList<DeviceInstanceFragment> mFragments;
    private final static int PAGE_LIMIT = ScanFragment.MAX_ALLOWED_CONS;
    private SharedPreferences sharedPrefs;
    private boolean isPaused = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        terminalFragment = this;
        mFragments = new ArrayList<>();
        fmManager = getActivity().getSupportFragmentManager();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        pagerAdapter = new TerminalPagerAdapter(mFragments, fmManager);
        viewPager = view.findViewById(R.id.viewPager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tlDevices));
        viewPager.setAdapter(pagerAdapter);
        viewPager.setVisibility(View.GONE);
        tlDevices = view.findViewById(R.id.tL_devices);
        tlDevices.setSelectedTabIndicatorColor(getResources().getColor(R.color.colorPrimary));
        tlDevices.setupWithViewPager(viewPager);
        tlDevices.setVisibility(View.GONE);
        tvNoData = view.findViewById(R.id.tv_noData);
        toolbar = getActivity().findViewById(R.id.customToolbar);
        return view;
    }

    public ArrayList<DeviceInstanceFragment> getTerminalInstances() {
        return this.mFragments;
    }

    public static TerminalFragment getFragment() {
        return terminalFragment;
    }

    /**
     * Adds a new Fragment to the PageAdapter. The method gets called out of the service
     * {@link UARTService#onDeviceConnected(BluetoothDevice)}
     * when a device has connected
     *
     * @param device The bluetooth device which you want to connect to
     */
    public void onDeviceConnected(BluetoothDevice device) {
        if (!isPaused) {
            // If called in onPause() -> App will crash
            MainActivity.getActivity().getBtmNavView().setSelectedItemId(R.id.item_terminal);
        }
        tlDevices.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.VISIBLE);
        tvNoData.setVisibility(View.GONE);
        toolbar.getMenu().findItem(R.id.menu_disconnect).setVisible(true);
        boolean checked = sharedPrefs.getBoolean(Preferences.PREF_GLOBAL_CONTROL, false);
        if (checked) {
            toolbar.getMenu().findItem(R.id.menu_sendGlobally).setChecked(true);
        }
        DeviceInstanceFragment deviceInstance = DeviceInstanceFragment.newInstance(device, mBinder);
        mFragments.add(deviceInstance);
        pagerAdapter.notifyDataSetChanged();
        viewPager.setCurrentItem(mFragments.indexOf(deviceInstance), true);

        ScanFragment.getFragment().onDeviceConnected(device);
        if (isPaused) {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getActivity().getApplicationContext(),
                    device.getName() + " " + getString(R.string.deviceReconnected), Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Disconnects the device selected in the tab
     */
    public void disconnect(BluetoothDevice device) {
        if (mBinder != null) {
            mBinder.disconnect(device);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        isPaused = false;
    }

    public void setService(BleMulticonnectProfileService.LocalBinder service) {
        this.mBinder = service;
    }

    /**
     * Gets called out of the service
     * {@link UARTService#onDeviceDisconnected(BluetoothDevice, int)}
     * when a device has disconnected
     */
    public void onDeviceDisconnected(BluetoothDevice device) {
        ScanFragment.getFragment().onDeviceDisconnected(device);
        if (pagerAdapter.getCount() > 0) {
            for (DeviceInstanceFragment f : mFragments) {
                if (f.getDevice().equals(device)) {
                    mFragments.remove(f);
                    break;
                }
            }
            pagerAdapter = new TerminalPagerAdapter(mFragments, fmManager);
            viewPager.setAdapter(pagerAdapter);
        }

        if (pagerAdapter.getCount() == 0) {
            tlDevices.setVisibility(View.GONE);
            viewPager.setVisibility(View.GONE);
            tvNoData.setVisibility(View.VISIBLE);
            toolbar.getMenu().setGroupVisible(R.id.menu_group_one, false);
            if (!isPaused) {
                // If called in onPause() -> App will crash
                MainActivity.getActivity().getBtmNavView().setSelectedItemId(R.id.item_scan);
            }
        }

    }

    /**
     * Shows a dialog where the user can choose to send
     * all entered commands to all connected device or not
     */
    public void controlAllDevices() {
        boolean checked = toolbar.getMenu().findItem(R.id.menu_sendGlobally).isChecked();
        if (checked) {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putBoolean(Preferences.PREF_GLOBAL_CONTROL, false);
            editor.commit();
            // If not all devices shall be controlled simultaneously
            // off screen limit is 1 (slightly less resources get used)
            viewPager.setOffscreenPageLimit(1);
            toolbar.getMenu().findItem(R.id.menu_sendGlobally).setChecked(false);
        } else {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putBoolean(Preferences.PREF_GLOBAL_CONTROL, true);
            editor.commit();
            // Needs to be set to be able to control all views
            // When offscreen limit to low --> devices out of
            // page limit can't be controlled globally
            viewPager.setOffscreenPageLimit(PAGE_LIMIT);
            toolbar.getMenu().findItem(R.id.menu_sendGlobally).setChecked(true);
        }
    }

    /**
     * Shows a dialog where you can switch between Hex- or Ascii-Encoding
     */
    public void showDialog() {
        EncodingDialog dialog = new EncodingDialog();
        dialog.show(fmManager, "dialog");
    }

    public int getSelectedTabPosition() {
        return viewPager.getCurrentItem();
    }

    /**
     * Searches the right index position of the selected tab
     * in the list of the scanned items
     *
     * @return The index of the item in the scanning list
     */
    public int getTabIndexOfScanListItem() {
        String title = tlDevices.getTabAt(getSelectedTabPosition()).getText().toString();
        ArrayList<ScanListItem> scannedItems = ScanFragment.getFragment().getScannedItems();
        for (int i = 0; i < scannedItems.size(); i++) {
            if (title.contains(scannedItems.get(i).getDeviceAddress())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Selects a tab
     *
     * @param device The device whose tab shall be selected
     */
    public void selectTab(BluetoothDevice device) {
        String tmp = device.getName() + "\n" + device.getAddress();
        for (int i = 0; i < tlDevices.getTabCount(); i++) {
            if (tlDevices.getTabAt(i).getText().equals(tmp)) {
                tlDevices.getTabAt(i).select();
                break;
            }
        }
    }
}
