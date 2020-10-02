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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.eisos.android.R;
import com.eisos.android.terminal.frags.ScanFragment;
import com.eisos.android.terminal.utils.Preferences;

public class PolicyActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbarTitle, text;
    private LinearLayout toolbarLogo;
    private Button btnAccept;
    private SharedPreferences sharedPrefs;
    private boolean firstStart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy);
        sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        firstStart = sharedPrefs.getBoolean(Preferences.PREF_FIRST_START, true);

        toolbar = findViewById(R.id.toolbar);

        toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getString(R.string.privacyPolicy));
        toolbarTitle.setVisibility(View.VISIBLE);

        toolbarLogo = toolbar.findViewById(R.id.toolbar_logo);
        toolbarLogo.setVisibility(View.GONE);

        text = findViewById(R.id.tv_policyText);
        text.setText(getText(R.string.policyContent));
        text.setMovementMethod(LinkMovementMethod.getInstance());

        btnAccept = findViewById(R.id.btn_accept);
        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAcceptClicked();
            }
        });

        if(firstStart) {
            toolbar.setNavigationIcon(R.drawable.ic_info);
            toolbar.setNavigationOnClickListener(null);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
            btnAccept.setVisibility(View.GONE);
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!firstStart) {
            ScanFragment.getFragment().bindActivityToService(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!firstStart) {
            ScanFragment.getFragment().unbindActivityFromService(this);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        if(firstStart) {
            finishAndRemoveTask();
        }
        super.onBackPressed();
    }

    /**
     * handles the action when the Accept Button has been pressed
     */
    private void onAcceptClicked() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean firstStart = prefs.getBoolean(Preferences.PREF_FIRST_START, true);
        if(firstStart) {
            // not the first start anymore
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(Preferences.PREF_FIRST_START, false);
            editor.commit();

            Intent intent = new Intent(PolicyActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            finish();
        }
    }
}
