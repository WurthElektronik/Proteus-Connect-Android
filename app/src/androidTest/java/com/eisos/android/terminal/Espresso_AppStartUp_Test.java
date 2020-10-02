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

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.eisos.android.R;
import com.eisos.android.terminal.SplashActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.eisos.android.terminal.MainActivity.getActivity;
import static org.hamcrest.Matchers.not;

/**
 * Delete storage of app before running this test!
 * Otherwise there will be no PolicyActivity.
 *
 * Android system language has to be set to german (because of system requests)
 * Possibly add string values which could also be correct (depends on device).
 *
 * Most of tests can only be performed on real device!
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class Espresso_AppStartUp_Test {

    @Rule
    public ActivityScenarioRule<SplashActivity> rule = new ActivityScenarioRule<>(SplashActivity.class);

    /**
     * Test for emulator (no bluetooth request)
     */
    @Test
    public void switchToMainActivityEmulator() {
        // Waiting for SplashActivity to end
        sleep(1000);
        // PolicyActivity
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        UiObject clickAccept = device.findObject(new UiSelector().text("ACCEPT"));
        if (clickAccept.exists()) {
            try {
                clickAccept.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        }
        sleep(1000);
        // Check if toast is displayed
        onView(ViewMatchers.withText(R.string.bluetoothNotEnabled)).inRoot(withDecorView(not(getActivity().getWindow().getDecorView()))) .check(matches(isDisplayed()));
        sleep(5000);
    }

    /**
     * App should be started with bluetooth disabled and location permission not granted
     */
    @Test
    public void switchToMainActivity_allow_everything() {
        // Waiting for SplashActivity to end
        sleep(1000);
        // PolicyActivity
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        UiObject clickAccept = device.findObject(new UiSelector().text("ACCEPT"));
        if (clickAccept.exists()) {
            try {
                clickAccept.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        }
        sleep(500);
        // Bluetooth request
        UiObject allowBluetooth = device.findObject(new UiSelector().text("ZULASSEN"));
        UiObject allowBluetoothV2 = device.findObject(new UiSelector().text("JA"));
        if (allowBluetooth.exists()) {
            try {
                allowBluetooth.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        } else if (allowBluetoothV2.exists()) {
            try {
                allowBluetoothV2.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        }
        sleep(4000);
        // Location permission request
        UiObject allowPermissions = device.findObject(new UiSelector().text("ZULASSEN"));
        if (allowPermissions.exists()) {
            try {
                allowPermissions.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        }
        sleep(1000);
        // Check if TextView text
        onView(withText(R.string.stopScanning)).check(matches(isDisplayed()));
        sleep(5000);
    }

    /**
     * App should be started with bluetooth disabled
     */
    @Test
    public void switchToMainActivity_deny_bluetooth() {
        // Waiting for SplashActivity to end
        sleep(1000);
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        // PolicyActivity
        UiObject clickAccept = device.findObject(new UiSelector().text("ACCEPT"));
        if (clickAccept.exists()) {
            try {
                clickAccept.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        }
        sleep(500);
        // Bluetooth request
        UiObject allowBluetooth = device.findObject(new UiSelector().text("ABLEHNEN"));
        UiObject allowBluetoothV2 = device.findObject(new UiSelector().text("NEIN"));
        if (allowBluetooth.exists()) {
            try {
                allowBluetooth.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        } else if (allowBluetoothV2.exists()) {
            try {
                allowBluetoothV2.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        }
        sleep(1000);
        // Check if toast is displayed
        onView(withText(R.string.bluetoothNotEnabled)).inRoot(withDecorView(not(getActivity().getWindow().getDecorView()))) .check(matches(isDisplayed()));
        sleep(5000);
    }


    /**
     * App should have bluetooth disabled and location permission not granted
     */
    @Test
    public void switchToMainActivity_deny_location() {
        // Waiting for SplashActivity to end
        sleep(1000);
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        // PolicyActivity
        UiObject clickAccept = device.findObject(new UiSelector().text("ACCEPT"));
        if (clickAccept.exists()) {
            try {
                clickAccept.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        }
        sleep(500);
        // Bluetooth request
        UiObject allowBluetooth = device.findObject(new UiSelector().text("ZULASSEN"));
        UiObject allowBluetoothV2 = device.findObject(new UiSelector().text("JA"));
        if (allowBluetooth.exists()) {
            try {
                allowBluetooth.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        } else if (allowBluetoothV2.exists()) {
            try {
                allowBluetoothV2.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        }
        sleep(4000);
        // Location permission request
        UiObject allowPermissions = device.findObject(new UiSelector().text("ABLEHNEN"));
        if (allowPermissions.exists()) {
            try {
                allowPermissions.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        }
        sleep(1000);
        // Check if snackBar is displayed
        onView(withText(R.string.permissionNotGranted)).check(matches(isDisplayed()));
        sleep(5000);
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
