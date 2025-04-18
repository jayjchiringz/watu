package com.system.guardian;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class AdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show();
    }
}
