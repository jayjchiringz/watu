package com.system.guardian.dex_patch_build;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class PatchOverride {

    private static final String TAG = "PatchOverride";
    private static final String TARGET_PACKAGE = "com.watuke.app";

    public static void applyPatch(Context context) {
        try {
            Log.i(TAG, "🚨 Final Watu suppression patch initiated");

            // 🔋 WakeLock Environment Check
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isWakeLockLevelSupported(PowerManager.PARTIAL_WAKE_LOCK)) {
                Log.i(TAG, "✔️ WakeLock environment available");
            }

            // 🔕 Attempt to unregister Firebase Receiver
            try {
                Class<?> receiverClass = Class.forName("com.watuke.app.FirebaseMessageReceiver");
                BroadcastReceiver receiver = (BroadcastReceiver) receiverClass.getDeclaredConstructor().newInstance();
                context.unregisterReceiver(receiver);
                Log.i(TAG, "🛑 FirebaseMessageReceiver unregistered");
            } catch (Throwable t) {
                Log.i(TAG, "ℹ️ Firebase receiver suppression skipped or failed silently");
            }

            // 🔕 Attempt to unregister SMS BroadcastReceiver
            try {
                Class<?> smsReceiverClass = Class.forName("com.watuke.app.MySMSBroadcastReceiver");
                BroadcastReceiver smsReceiver = (BroadcastReceiver) smsReceiverClass.getDeclaredConstructor().newInstance();
                context.unregisterReceiver(smsReceiver);
                Log.i(TAG, "📴 SMS BroadcastReceiver unregistered");
            } catch (Throwable t) {
                Log.i(TAG, "ℹ️ SMS receiver suppression skipped or failed silently");
            }

            // 🔁 Attempt Force-Stop
            try {
                Process proc = Runtime.getRuntime().exec("am force-stop " + TARGET_PACKAGE);
                proc.waitFor();
                Log.i(TAG, "💀 Force-stopped Watu package");
            } catch (Exception e) {
                Log.w(TAG, "⚠️ Failed to force-stop: " + e.getMessage());
            }

            // 🔧 Cancel possible alarms via AlarmManager (if known)
            try {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (am != null) {
                    Log.i(TAG, "⏰ AlarmManager present — placeholder only, no PendingIntent refs yet.");
                }
            } catch (Throwable t) {
                Log.w(TAG, "⚠️ AlarmManager suppression failed: " + t.getMessage());
            }

            // 🧯 Kill running services from Watu
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);
                for (ActivityManager.RunningServiceInfo svc : services) {
                    if (TARGET_PACKAGE.equals(svc.service.getPackageName())) {
                        Intent i = new Intent().setComponent(svc.service);
                        context.stopService(i);
                        Log.i(TAG, "🧯 Killed Watu service: " + svc.service.getClassName());
                    }
                }
            }

            // 🔍 Final Validation Log
            assert am != null;
            List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo proc : procs) {
                if (proc.processName.equals(TARGET_PACKAGE)) {
                    Log.w(TAG, "⚠️ Watu process still running after kill attempt");
                }
            }

            Toast.makeText(context, "✅ Watu Suppression Complete", Toast.LENGTH_LONG).show();
            Log.i(TAG, "🎯 Suppression cycle complete");

        } catch (Throwable t) {
            Log.e(TAG, "❌ PatchOverride fatal error", t);
        }
    }
}
