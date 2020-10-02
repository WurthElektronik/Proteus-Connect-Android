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
package com.eisos.android.terminal;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.eisos.android.R;
import com.eisos.android.terminal.bluetooth.services.BleMulticonnectProfileService;
import com.eisos.android.terminal.bluetooth.services.UARTService;
import com.eisos.android.terminal.customLayout.ScanListItem;
import com.eisos.android.terminal.frags.InfoFragment;
import com.eisos.android.terminal.frags.ScanFragment;
import com.eisos.android.terminal.frags.TerminalFragment;
import com.eisos.android.terminal.profiles.ProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private long mBackPressed;
    private static final int TIME_INTERVAL = 1000;
    private static Toast mExitToast;
    private BottomNavigationView btmNavView;
    private FragmentManager fm;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private LinearLayout toolbarLogo;
    private InfoFragment infoFragment;
    private ScanFragment scanFragment;
    private TerminalFragment terminalFragment;
    private Fragment activeFrag;
    private static MainActivity activity;
    private Menu menu;
    private String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.FOREGROUND_SERVICE};
    private static final int REQUEST_CODE = 1;
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_main);

        btmNavView = findViewById(R.id.btm_nav);
        toolbar = findViewById(R.id.customToolbar);
        toolbar.getOverflowIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        toolbar.setTitle("");
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        setSupportActionBar(toolbar);
        toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.searchToolbarTitle);
        toolbarLogo = toolbar.findViewById(R.id.toolbar_logo);
        toolbarLogo.setVisibility(View.GONE);

        infoFragment = new InfoFragment();
        scanFragment = new ScanFragment();
        terminalFragment = new TerminalFragment();

        fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.fragContainer, infoFragment, InfoFragment.TAG).hide(infoFragment);
        ft.add(R.id.fragContainer, scanFragment, ScanFragment.TAG);
        ft.add(R.id.fragContainer, terminalFragment, TerminalFragment.TAG).hide(terminalFragment);
        ft.commit();
        activeFrag = scanFragment;

        // set Listener of navigation bar
        btmNavView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.item_info:
                        stopScan();
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.hide(activeFrag);
                        ft.show(infoFragment);
                        ft.commit();
                        activeFrag = infoFragment;

                        toolbarLogo.setVisibility(View.VISIBLE);
                        toolbarTitle.setVisibility(View.GONE);
                        toolbar.getMenu().setGroupVisible(R.id.menu_group_one, false);
                        return true;
                    case R.id.item_scan:
                        FragmentTransaction ft2 = fm.beginTransaction();
                        ft2.hide(activeFrag);
                        ft2.show(scanFragment);
                        ft2.commit();
                        activeFrag = scanFragment;

                        toolbarLogo.setVisibility(View.GONE);
                        toolbarTitle.setVisibility(View.VISIBLE);
                        toolbar.getMenu().setGroupVisible(R.id.menu_group_one, false);
                        return true;
                    case R.id.item_terminal:
                        stopScan();
                        FragmentTransaction ft3 = fm.beginTransaction();
                        ft3.hide(activeFrag);
                        ft3.show(terminalFragment);
                        ft3.commit();
                        activeFrag = terminalFragment;

                        toolbarTitle.setVisibility(View.GONE);
                        toolbarLogo.setVisibility(View.VISIBLE);
                        if (scanFragment.getConnectedListItems().size() > 0) {
                            toolbar.getMenu().setGroupVisible(R.id.menu_group_one, true);
                        }
                        return true;
                }
                return false;
            }
        });
        showContent();
    }

    public String[] getPermissions() {
        return this.permissions;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        this.menu = menu;
        MenuItem disconnect = menu.findItem(R.id.menu_disconnect);
        // Load string res and change color to white
        disconnect.setTitle(Html.fromHtml("<font color='#FFFFFF'>" + getString(R.string.menuDisconnect) + "</font>"));
        menu.setGroupVisible(R.id.menu_group_one, false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_disconnect:
                ScanListItem listItem = ((ScanListItem) scanFragment.getScannedItems().get(terminalFragment.getTabIndexOfScanListItem()));
                BluetoothDevice device = listItem.getDevice();
                terminalFragment.disconnect(device);
                break;
            case R.id.menu_profiles:
                Intent intent = new Intent(MainActivity.getActivity(), ProfileActivity.class);
                startActivity(intent);
                break;
            case R.id.menu_encoding:
                terminalFragment.showDialog();
                break;
            case R.id.menu_sendGlobally:
                terminalFragment.controlAllDevices();
                break;
        }
        return true;
    }

    public BottomNavigationView getBtmNavView() {
        return this.btmNavView;
    }

    public static MainActivity getActivity() {
        return activity;
    }

    public Menu getMenu() {
        return this.menu;
    }

    /**
     * Stops the ble scan in {@link ScanFragment#stopScan()}
     */
    public void stopScan() {
        scanFragment.stopScan();
    }

    private void showContent() {
        // select scan menu
        btmNavView.setSelectedItemId(R.id.item_scan);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    scanFragment.scan();
                    if (snackbar != null) {
                        snackbar.dismiss();
                    }
                } else {
                    scanFragment.stopScan();
                    if(snackbar == null) {
                        snackbar = Snackbar.make(findViewById(R.id.fragContainer), R.string.permissionNotGranted, Snackbar.LENGTH_INDEFINITE);
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbar.getView().getLayoutParams();
                        params.gravity = Gravity.TOP;
                        params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
                        params.width = FrameLayout.LayoutParams.MATCH_PARENT;
                        snackbar.getView().setLayoutParams(params);
                        snackbar.setAction(R.string.settingsTitle, this::openSettings);
                        snackbar.setActionTextColor(getResources().getColor(android.R.color.holo_orange_light));
                        snackbar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    }
                    snackbar.show();
                }
                return;
            }

        }
    }

    /**
     * Opens up the settings menu of the app. Here it is possible to
     * grant the location permission
     * @param v The current view
     */
    public void openSettings(View v) {
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + activity.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            if (mExitToast != null) {
                mExitToast.cancel();
            }
            finish();
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            mExitToast = Toast.makeText(getApplicationContext(), R.string.doubleTabToExit, Toast.LENGTH_SHORT);
            mExitToast.show();
        }
        mBackPressed = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        // Disconnect all connected devices
        List<BluetoothDevice> scanList = scanFragment.getConnectedDevices();
        if (scanList != null && scanList.size() > 0) {
            BleMulticonnectProfileService.LocalBinder service = scanFragment.getService();
            for (BluetoothDevice device : scanList) {
                if (service.isConnected(device)) {
                    service.disconnect(device);
                }
            }
        }
        Intent intent = new Intent(MainActivity.this, UARTService.class);
        stopService(intent);
        super.onDestroy();
    }
}
