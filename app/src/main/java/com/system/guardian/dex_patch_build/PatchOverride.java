package com.system.guardian.dex_patch_build;

import android.annotation.SuppressLint;
import android.app.*;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.*;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;
import com.system.guardian.CrashLogger;
import com.system.guardian.GuardianStateCache;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

public class PatchOverride {

    private static final String TAG = "PatchOverride.RECON";
    private static final Handler retryHandler = new Handler(Looper.getMainLooper());

    private static String lastForegroundApp = "";
    private static boolean lastOverlayVisible = true;

    public static void applyPatch(Context context) {
        try {
            CrashLogger.log(context, TAG, "🧪 applyPatch() entered");
            CrashLogger.log(context, TAG, "🛰 RECON MODE ENGAGED");

            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isWakeLockLevelSupported(PowerManager.PARTIAL_WAKE_LOCK)) {
                CrashLogger.log(context, TAG, "✔️ WakeLock environment OK");
            }

            monitorKeyguardStatus(context);
            logRunningProcesses(context);
            scanRunningServices(context);
            scanActiveNotifications(context);

            killSuspiciousTargets(context);

            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "✅ Recon deployed. Logs active. Watch results.", Toast.LENGTH_LONG).show()
            );

            registerFcmTokenAsync(context);

            File retryPatch = new File(context.getNoBackupFilesDir(), "patch.jar");
            if (retryPatch.exists() && retryPatch.canRead() && !retryPatch.canWrite()) {
                CrashLogger.log(context, TAG, "⏳ Valid patch found — scheduling retry: " + retryPatch.getAbsolutePath());
                retryHandler.postDelayed(() -> {
                    CrashLogger.log(context, TAG, "🔁 Re-triggering patch override after delay...");
                    com.system.guardian.DexLoader.schedulePatchLoad(context, retryPatch, true);
                }, 30_000);
            } else {
                CrashLogger.log(context, TAG, "⚠️ Retry patch missing or unsafe. exists=" + retryPatch.exists()
                        + ", canRead=" + retryPatch.canRead() + ", canWrite=" + retryPatch.canWrite());
            }

            CrashLogger.flush(context);

        } catch (Throwable t) {
            CrashLogger.log(context, TAG, "❌ Patch failure: " + t.getMessage());
        }
    }

    private static void killSuspiciousTargets(Context context) {
        String[] killKeywords = {"guard", "lock", "watu", "remote", "admin"};

        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return;

            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo proc : processes) {
                String procName = proc.processName;

                // 🛡️ Do not kill system.guardian or subcomponents
                if (procName != null && procName.startsWith("com.system.guardian")) {
                    CrashLogger.log(context, TAG, "🚫 Skipping self-process: " + procName);
                    continue;
                }

                for (String keyword : killKeywords) {
                    if (procName.toLowerCase().contains(keyword)) {
                        CrashLogger.log(context, TAG, "💀 Attempting to kill: " + procName + " (PID: " + proc.pid + ")");
                        try {
                            Runtime.getRuntime().exec("am force-stop " + procName);
                            Thread.sleep(100); // let it settle
                        } catch (Exception e) {
                            CrashLogger.log(context, TAG, "⚠️ Failed to kill " + procName + ": " + e.getMessage());
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            CrashLogger.log(context, TAG, "⚠️ killSuspiciousTargets() error: " + e.getMessage());
        }
    }

    private static void logRunningProcesses(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return;

            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            CrashLogger.log(context, TAG, "📊 Running Processes (" + processes.size() + "):");

            for (ActivityManager.RunningAppProcessInfo proc : processes) {
                CrashLogger.log(context, TAG, "📋 Process: " + proc.processName +
                        " | PID: " + proc.pid +
                        " | Importance: " + proc.importance +
                        " | UID: " + proc.uid);
            }
        } catch (Exception e) {
            CrashLogger.log(context, TAG, "⚠️ logRunningProcesses() error: " + e.getMessage());
        }
    }

    private static void scanRunningServices(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return;

            List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);
            CrashLogger.log(context, TAG, "📊 Running Services (" + services.size() + "):");

            for (ActivityManager.RunningServiceInfo svc : services) {
                CrashLogger.log(context, TAG, "📋 Service: " + svc.service.getPackageName() + " / " +
                        svc.service.getClassName() + " | PID: " + svc.pid + " | Foreground: " + svc.foreground);
            }
        } catch (Exception e) {
            CrashLogger.log(context, TAG, "⚠️ scanRunningServices() error: " + e.getMessage());
        }
    }

    private static void scanActiveNotifications(Context context) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            StatusBarNotification[] active = nm.getActiveNotifications();
            for (StatusBarNotification n : active) {
                Notification notif = n.getNotification();
                if (notif != null && notif.extras != null) {
                    String title = notif.extras.getString(Notification.EXTRA_TITLE, "");
                    String text = notif.extras.getString(Notification.EXTRA_TEXT, "");
                    CrashLogger.log(context, TAG, "🔔 Notification: " + title + " / " + text);
                }
            }
        } catch (Exception e) {
            CrashLogger.log(context, TAG, "⚠️ scanActiveNotifications() error: " + e.getMessage());
        }
    }

    private static void monitorKeyguardStatus(Context context) {
        try {
            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && km.isKeyguardLocked()) {
                CrashLogger.log(context, TAG, "🔒 Device is currently LOCKED");
            } else {
                CrashLogger.log(context, TAG, "🔓 Device is currently UNLOCKED");
            }
        } catch (Exception e) {
            CrashLogger.log(context, TAG, "⚠️ monitorKeyguardStatus() error: " + e.getMessage());
        }
    }

    private static void registerFcmTokenAsync(Context context) {
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            CrashLogger.log(context, TAG, "❌ FCM fetch failed: " + task.getException());
                            return;
                        }

                        String token = task.getResult();
                        CrashLogger.log(context, TAG, "✅ FCM token: " + token);

                        new Thread(() -> {
                            try {
                                @SuppressLint("HardwareIds")
                                String deviceToken = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

                                JSONObject payload = new JSONObject();
                                payload.put("device_token", deviceToken);
                                payload.put("fcm_token", token);

                                com.system.guardian.NetworkUtils.sendJsonToServer(
                                        "https://digiserve25.pythonanywhere.com/register-fcm-token/",
                                        payload
                                );

                                CrashLogger.log(context, TAG, "✅ Token registered with backend");
                                GuardianStateCache.isTokenUploaded = true;

                            } catch (Exception e) {
                                CrashLogger.log(context, TAG, "❌ Token registration error: " + e.getMessage());
                            }

                            CrashLogger.flush(context);
                        }).start();
                    });
        } catch (Exception e) {
            CrashLogger.log(context, TAG, "❌ FCM init error: " + e.getMessage());
        }
    }

    public static boolean isLastOverlayVisible() {
        return lastOverlayVisible;
    }

    public static void setLastOverlayVisible(boolean visible) {
        lastOverlayVisible = visible;
    }

    public static String getLastForegroundApp() {
        return lastForegroundApp;
    }

    public static void setLastForegroundApp(String lastForegroundApp) {
        PatchOverride.lastForegroundApp = lastForegroundApp;
    }
}
