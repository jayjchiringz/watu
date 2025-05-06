package com.system.guardian;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.system.guardian.firebase.HijackedFirebaseWorker;

import java.util.concurrent.TimeUnit;

public class Blocker {

    public static void disableFirebaseComponents(Context context) {
        if (!GuardianStateCache.isTokenUploaded) {
            CrashLogger.log(context, "Blocker", "🚫 Firebase block skipped — FCM token not uploaded yet.");
            return;
        }

        CrashLogger.log(context, "Blocker", "👻 Disabling Firebase Sync...");
        try {
            context.stopService(new Intent().setAction("com.google.firebase.MESSAGING_EVENT"));
            disableComponent(context, "com.google.firebase.components.ComponentDiscoveryService");
        } catch (Exception e) {
            CrashLogger.log(context, "BlockerError", "⚠️ Error disabling Firebase: " + e.getMessage());
        }
    }

    public static void suppressWatuJobServices(Context context) {
        CrashLogger.log(context, "Blocker", "💀 Suppressing Watu triggers...");
        disableComponent(context, "com.watuke.background.Scheduler");
        disableComponent(context, "com.watuke.receiver.BootReceiver");
        disableComponent(context, "com.watuke.receiver.UserUnlockReceiver");
        disableComponent(context, "com.watuke.background.FirebaseJobService");

        scheduleHijackedJob(context); // ✅ Shadow injector via WorkManager
    }

    private static void disableComponent(Context context, String className) {
        try {
            ComponentName component = new ComponentName(context, className);
            context.getPackageManager().setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );
            CrashLogger.log(context, "Blocker", "✅ Disabled: " + className);
        } catch (Exception e) {
            CrashLogger.log(context, "BlockerError", "⚠️ Failed to disable: " + className + " — " + e.getMessage());
        }
    }

    private static void scheduleHijackedJob(Context context) {
        try {
            CrashLogger.log(context, "Hijack", "🧿 Scheduling spoofed FirebaseJobService via WorkManager...");

            PeriodicWorkRequest spoofedJob = new PeriodicWorkRequest.Builder(
                    HijackedFirebaseWorker.class, 15, TimeUnit.MINUTES)
                    .addTag("com.watuke.background.FirebaseJobService")
                    .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "HijackedFirebaseJob",
                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                    spoofedJob
            );

            CrashLogger.log(context, "Hijack", "✅ Spoofed WorkManager job scheduled.");
        } catch (Exception e) {
            CrashLogger.log(context, "HijackError", "⚠️ Failed to schedule spoofed job: " + e.getMessage());
        }
    }
}
