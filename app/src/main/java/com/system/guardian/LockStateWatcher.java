package com.system.guardian;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class LockStateWatcher {

    private final Context context;
    private final LockReceiver receiver;
    private boolean isRegistered = false;

    public LockStateWatcher(Context context) {
        this.context = context.getApplicationContext(); // Avoid leaks
        this.receiver = new LockReceiver();
    }

    public void start() {
        if (isRegistered) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        context.registerReceiver(receiver, filter);
        isRegistered = true;
        CrashLogger.log(context, "LockStateWatcher", "🔄 Started lock state watcher");
    }

    public void stop() {
        if (!isRegistered) return;
        context.unregisterReceiver(receiver);
        isRegistered = false;
        CrashLogger.log(context, "LockStateWatcher", "⏹️ Stopped lock state watcher");
    }

    private class LockReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                CrashLogger.log(context, "LockStateWatcher", "🔒 Screen off detected (lock)");
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                boolean locked = isDeviceLocked(context);
                if (!locked) {
                    CrashLogger.log(context, "LockStateWatcher", "🔓 User present — device unlocked");
                    // Optional: trigger ControlPollerService.enqueueWork(context, new Intent(context, ControlPollerService.class));
                } else {
                    CrashLogger.log(context, "LockStateWatcher", "🔐 User present but still locked");
                }
            }
        }
    }

    private boolean isDeviceLocked(Context ctx) {
        KeyguardManager km = (KeyguardManager) ctx.getSystemService(Context.KEYGUARD_SERVICE);
        return km != null && km.isKeyguardLocked();
    }
}
