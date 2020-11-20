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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eisos.android.terminal.ImprintActivity;
import com.eisos.android.terminal.PolicyActivity;
import com.eisos.android.R;
import com.eisos.android.terminal.WhatsNewActivity;

public class InfoFragment extends Fragment {

    public static final String TAG = "InfoFragment";
    private LinearLayout llPolicy, llImprint, llWhatsNew, llSensors, llManuals, llSourceCode;
    private TextView tvVersion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);
        llPolicy = view.findViewById(R.id.ll_policy);
        llPolicy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPolicyClicked();
            }
        });

        llImprint = view.findViewById(R.id.ll_imprint);
        llImprint.setOnClickListener((View v) -> onImprintClicked());

        llWhatsNew = view.findViewById(R.id.ll_whats_new);
        llWhatsNew.setOnClickListener((View v) -> onWhatsNewClicked());

        llSensors = view.findViewById(R.id.ll_sensors);
        llSensors.setOnClickListener((View v) -> onSensorInfoClicked());

        llManuals = view.findViewById(R.id.ll_userManual);
        llManuals.setOnClickListener((View v) -> onUserManualsClicked());

        llSourceCode = view.findViewById(R.id.ll_source_code);
        llSourceCode.setOnClickListener((View v) -> onSourceCodeClicked());

        tvVersion = view.findViewById(R.id.tv_version);
        String version = "";
        try {
            version = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        }catch(PackageManager.NameNotFoundException nf) {
            nf.printStackTrace();
        }
        tvVersion.setText(version);
        setHasOptionsMenu(false);
        return view;
    }

    /**
     * onClickListener for the policy information
     */
    private void onPolicyClicked() {
        Intent intent = new Intent(getActivity(), PolicyActivity.class);
        getActivity().startActivity(intent);
    }

    /**
     * onClickListener for the imprint
     */
    private void onImprintClicked() {
        Intent intent = new Intent(getActivity(), ImprintActivity.class);
        getActivity().startActivity(intent);
    }

    /**
     * onClickListener for the what's new section
     */
    private void onWhatsNewClicked() {
        Intent intent = new Intent(getActivity(), WhatsNewActivity.class);
        getActivity().startActivity(intent);
    }

    /**
     * Links to the wireless connectivity page
     */
    private void onSensorInfoClicked() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.we-online.de/web/en/electronic_components/" +
                "produkte_pb/produktinnovationen/wirelessconnectivitylandingpage.php"));
        startActivity(browserIntent);
    }

    /**
     * Links to the wco manuals
     */
    private void onUserManualsClicked() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.we-online.com/web/en/electronic_components/" +
                "produkte_pb/service_pbs/wco/handbuecher/wco_handbuecher.php"));
        startActivity(browserIntent);
    }

    /**
     * Links to the source code of the app (GitHub.com)
     */
    private void onSourceCodeClicked() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/WurthElektronik/"));
        startActivity(browserIntent);
    }
}
