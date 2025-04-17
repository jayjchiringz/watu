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
import android.widget.Toast;

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
        // ✅ Moved this check INSIDE the method
        if (!RemoteControlService.isGuardianEnabled(this)) {
            CrashLogger.log(this, "RemoteControl", "🛑 Guardian remotely disabled. Exiting.");
            LogUploader.uploadLog(this, "🛑 Guardian remotely disabled. Interceptor exiting.");
            return;
        }

        if (event == null || event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();

        if (packageName.equals(TARGET_PKG)) {
            CrashLogger.log(this, "InterceptorService", "🚩 Watu detected - initiating suppression");

            OverlayBlocker.show(this);
            killWatu();
            performGlobalAction(GLOBAL_ACTION_BACK);

            String logMessage = "Watu suppression triggered at " + System.currentTimeMillis();
            LogUploader.uploadLog(this, "🚨 Watu detected, suppression triggered.");
            CrashLogger.log(this, "RemoteLog", "📡 Log uploaded remotely");
            LogUploader.uploadLog(this, "📡 Log uploaded remotely");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                boolean stillRunning = isWatuAlive();
                CrashLogger.log(this, "ProcessCheck",
                        stillRunning ? "⚠️ Watu still alive after kill" : "✅ Watu suppressed");
            }, 3000);
        }
    }

    private void killWatu() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            am.killBackgroundProcesses(TARGET_PKG);
            Runtime.getRuntime().exec("am force-stop " + TARGET_PKG);
        } catch (IOException e) {
            CrashLogger.log(this, "KillAttempt", "⚠️ Failed to force-stop via shell: " + e.getMessage());
        }
    }

    private boolean isWatuAlive() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo p : procs) {
            if (p.processName.equals(TARGET_PKG)) {
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
                performGlobalAction(GLOBAL_ACTION_BACK);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    OverlayBlocker.show(getApplicationContext());
                }
            } else {
                CrashLogger.log(getApplicationContext(), "Watchdog", "✅ Watu not detected in memory");
                OverlayBlocker.hide(getApplicationContext());
            }

            new Handler(Looper.getMainLooper()).postDelayed(this, 5000);
        }
    };
}
