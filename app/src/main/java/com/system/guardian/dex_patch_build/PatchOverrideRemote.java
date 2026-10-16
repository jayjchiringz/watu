package com.system.guardian.dex_patch_build;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.system.guardian.CrashLogger;
import com.system.guardian.OverlayBlocker;

import java.util.List;

public class PatchOverrideRemote {

    private static final String TAG = "PatchOverride.d_1_1_11";
    private static final String TARGET_PACKAGE = "com.watuke.app";
    private static final Handler retryHandler = new Handler(Looper.getMainLooper());

    public static void applyPatch(Context context) {
        try {
            CrashLogger.log(context, TAG, "🚨 DIAGNOSTIC PATCH ACTIVE — Full kill+trace attempt");
            Log.i(TAG, "🚨 DIAGNOSTIC PATCH ACTIVE");

            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isWakeLockLevelSupported(PowerManager.PARTIAL_WAKE_LOCK)) {
                Log.i(TAG, "✔️ WakeLock environment OK");
            }

            // Pre-kill trace
            logWatuStatus(context, "📊 Pre-kill status check");

            // Kill sequence (with retry)
            for (int i = 1; i <= 3; i++) {
                killWatu(context);
                Thread.sleep(1500);
                if (!isWatuRunning(context)) {
                    CrashLogger.log(context, TAG, "✅ Watu not detected after kill attempt " + i);
                    break;
                } else {
                    CrashLogger.log(context, TAG, "🔁 Watu still running after attempt " + i);
                }
            }

            // Overlay refresh (force hide then show)
            OverlayBlocker.hide(context);
            Thread.sleep(500);
            OverlayBlocker.show(context);
            CrashLogger.log(context, TAG, "🛡️ Overlay forcibly reset");

            // Toast for local feedback
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "✅ Diagnostic Patch Run", Toast.LENGTH_LONG).show()
            );

            // Keyguard + overlay post actions (now main-thread safe)
            new Handler(Looper.getMainLooper()).post(() -> {
                injectSmartSuppression(context);
            });

            // Post-trace
            logWatuStatus(context, "📊 Post-kill status check");
            CrashLogger.flush(context);

            // 🔁 Re-trigger control polling via Service
            Intent intent = new Intent(context, com.system.guardian.ControlPollerService.class);
            com.system.guardian.ControlPollerService.enqueueWork(context, intent);
            CrashLogger.log(context, TAG, "📡 Triggered ControlPollerService from patch");

            // 🛰️ Optional fallback: WorkManager task to poll server
            androidx.work.WorkManager.getInstance(context).enqueue(
                    new androidx.work.OneTimeWorkRequest.Builder(com.system.guardian.ControlPollerWorker.class).build()
            );
            CrashLogger.log(context, TAG, "📶 Triggered ControlPollerWorker (WorkManager fallback)");

        } catch (Throwable t) {
            CrashLogger.log(context, TAG, "❌ Patch failure: " + t.getMessage());
            Log.e(TAG, "Patch failure", t);
        }
    }

    private static void killWatu(Context context) {
        try {
            CrashLogger.log(context, TAG, "🧪 Executing killWatu()");
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

            if (am != null) {
                am.killBackgroundProcesses(TARGET_PACKAGE);
                CrashLogger.log(context, TAG, "🔪 killBackgroundProcesses() called");
            }

            Runtime.getRuntime().exec("am force-stop " + TARGET_PACKAGE);
            CrashLogger.log(context, TAG, "💀 force-stop shell command sent");

            if (am != null) {
                List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);
                for (ActivityManager.RunningServiceInfo svc : services) {
                    if (TARGET_PACKAGE.equals(svc.service.getPackageName())) {
                        context.stopService(new Intent().setComponent(svc.service));
                        CrashLogger.log(context, TAG, "🧯 Killed service: " + svc.service.getClassName());
                    }
                }
            }
        } catch (Exception e) {
            CrashLogger.log(context, TAG, "❌ killWatu() exception: " + e.getMessage());
        }
    }

    private static boolean isWatuRunning(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                for (ActivityManager.RunningAppProcessInfo proc : am.getRunningAppProcesses()) {
                    if (proc.processName.equals(TARGET_PACKAGE)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            CrashLogger.log(context, TAG, "⚠️ isWatuRunning() error: " + e.getMessage());
        }
        return false;
    }

    private static void logWatuStatus(Context context, String prefix) {
        boolean isRunning = isWatuRunning(context);
        boolean isTop = isTopActivityWatu(context);
        CrashLogger.log(context, TAG, prefix + " — running=" + isRunning + ", foreground=" + isTop);
    }

    private static boolean isTopActivityWatu(Context context) {
        try {
            long now = System.currentTimeMillis();
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return false;

            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 10000, now);
            if (stats == null || stats.isEmpty()) return false;

            UsageStats latest = null;
            for (UsageStats s : stats) {
                if (latest == null || s.getLastTimeUsed() > latest.getLastTimeUsed()) {
                    latest = s;
                }
            }

            String top = latest != null ? latest.getPackageName() : null;
            CrashLogger.log(context, TAG, "👁️ Foreground app: " + top);
            return TARGET_PACKAGE.equals(top);

        } catch (Exception e) {
            CrashLogger.log(context, TAG, "⚠️ isTopActivityWatu error: " + e.getMessage());
            return false;
        }
    }

    private static void injectSmartSuppression(Context context) {
        softBypassKeyguard(context);
        monitorOverlay(context);
    }

    private static void softBypassKeyguard(Context context) {
        try {
            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && km.isKeyguardLocked()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    KeyguardManager.KeyguardLock kl = km.newKeyguardLock("SmartBypass");
                    kl.disableKeyguard();
                    CrashLogger.log(context, TAG, "🔓 Deprecated keyguard bypass used");
                } else {
                    CrashLogger.log(context, TAG, "🔐 API >= 26 — keyguard locked, no bypass");
                }
            }
        } catch (Exception e) {
            CrashLogger.log(context, TAG, "⚠️ softBypassKeyguard exception: " + e.getMessage());
        }
    }

    private static void monitorOverlay(Context context) {
        try {
            Class<?> blockerClass = Class.forName("com.system.guardian.OverlayBlocker");
            boolean showing = Boolean.TRUE.equals(blockerClass.getMethod("isShowing").invoke(null));

            if (!showing) {
                blockerClass.getMethod("show", Context.class).invoke(null, context);
                CrashLogger.log(context, TAG, "🧱 Overlay manually re-enabled");
            }
        } catch (Exception e) {
            CrashLogger.log(context, TAG, "⚠️ monitorOverlay failed: " + e.getMessage());
        }
    }
}
