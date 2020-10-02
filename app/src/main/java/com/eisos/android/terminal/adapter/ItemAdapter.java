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
package com.eisos.android.terminal.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eisos.android.R;
import com.eisos.android.terminal.MainActivity;
import com.eisos.android.terminal.customLayout.ScanListItem;
import com.eisos.android.terminal.frags.ScanFragment;

import java.util.ArrayList;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.DeviceHolder> {

    private Context context;
    private ScanFragment scanFragment;
    private ArrayList<ScanListItem> mListValues;

    public ItemAdapter(@NonNull Context context, ScanFragment scanFragment) {
        this.context = context;
        this.scanFragment = scanFragment;
        mListValues = new ArrayList<>();
    }

    @NonNull
    @Override
    public DeviceHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(this.context)
                .inflate(R.layout.scan_device_item, parent, false);

        return new DeviceHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceHolder holder, int position) {
        final ScanListItem listItem = mListValues.get(position);
        holder.itemView.setOnClickListener((View v) -> {
            scanFragment.connectDevice(listItem);
        });
        holder.tvDeviceName.setText(listItem.getDeviceName());
        holder.tvDeviceAddress.setText(listItem.getDeviceAddress());
        holder.tvRssi.setText(listItem.getRssiText());
        holder.imgRssi.setImageResource(listItem.getImgResourceRssi());
        holder.imgState.setImageResource(listItem.getImgResourceConState());
        holder.imgFav.setOnClickListener((View v) -> {
            if(holder.imgFav.getDrawable().getConstantState().equals(context.getDrawable(R.drawable.ic_star_border).getConstantState())) {
                holder.imgFav.setImageResource(R.drawable.ic_star);
                scanFragment.addDeviceToFavourites(listItem);
            } else {
                holder.imgFav.setImageResource(R.drawable.ic_star_border);
                scanFragment.deleteDeviceFromFavourites(listItem);
            }
        });
        holder.imgMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(MainActivity.getActivity(), holder.imgMenu);
                menu.getMenuInflater().inflate(R.menu.scanned_item_menu, menu.getMenu());
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menuItem_autoconnect:
                                scanFragment.onAutoConnectClicked(listItem);
                                break;
                            case R.id.menuItem_preferred_phy:
                                scanFragment.onPreferredPhyClicked(listItem);
                                break;
                        }
                        return true;
                    }
                });
                menu.show();
            }
        });

        if(scanFragment.isDeviceFavourite(listItem)) {
            holder.imgFav.setImageResource(R.drawable.ic_star);
        } else {
            holder.imgFav.setImageResource(R.drawable.ic_star_border);
        }

        if(!listItem.isItemUpdating()) {
            listItem.setRssiAlpha(0.75f);
            listItem.setRssiColor(Color.GRAY);
            holder.tvRssi.setTextColor(listItem.getRssiColor());
        }
        else {
            listItem.setRssiAlpha(1.0f);
            listItem.setRssiColor(context.getResources().getColor(R.color.colorPrimary));
            holder.tvRssi.setTextColor(Color.BLACK);
        }

        if(listItem.getMenuVisibility()) {
            holder.imgMenu.setEnabled(true);
            holder.imgMenu.setAlpha(1.0f);
            holder.imgMenu.setColorFilter(Color.BLACK);
        } else {
            holder.imgMenu.setEnabled(false);
            holder.imgMenu.setAlpha(0.75f);
            holder.imgMenu.setColorFilter(Color.GRAY);
        }

        holder.tvRssi.setAlpha(listItem.getRssiAlpha());
        holder.imgRssi.setAlpha(listItem.getRssiAlpha());
        holder.imgRssi.setColorFilter(listItem.getRssiColor());
    }

    @Override
    public long getItemId(int position) {
        return mListValues.get(position).getID();
    }

    @Override
    public int getItemCount() {
        return mListValues.size();
    }

    public void addListItem(ScanListItem listItem) {
        mListValues.add(listItem);
        notifyItemInserted(mListValues.size()-1);
    }

    public ArrayList<ScanListItem> getItems() {
        return this.mListValues;
    }

    public void clearDevices() {
        mListValues.clear();
        notifyDataSetChanged();
    }

    public void setAllItemsNotUpdating(boolean value) {
        if(value) {
            for (ScanListItem item : mListValues) {
                item.setItemUpdateStatus(false);
            }
        } else {
            for (ScanListItem item : mListValues) {
                item.setItemUpdateStatus(true);
            }
        }
        notifyDataSetChanged();
    }

    class DeviceHolder extends RecyclerView.ViewHolder {
        private TextView tvDeviceName;
        private TextView tvDeviceAddress;
        private TextView tvRssi;
        private ImageView imgFav;
        private ImageView imgRssi;
        private ImageView imgState;
        private ImageView imgMenu;

        public DeviceHolder(View view) {
            super(view);
            tvDeviceName = view.findViewById(R.id.tv_device_name);
            tvDeviceAddress = view.findViewById(R.id.tv_device_address);
            tvRssi = view.findViewById(R.id.tv_rssi);
            imgFav = view.findViewById(R.id.img_star);
            imgRssi = view.findViewById(R.id.img_rssi);
            imgState = view.findViewById(R.id.img_connection_state);
            imgMenu = view.findViewById(R.id.img_popup_menu);
        }
    }
}
