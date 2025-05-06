package com.system.guardian.dex_patch_build;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;
import com.system.guardian.CrashLogger;
import com.system.guardian.GuardianStateCache;
import com.system.guardian.LockStateWatcher;

import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PatchOverride {

    private static final String TAG = "PatchOverride.RECON";
    private static final Handler retryHandler = new Handler(Looper.getMainLooper());

    private static final long PATCH_RETRY_DELAY_MS = 30_000;
    private static final String PATCH_FILENAME = "patch.jar";

    private static String lastForegroundApp = "";
    private static boolean lastOverlayVisible = true;
    private static long lastPatchRetryLog = 0;
    private static long lastPatchModified = 0;
    private static boolean retryScheduledForCurrentPatch = false;
    private static String lastServicesSnapshot = "";

    private static final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private static LockStateWatcher lockWatcher;

    public static void applyPatch(Context context) {
        try {
            CrashLogger.log(context, TAG, "🧪 applyPatch() entered");
            CrashLogger.log(context, TAG, "🛰 RECON MODE ENGAGED");

            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isWakeLockLevelSupported(PowerManager.PARTIAL_WAKE_LOCK)) {
                CrashLogger.log(context, TAG, "✔️ WakeLock environment OK");
            }

            monitorKeyguardStatus(context);

            if (lockWatcher == null) {
                lockWatcher = new LockStateWatcher(context);
                lockWatcher.start();
            }

            logRunningProcesses(context);
            scanRunningServices(context);
            scanActiveNotifications(context);

            detectSuspiciousProcesses(context); // Kill logic removed

            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "✅ Recon deployed. Logs active. Watch results.", Toast.LENGTH_LONG).show()
            );

            registerFcmTokenAsync(context);

            File retryPatch = new File(context.getNoBackupFilesDir(), PATCH_FILENAME);
            if (retryPatch.exists() && retryPatch.canRead() && !retryPatch.canWrite()) {
                long modified = retryPatch.lastModified();

                if (modified != lastPatchModified) {
                    lastPatchModified = modified;
                    retryScheduledForCurrentPatch = false;
                }

                if (!retryScheduledForCurrentPatch) {
                    retryScheduledForCurrentPatch = true;

                    CrashLogger.log(context, TAG, "⏳ Patch version valid — scheduling retry: " + retryPatch.getAbsolutePath());

                    retryHandler.postDelayed(() -> {
                        CrashLogger.log(context, TAG, "🔁 Re-triggering patch override after delay...");
                        com.system.guardian.DexLoader.schedulePatchLoad(context, retryPatch, true);
                    }, PATCH_RETRY_DELAY_MS);
                } else {
                    CrashLogger.log(context, TAG, "⏱ Patch already scheduled. Awaiting manual re-trigger or patch update.");
                }
            } else {
                CrashLogger.log(context, TAG, "⚠️ Retry patch missing or unsafe. exists=" + retryPatch.exists()
                        + ", canRead=" + retryPatch.canRead() + ", canWrite=" + retryPatch.canWrite());
            }

            CrashLogger.flush(context);

        } catch (Throwable t) {
            CrashLogger.log(context, TAG, "❌ Patch failure: " + t.getMessage());
        }
    }

    // Replacement for kill logic: logs only
    private static void detectSuspiciousProcesses(Context context) {
        String[] killKeywords = {"guard", "lock", "watu", "remote", "admin"};

        try {
            ActivityManager am = getActivityManager(context);
            if (am == null) return;

            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo proc : processes) {
                String procName = proc.processName;

                if (procName != null && procName.startsWith("com.system.guardian")) {
                    CrashLogger.log(context, TAG, "🚫 Skipping self-process: " + procName);
                    continue;
                }

                for (String keyword : killKeywords) {
                    if (procName != null && procName.toLowerCase().contains(keyword)) {
                        CrashLogger.log(context, TAG, "🕵️ Suspicious process: " + procName + " (PID: " + proc.pid + ")");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            CrashLogger.log(context, TAG, "⚠️ detectSuspiciousProcesses() error: " + e.getMessage());
        }
    }

    private static void logRunningProcesses(Context context) {
        try {
            ActivityManager am = getActivityManager(context);
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
            ActivityManager am = getActivityManager(context);
            if (am == null) return;

            List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);

            StringBuilder snapshot = new StringBuilder();
            for (ActivityManager.RunningServiceInfo svc : services) {
                snapshot.append(svc.service.getPackageName()).append("/")
                        .append(svc.service.getClassName()).append("|")
                        .append(svc.pid).append("|")
                        .append(svc.foreground).append("\n");
            }

            String newSnapshot = snapshot.toString();
            if (!newSnapshot.equals(lastServicesSnapshot)) {
                CrashLogger.log(context, TAG, "📊 Running Services (" + services.size() + "):");
                for (String line : newSnapshot.split("\n")) {
                    if (!line.isEmpty()) CrashLogger.log(context, TAG, "📋 Service: " + line);
                }
                lastServicesSnapshot = newSnapshot;
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
            KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager == null) {
                CrashLogger.log(context, TAG, "⚠️ KeyguardManager unavailable.");
                return;
            }

            boolean isLocked = keyguardManager.isKeyguardLocked();
            String lockStatus = isLocked ? "🔒 LOCKED" : "🔓 UNLOCKED";

            String lastApp = PatchOverride.getLastForegroundApp();
            String localCause = isLocked ? inferLockSource(context) : "N/A";
            String remoteHint = isLocked ? inferRemoteLockByUsageTimeline(context) : "N/A";
            String lockCauseHint = localCause + " | " + remoteHint;

            CrashLogger.log(context, TAG, String.format("🧩 Keyguard status: %s | LastApp=%s | Suspect=%s",
                    lockStatus, lastApp, lockCauseHint));

        } catch (Exception e) {
            CrashLogger.log(context, TAG, "⚠️ monitorKeyguardStatus() error: " + e.getMessage());
        }
    }

    /**
     * Try to infer who triggered the device lock
     */
    private static String inferLockSource(Context context) {
        try {
            ActivityManager am = getActivityManager(context);
            if (am == null) return "Unknown";

            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo proc : processes) {
                String pname = proc.processName.toLowerCase();

                if ((pname.contains("lock") || pname.contains("keyguard") || pname.contains("screen"))
                        && proc.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return pname + " (fg)";
                }
            }

            return "No obvious locker process";

        } catch (Exception e) {
            return "Lock source detection failed: " + e.getMessage();
        }
    }

    @SuppressLint("WrongConstant")
    private static String inferRemoteLockByUsageTimeline(Context context) {
        try {
            UsageStatsManager usageStatsManager =
                    (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

            if (usageStatsManager == null) return "UsageStatsManager unavailable";

            long now = System.currentTimeMillis();
            long windowStart = now - 10_000; // last 10 seconds

            UsageEvents events = usageStatsManager.queryEvents(windowStart, now);
            if (events == null) return "No usage events available";

            String lastLockRelatedPackage = null;
            long lastLockEventTime = 0;
            StringBuilder timeline = new StringBuilder();

            while (events.hasNextEvent()) {
                UsageEvents.Event event = new UsageEvents.Event();
                events.getNextEvent(event);

                String pkg = event.getPackageName();
                int type = event.getEventType();
                long time = event.getTimeStamp();

                if (type == UsageEvents.Event.SCREEN_NON_INTERACTIVE
                        || type == UsageEvents.Event.KEYGUARD_SHOWN
                        || pkg.toLowerCase().contains("admin")
                        || pkg.toLowerCase().contains("lock")
                        || pkg.toLowerCase().contains("remote")
                        || pkg.toLowerCase().contains("find")) {

                    timeline.append("⚠️ ").append(pkg).append(" | event=").append(type).append(" | @").append(time).append("\n");

                    if (time > lastLockEventTime) {
                        lastLockRelatedPackage = pkg;
                        lastLockEventTime = time;
                    }
                }
            }

            if (lastLockRelatedPackage != null) {
                return "Detected remote/admin lock pattern via " + lastLockRelatedPackage + " @ " + lastLockEventTime;
            } else {
                return "No remote/admin lock triggers in timeline";
            }

        } catch (Exception e) {
            return "Usage timeline analysis failed: " + e.getMessage();
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

                        backgroundExecutor.execute(() -> {
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
                        });
                    });
        } catch (Exception e) {
            CrashLogger.log(context, TAG, "❌ FCM init error: " + e.getMessage());
        }
    }

    private static ActivityManager getActivityManager(Context context) {
        return (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public static String getRunningServicesSnapshot(Context context) {
        StringBuilder result = new StringBuilder();
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);
                for (ActivityManager.RunningServiceInfo svc : services) {
                    result.append(svc.service.getPackageName()).append("/")
                            .append(svc.service.getClassName()).append("|PID: ")
                            .append(svc.pid).append("|FG: ")
                            .append(svc.foreground).append("\n");
                }
            }
        } catch (Exception e) {
            result.append("⚠️ Failed to retrieve services: ").append(e.getMessage());
        }
        return result.toString();
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

    public static long getLastPatchRetryLog() {
        return lastPatchRetryLog;
    }

    public static void setLastPatchRetryLog(long lastPatchRetryLog) {
        PatchOverride.lastPatchRetryLog = lastPatchRetryLog;
    }
}