/*
 * LibreAV - Anti-malware for Android using machine learning
 * Copyright (C) 2020 Project Matris
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.myapplication;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cn.nekocode.badge.BadgeDrawable;

public class AppsAdapter extends RecyclerView.Adapter {
    int position;
    Context context;
    public ArrayList<AppInfo> apps;
    private WeakReference<Context> contextRef;

    public AppsAdapter(Context context, ArrayList<AppInfo> scannedapps) {
        this.context=context;
        apps = scannedapps;
        contextRef = new WeakReference<>(context);
    }
    public ArrayList<AppInfo> getApps(){
        return apps;
    }
    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appLabel;
        TextView prediction;
        ImageView uninstallButton;

        ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.itemIcon);
            appLabel = itemView.findViewById(R.id.itemLabel);
            prediction = itemView.findViewById(R.id.itemSecondaryLabel);
            uninstallButton = itemView.findViewById(R.id.uninstallButton);
        }

        void bindAppInfo(final AppInfo appInfo) {
            if (appInfo.appIcon == null) {
                appInfo.appIcon = appInfo.loadIcon(contextRef.get());
            }
            appIcon.setImageDrawable(appInfo.appIcon);
            appLabel.setText(appInfo.appName);

            if (appInfo.prediction.equalsIgnoreCase(contextRef.get().getString(R.string.malware))) {
                prediction.setTextColor(Color.parseColor("#FF0000"));
            } else if (appInfo.prediction.equalsIgnoreCase(contextRef.get().getString(R.string.safe))) {
                prediction.setTextColor(Color.parseColor("#008000"));
            } else if (appInfo.prediction.equalsIgnoreCase(contextRef.get().getString(R.string.risky))) {
                prediction.setTextColor(Color.parseColor("#FFA500"));
            } else {
                prediction.setTextColor(Color.parseColor("#0080FF"));
            }

            if (appInfo.isSystemApp == 1) {
                final BadgeDrawable drawable2 =
                        new BadgeDrawable.Builder()
                                .type(BadgeDrawable.TYPE_ONLY_ONE_TEXT)
                                .badgeColor(0xff336699)
                                .text1(contextRef.get().getString(R.string.system_app))
                                .build();

                SpannableString spannableString =
                        new SpannableString(TextUtils.concat(appInfo.prediction, "  ", drawable2.toSpannable()));
                prediction.setText(spannableString);

            } else {
                prediction.setText(appInfo.prediction);
            }
            uninstallButton.setOnClickListener(new View.OnClickListener() {
                                                   @Override
                                                   public void onClick(View v) {
                                                       LocalBroadcastManager.getInstance(contextRef.get()).registerReceiver(mMessageReceiver,
                                                               new IntentFilter("uninstall"));
                                                       Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                                                       intent.setData(Uri.parse("package:" + appInfo.packageName));
                                                       intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                                                       position=getAdapterPosition();
                                                       ((Activity)context).startActivityForResult(intent,1);

                                                   }
                                               }
            );
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent =new Intent(contextRef.get(), AppDetails.class);
                    intent.putExtra("appName",appInfo.appName);
                    intent.putExtra("packageName",appInfo.packageName);
                    intent.putExtra("result",appInfo.prediction);
                    intent.putExtra("prediction",appInfo.predictionScore);
                    intent.putExtra("scan_mode","normal_scan");
                    contextRef.get().startActivity(intent);
                }
            });
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LocalBroadcastManager.getInstance(contextRef.get()).unregisterReceiver(mMessageReceiver);
            String message = intent.getStringExtra("uninstall");
            if(message.equals("yes"))
                delete(position);
        }
    };

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LocalBroadcastManager.getInstance(parent.getContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("uninstall"));
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_result_list_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        ViewHolder vh = (ViewHolder) holder;
        vh.bindAppInfo(apps.get(position));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    private void delete(int position) {
        try {
            Log.d("un", String.valueOf(position));
            apps.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, apps.size());
            Toast.makeText(contextRef.get(),contextRef.get().getString(R.string.uninstall_successful),Toast.LENGTH_SHORT).show();
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }
}
