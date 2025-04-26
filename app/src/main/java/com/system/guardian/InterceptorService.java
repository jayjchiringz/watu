package com.system.guardian;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.system.guardian.core.LogUploader;
import com.system.guardian.core.RemoteControlService;

import java.io.IOException;
import java.util.List;

public class InterceptorService extends AccessibilityService {

    private static final String TARGET_PKG = "com.watuke.app";
    private static final boolean DEBUG_MODE = false;

    private boolean wasWatuAlive = false;
    private final Handler watchdogHandler = new Handler(Looper.getMainLooper());
    private boolean suppressionInProgress = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        final String packageName = event.getPackageName().toString();
        if (!packageName.equals(TARGET_PKG) || suppressionInProgress) return;

        suppressionInProgress = true;

        RemoteControlService.checkGuardianStatus(this, isEnabled -> {
            final String logPrefix = "🎯 Interceptor Decision —";

            String decisionLog = logPrefix + " isEnabled=" + isEnabled +
                    ", useLocalOverride=" + GuardianStateCache.useLocalOverride +
                    ", localOverrideValue=" + GuardianStateCache.localOverrideEnabled +
                    ", lastKnown=" + GuardianStateCache.lastKnownState;

            if (!decisionLog.equals(GuardianStateCache.lastLog)) {
                CrashLogger.log(this, "RemoteControl", decisionLog);
                LogUploader.uploadLog(this, decisionLog);
                GuardianStateCache.lastLog = decisionLog;
            }

            if (!isEnabled) {
                suppressionInProgress = false;
                CrashLogger.log(this, "RemoteControl", "🛑 Guardian remotely disabled.");
                GuardianStateCache.lastLog = "guardian-disabled";
                return;
            }

            CrashLogger.log(this, "InterceptorService", "🚩 Watu detected — Suppression starting...");
            boolean overlayShown = OverlayBlocker.show(this);

            if (!overlayShown && !"overlay-failed".equals(GuardianStateCache.lastLog)) {
                CrashLogger.log(this, "OverlayBlocker", "⚠️ Overlay failed to display.");
                GuardianStateCache.lastLog = "overlay-failed";
            }

            killWatu();
            performGlobalAction(GLOBAL_ACTION_BACK);
            performGlobalAction(GLOBAL_ACTION_HOME);

            String suppressMsg = "🚨 Watu suppressed";
            CrashLogger.log(this, "Suppression", suppressMsg);
            LogUploader.uploadLog(this, suppressMsg);

            watchdogHandler.postDelayed(() -> {
                if (isWatuAlive()) {
                    if (!"retry-suppression".equals(GuardianStateCache.lastLog)) {
                        CrashLogger.log(this, "RetryKill", "🔁 Retrying suppression");
                        GuardianStateCache.lastLog = "retry-suppression";
                    }
                    killWatu();
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    OverlayBlocker.show(this);
                }
                suppressionInProgress = false;
            }, 2500);
        });
    }

    private void killWatu() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(TARGET_PKG);
                CrashLogger.log(this, "KillCommand", "🔪 Background kill");
            }

            Runtime.getRuntime().exec("am force-stop " + TARGET_PKG);
            CrashLogger.log(this, "KillCommand", "💀 Shell force-stop sent");

            killWatuServices();
        } catch (IOException e) {
            CrashLogger.log(this, "KillAttempt", "⚠️ Shell kill failed: " + e.getMessage());
        }
    }

    private void killWatuServices() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return;

        List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo service : services) {
            if (TARGET_PKG.equals(service.service.getPackageName())) {
                String name = service.service.getClassName();
                if (!name.equals(GuardianStateCache.lastServiceKill)) {
                    try {
                        stopService(new Intent().setComponent(service.service));
                        CrashLogger.log(this, "ServiceWatch", "💣 Service stopped: " + name);
                        GuardianStateCache.lastServiceKill = name;
                    } catch (Exception ex) {
                        CrashLogger.log(this, "ServiceStopFail", "⚠️ Failed to stop: " + name);
                    }
                }
            }
        }
    }

    private boolean isWatuAlive() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;

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
        if (DEBUG_MODE) {
            CrashLogger.log(this, "InterceptorService", "⚠️ AccessibilityService interrupted");
        }
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

        watchdogHandler.postDelayed(watuWatcher, 3000);
        CrashLogger.log(this, "InterceptorService", "✅ Accessibility Service connected");
        Toast.makeText(this, "System Guardian: Accessibility connected", Toast.LENGTH_LONG).show();
    }

    private final Runnable watuWatcher = new Runnable() {
        @Override
        public void run() {
            boolean currentlyAlive = isWatuAlive();

            if (currentlyAlive && !wasWatuAlive) {
                wasWatuAlive = true;
                CrashLogger.log(getApplicationContext(), "Watchdog", "⚠️ Watu revived");
                killWatu();
                performGlobalAction(GLOBAL_ACTION_HOME);
                OverlayBlocker.show(getApplicationContext());
            } else if (!currentlyAlive && wasWatuAlive) {
                wasWatuAlive = false;
                CrashLogger.log(getApplicationContext(), "Watchdog", "✅ Watu not detected");
                OverlayBlocker.hide(getApplicationContext());
            }

            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && km.isKeyguardLocked()) {
                if (!"lock-screen".equals(GuardianStateCache.lastLog)) {
                    CrashLogger.log(getApplicationContext(), "Keyguard", "🔒 Lock screen");
                    LogUploader.uploadLog(getApplicationContext(), "🔐 Anti-lock defense triggered");
                    GuardianStateCache.lastLog = "lock-screen";
                }
            }

            watchdogHandler.postDelayed(this, 5000);
        }
    };
}
