package com.system.guardian;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.content.Intent;
import android.net.Uri;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.List;

public class InterceptorService extends AccessibilityService {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();

        // Log all event types
        CrashLogger.log(this, "EventType", "Event Type: " + event.getEventType());

        // Log background events
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED ||
                event.getEventType() == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            CrashLogger.log(this, "BackgroundEvent", "Package: " + packageName + ", Event: " + event.toString());
        }

        // Detect Watu UI via window class
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String className = event.getClassName() != null ? event.getClassName().toString() : "(unknown)";
            if (packageName.equals("com.watuke.app")) {
                CrashLogger.log(this, "WindowCheck", "Watu visible: " + className);
                performGlobalAction(GLOBAL_ACTION_BACK);
                OverlayBlocker.show(this);
            }
        }

        // Detect Watu package activity (any event)
        if (packageName.equals("com.watuke.app")) {
            CrashLogger.log(this, "InterceptorService", "🚩 Watu detected - attempting suppression");

            OverlayBlocker.show(this);

            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            am.killBackgroundProcesses("com.watuke.app");

            performGlobalAction(GLOBAL_ACTION_BACK);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                ActivityManager am2 = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> procs = am2.getRunningAppProcesses();
                boolean stillRunning = false;

                for (ActivityManager.RunningAppProcessInfo p : procs) {
                    if (p.processName.equals("com.watuke.app")) {
                        stillRunning = true;
                        break;
                    }
                }

                if (stillRunning) {
                    CrashLogger.log(this, "ProcessCheck", "⚠️ Watu still alive after kill attempt");
                } else {
                    CrashLogger.log(this, "ProcessCheck", "✅ Watu process no longer found");
                }
            }, 3000);
        }
    }

    @Override
    public void onInterrupt() {
        CrashLogger.log(this, "InterceptorService", "Service interrupted.");
    }

    @Override
    protected void onServiceConnected() {
        // Check overlay permission at startup
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
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();

            boolean isWatuRunning = false;

            for (ActivityManager.RunningAppProcessInfo p : procs) {
                if (p.processName.equals("com.watuke.app")) {
                    isWatuRunning = true;
                    break;
                }
            }

            if (isWatuRunning) {
                CrashLogger.log(getApplicationContext(), "Watchdog", "⚠️ Watu revived — re-killing now");
                am.killBackgroundProcesses("com.watuke.app");
                performGlobalAction(GLOBAL_ACTION_BACK);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    OverlayBlocker.show(getApplicationContext());
                }
            } else {
                CrashLogger.log(getApplicationContext(), "Watchdog", "✅ Watu not detected in memory");
                OverlayBlocker.hide(getApplicationContext());
            }

            // Repeat check every 5 seconds
            new Handler(Looper.getMainLooper()).postDelayed(this, 5000);
        }
    };
}
