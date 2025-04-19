package com.system.guardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class InstallResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra("android.content.pm.extra.STATUS", -1);
        String msg = intent.getStringExtra("android.content.pm.extra.STATUS_MESSAGE");

        Log.i("InstallReceiver", "Install result: " + status + " — " + msg);
        CrashLogger.log(context, "InstallReceiver", "📦 Silent Install Status: " + status + " — " + msg);
    }
}
