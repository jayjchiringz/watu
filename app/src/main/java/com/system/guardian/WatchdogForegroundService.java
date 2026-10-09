package com.system.guardian;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.system.guardian.CrashLogger;
import com.system.guardian.OverlayBlocker;
import com.system.guardian.R;

import java.io.IOException;
public class WatchdogForegroundService extends Service {

    private static final String CHANNEL_ID = "guardian_watchdog";
    private final Handler handler = new Handler();

    private final Runnable watchdogLoop = new Runnable() {
        @Override
        public void run() {
            try {
                checkTopApp(); // 🆕 monitor foreground UI context

                if (isWatuAlive()) {
                    CrashLogger.log(getApplicationContext(), "WatchdogService", "⚠️ Watu running — suppressing again");
                    OverlayBlocker.show(getApplicationContext());
                    killWatu();
                } else {
                    // 🛡️ Keep suppression persistent
                    CrashLogger.log(getApplicationContext(), "WatchdogService", "🛡️ Keeping overlay active post-Watu kill");
                }

                // 🔒 Keyguard overlay protection
                KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                if (km != null && km.isKeyguardLocked()) {
                    CrashLogger.log(getApplicationContext(), "WatchdogService", "🔒 Keyguard detected — re-enabling overlay");
                    OverlayBlocker.show(getApplicationContext());
                }

                // 🩺 Check if overlay is still showing
                if (!OverlayBlocker.isShowing()) {
                    CrashLogger.log(getApplicationContext(), "WatchdogService", "❗ Overlay unexpectedly hidden — restoring");
                    OverlayBlocker.show(getApplicationContext());
                }

            } catch (Exception e) {
                CrashLogger.log(getApplicationContext(), "WatchdogService", "❌ Watchdog error: " + e.getMessage());
            }

            handler.postDelayed(this, 5000); // 5s interval
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // Now using safe loop trigger inside onStartCommand()
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(watchdogLoop);
        super.onDestroy();
        CrashLogger.log(this, "WatchdogService", "🧯 Foreground watchdog stopped");
    }

    private boolean started = false;

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!started) {
            try {
                startForeground(1, buildNotification());
                handler.postDelayed(watchdogLoop, 1000);
                CrashLogger.log(this, "WatchdogService", "🚀 Watchdog loop scheduled");
                CrashLogger.flush(this); // ✅ Ensures immediate upload of launch logs
            } catch (Exception e) {
                CrashLogger.log(this, "WatchdogService", "❌ Failed to start loop: " + e.getMessage());
            }
            started = true;
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean isWatuAlive() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        for (ActivityManager.RunningAppProcessInfo proc : am.getRunningAppProcesses()) {
            if (GuardianConfig.isTarget(this, proc.processName)) return true;
        }
        return false;
    }

    private void killWatu() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                for (String pkg : GuardianConfig.getTargets(this)) {
                    am.killBackgroundProcesses(pkg);
                }
            }
            for (String pkg : GuardianConfig.getTargets(this)) {
                try {
                    Runtime.getRuntime().exec("am force-stop " + pkg);
                } catch (IOException ignored) {}
            }
            CrashLogger.log(getApplicationContext(), "WatchdogService", "🛑 Target(s) force-stopped");
        } catch (Exception e) {
            CrashLogger.log(getApplicationContext(), "WatchdogService", "❌ force-stop failed: " + e.getMessage());
        }
    }

    private void checkTopApp() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return;
            for (ActivityManager.AppTask task : am.getAppTasks()) {
                if (task.getTaskInfo() == null || task.getTaskInfo().topActivity == null) continue;
                String top = task.getTaskInfo().topActivity.getPackageName();
                if (GuardianConfig.isTarget(this, top)) {
                    CrashLogger.log(getApplicationContext(), "WatchdogService",
                            "👁️ Target top activity (" + top + ") — re-suppressing");
                    OverlayBlocker.show(getApplicationContext());
                    killWatu();
                }
                // else: swallow. Do not log every foreground app — it floods the feed.
            }
        } catch (Exception e) {
            Log.e("WatchdogService", "checkTopApp() failed", e);
        }
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(CHANNEL_ID, "Guardian Watchdog", NotificationManager.IMPORTANCE_MIN);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(chan);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Guardian Watchdog Active")
                .setContentText("Monitoring Watu and system lock events...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();
    }
}
