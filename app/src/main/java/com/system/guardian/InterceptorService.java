package com.system.guardian;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.net.Uri;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;
import android.app.KeyguardManager;

import androidx.annotation.RequiresApi;

import com.system.guardian.core.LogUploader;
import com.system.guardian.core.RemoteControlService;

import java.io.IOException;
import java.util.List;

public class InterceptorService extends AccessibilityService {

    private static final String TARGET_PKG = "com.watuke.app";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();

        RemoteControlService.checkGuardianStatus(this, isEnabled -> {
            String logPrefix = "🎯 Interceptor Decision —";

            CrashLogger.log(this, "RemoteControl", logPrefix + " isEnabled=" + isEnabled +
                    ", useLocalOverride=" + GuardianStateCache.useLocalOverride +
                    ", localOverrideValue=" + GuardianStateCache.localOverrideEnabled +
                    ", lastKnown=" + GuardianStateCache.lastKnownState);

            LogUploader.uploadLog(this, logPrefix + " isEnabled=" + isEnabled +
                    ", useLocalOverride=" + GuardianStateCache.useLocalOverride +
                    ", lastKnown=" + GuardianStateCache.lastKnownState);

            if (!isEnabled) {
                CrashLogger.log(this, "RemoteControl", "🛑 Guardian remotely disabled. Skipping interception.");
                LogUploader.uploadLog(this, "🛑 Guardian remotely disabled. Interceptor exiting.");
                return;
            }

            if (packageName.equals(TARGET_PKG)) {
                CrashLogger.log(this, "InterceptorService", "🚩 Watu detected — Suppression starting...");

                OverlayBlocker.show(this);
                killWatu();
                performGlobalAction(GLOBAL_ACTION_BACK); // Try backing out
                performGlobalAction(GLOBAL_ACTION_HOME); // Immediately send user to home

                LogUploader.uploadLog(this, "🚨 Watu detected, suppression triggered.");
                CrashLogger.log(this, "Suppression", "📡 Suppression actions applied");

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    boolean stillRunning = isWatuAlive();
                    CrashLogger.log(this, "ProcessCheck",
                            stillRunning ? "⚠️ Watu still alive after kill" : "✅ Watu successfully suppressed");

                    if (stillRunning) {
                        CrashLogger.log(this, "RetryKill", "🔁 Retrying suppression sequence");
                        killWatu();
                        performGlobalAction(GLOBAL_ACTION_HOME);
                        OverlayBlocker.show(this);
                    }
                }, 3000);
            }
        });
    }

    private void killWatu() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            am.killBackgroundProcesses(TARGET_PKG);
            Runtime.getRuntime().exec("am force-stop " + TARGET_PKG);

            CrashLogger.log(this, "KillCommand", "💀 Watu kill sequence executed");

            // Optional: kill lingering services
            killWatuServices();
        } catch (IOException e) {
            CrashLogger.log(this, "KillAttempt", "⚠️ Failed to force-stop via shell: " + e.getMessage());
        }
    }

    private void killWatuServices() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo service : services) {
            if (service.service.getPackageName().equals(TARGET_PKG)) {
                CrashLogger.log(this, "ServiceWatch", "💣 Killing Watu service: " + service.service.getClassName());
                Intent intent = new Intent().setComponent(service.service);
                stopService(intent);
            }
        }
    }

    private boolean isWatuAlive() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo proc : procs) {
            if (proc.processName.equals(TARGET_PKG)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        CrashLogger.log(this, "InterceptorService", "Service interrupted.");
    }

    @Override
    protected void onServiceConnected() {
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launchIntent);

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        new Handler(Looper.getMainLooper()).postDelayed(watuWatcher, 3000);
        super.onServiceConnected();
        CrashLogger.log(this, "InterceptorService", "Accessibility Service CONNECTED");
        Toast.makeText(this, "System Guardian: Accessibility connected", Toast.LENGTH_LONG).show();
    }

    private final Runnable watuWatcher = new Runnable() {
        @Override
        public void run() {
            if (isWatuAlive()) {
                CrashLogger.log(getApplicationContext(), "Watchdog", "⚠️ Watu revived — re-killing now");
                killWatu();
                performGlobalAction(GLOBAL_ACTION_HOME);
                OverlayBlocker.show(getApplicationContext());
            } else {
                CrashLogger.log(getApplicationContext(), "Watchdog", "✅ Watu not detected in memory");
                OverlayBlocker.hide(getApplicationContext());
            }

            // 🔒 Lock detection
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && km.isKeyguardLocked()) {
                CrashLogger.log(getApplicationContext(), "Keyguard", "🔒 Lock screen detected");
                OverlayBlocker.show(getApplicationContext());
                LogUploader.uploadLog(getApplicationContext(), "🔐 Anti-lock defense triggered");
            }

            new Handler(Looper.getMainLooper()).postDelayed(this, 5000);
        }
    };
}
