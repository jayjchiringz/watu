package com.system.guardian;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

public class GhostModeService extends Service {

    private static final String TAG = "👻GhostSilentService";
    private static final String CHANNEL_ID = "ghost_channel";
    private static final int NOTIFICATION_ID = 101;
    private static final long RESTART_DELAY_MS = 500;

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        CrashLogger.log(this, TAG, "🧬 Service initializing...");

        acquireWakeLock();
        startInvisibleForeground();
        blockUnwantedComponents();

        CrashLogger.log(this, TAG, "👻 GhostMode Activated: Silent bypass engaged.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CrashLogger.log(this, TAG, "👁️ Ghost service heartbeat...");
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        scheduleSelfRestart();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        CrashLogger.log(this, TAG, "👻 GhostModeService destroyed, rearming...");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, GhostModeService.class);
        context.startService(intent);
    }

    // --- Internals ---

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":Wakelock");
            wakeLock.acquire(60 * 1000L /*1 minute*/);
            CrashLogger.log(this, TAG, "🔒 WakeLock acquired");
        } else {
            CrashLogger.log(this, TAG, "⚠️ PowerManager null — WakeLock skipped");
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            CrashLogger.log(this, TAG, "🔓 WakeLock released");
        }
    }

    private void startInvisibleForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Ghost Channel", NotificationManager.IMPORTANCE_NONE
                );
                channel.setDescription("Silent stealth channel");
                nm.createNotificationChannel(channel);
            }

            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("")
                    .setSmallIcon(android.R.color.transparent)
                    .build();

            startForeground(NOTIFICATION_ID, notification);
            CrashLogger.log(this, TAG, "🧊 Foreground started invisibly");
        }
    }

    private void blockUnwantedComponents() {
        Blocker.disableFirebaseComponents(getApplicationContext());
        Blocker.suppressWatuJobServices(getApplicationContext());
        CrashLogger.log(this, TAG, "🚫 Firebase & Watu launch blocked");
    }

    private void scheduleSelfRestart() {
        Intent restartIntent = new Intent(getApplicationContext(), GhostModeService.class);
        restartIntent.setPackage(getPackageName());

        PendingIntent pending = PendingIntent.getService(
                this, 1, restartIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) {
            alarm.set(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + RESTART_DELAY_MS,
                    pending
            );
            CrashLogger.log(this, TAG, "🩺 Watchdog restart armed");
        } else {
            CrashLogger.log(this, TAG, "❌ AlarmManager null — restart failed");
        }
    }
}
