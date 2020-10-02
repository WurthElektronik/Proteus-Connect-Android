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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.eisos.android.R;
import com.eisos.android.terminal.SplashActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.eisos.android.terminal.MainActivity.getActivity;
import static org.hamcrest.Matchers.not;

/**
 * Before starting the tests the app should have been started at least one time,
 * bluetooth should be enabled and the location permission granted
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class Espresso_Connection_Test {

    @Rule
    public ActivityTestRule<SplashActivity> activityRule =
            new ActivityTestRule<>(SplashActivity.class);

    /**
     * Only works if device name = Prot3
     */
    @Test
    public void proteus3_Connection_Disconnection_correct_input() {
        // Waiting for SplashActivity to end and Proteus3 to be scanned
        sleep(3000);
        onView(withText("Prot3")).check(matches(isDisplayed())).perform(click());
        sleep(4500); // Wait for device answer
        onView(ViewMatchers.withId(R.id.et_command)).perform(typeText("010203"), closeSoftKeyboard());
        sleep(500);
        onView(withId(R.id.btn_send)).perform(click());
        sleep(2000);
        onView(withId(R.id.et_command)).check(matches(withText("")));
        sleep(500);
        onView(withId(R.id.menu_disconnect)).check(matches(isDisplayed())).perform(click());
        sleep(2000);
    }

    /**
     * Only works if device name = Prot3. Encoding has to be set to hex
     */
    @Test
    public void proteus3_Connection_Disconnection_wrong_hex_input() {
        // Waiting for SplashActivity to end and Proteus3 to be scanned
        sleep(3000);
        onView(withText("Prot3")).check(matches(isDisplayed())).perform(click());
        sleep(4500); // Wait for device answer
        onView(withId(R.id.et_command)).perform(typeText("123"), closeSoftKeyboard()); // Not hexadecimal
        sleep(500);
        onView(withId(R.id.btn_send)).perform(click());
        onView(withId(R.id.et_command)).check(matches(withText("")));
        // Check if toast is displayed
        onView(withText(R.string.hexParsingError)).inRoot(withDecorView(not(getActivity().getWindow().getDecorView()))) .check(matches(isDisplayed()));
        sleep(1000);
        onView(withId(R.id.menu_disconnect)).check(matches(isDisplayed())).perform(click());
        sleep(2000);
    }


    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
