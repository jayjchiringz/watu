package com.system.guardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.system.guardian.core.LogUploader;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, InterceptorService.class);
            context.startService(serviceIntent);

            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
            LogUploader.uploadLog(context, "⚙️ Boot completed - services started");
        }
    }
}
