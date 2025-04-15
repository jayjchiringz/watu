package com.system.guardian;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.List;

public class InterceptorService extends AccessibilityService {

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

        // Detect Watu and suppress
        if (packageName.equals("com.watuke.app")) {
            CrashLogger.log(this, "InterceptorService", "🛑 Watu detected - trying to kill");

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
        super.onServiceConnected();
        CrashLogger.log(this, "InterceptorService", "Accessibility Service CONNECTED");
        Toast.makeText(this, "System Guardian: Accessibility connected", Toast.LENGTH_LONG).show();
    }
}
