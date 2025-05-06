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

    private final Handler watchdogHandler = new Handler(Looper.getMainLooper());
    private boolean wasWatuAlive = false;
    private boolean suppressionInProgress = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();

        if (packageName.contains("com.watuke")) {
            handlePotentialInjection();
        }

        if (!packageName.equals(TARGET_PKG) || suppressionInProgress) return;

        suppressionInProgress = true;

        RemoteControlService.checkGuardianStatus(getApplicationContext(), isEnabled -> {
            if (!isEnabled) {
                suppressionInProgress = false;
                return;
            }

            if (GuardianStateCache.isGhostModeEnabled) {
                handleGhostMode();
            } else {
                handleSuppressionMode();
            }
        });
    }

    private void handlePotentialInjection() {
        CrashLogger.log(getApplicationContext(), "Interceptor", "Watu app active, injecting overlay/job");
        // TODO: Replace with actual job logic if implemented
        // triggerJob();
    }

    private void handleGhostMode() {
        CrashLogger.log(getApplicationContext(), "GhostInterceptor", "👻 GhostMode Active — Redirect Only");
        performGlobalAction(GLOBAL_ACTION_BACK);
        performGlobalAction(GLOBAL_ACTION_HOME);
        watchdogHandler.postDelayed(() -> suppressionInProgress = false, 1000);
    }

    private void handleSuppressionMode() {
        CrashLogger.log(getApplicationContext(), "InterceptorService", "🚨 Normal suppression mode — Killing...");
        killWatu();
        performGlobalAction(GLOBAL_ACTION_HOME);
        watchdogHandler.postDelayed(() -> suppressionInProgress = false, 2500);
    }

    private void killWatu() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(TARGET_PKG);
                CrashLogger.log(getApplicationContext(), "KillCommand", "🔪 Background kill");
            }

            Runtime.getRuntime().exec("am force-stop " + TARGET_PKG);
            CrashLogger.log(getApplicationContext(), "KillCommand", "💀 Shell force-stop sent");

            killWatuServices();
        } catch (IOException e) {
            CrashLogger.log(getApplicationContext(), "KillAttempt", "⚠️ Shell kill failed: " + e.getMessage());
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
                        CrashLogger.log(getApplicationContext(), "ServiceWatch", "💣 Service stopped: " + name);
                        GuardianStateCache.lastServiceKill = name;
                    } catch (Exception ex) {
                        CrashLogger.log(getApplicationContext(), "ServiceStopFail", "⚠️ Failed to stop: " + name);
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
            CrashLogger.log(getApplicationContext(), "InterceptorService", "⚠️ AccessibilityService interrupted");
        }
    }

    @Override
    protected void onServiceConnected() {
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        watchdogHandler.postDelayed(watuWatcher, 3000);
        CrashLogger.log(getApplicationContext(), "InterceptorService", "✅ Accessibility Service connected");
        Toast.makeText(getApplicationContext(), "System Guardian: Accessibility connected", Toast.LENGTH_LONG).show();
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

            handleKeyguardEvents();

            watchdogHandler.postDelayed(this, 5000);
        }
    };

    private void handleKeyguardEvents() {
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km != null && km.isKeyguardLocked()) {
            if (!"lock-screen".equals(GuardianStateCache.lastLog)) {
                CrashLogger.log(getApplicationContext(), "Keyguard", "🔒 Lock screen");
                LogUploader.uploadLog(getApplicationContext(), "🔐 Anti-lock defense triggered");
                GuardianStateCache.lastLog = "lock-screen";
            }
        }
    }
}
