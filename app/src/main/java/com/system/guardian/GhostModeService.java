package com.system.guardian;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.system.guardian.CrashLogger;

public class GhostModeService extends Service {
    private static final String TAG = "GhostModeService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CrashLogger.log(this, TAG, "👻 GhostMode Activated: Watching silently...");

        // Prevent Firebase/Watu from launching background jobs
        Blocker.disableFirebaseComponents(getApplicationContext());

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void start(Context context) {
        context.startService(new Intent(context, GhostModeService.class));
    }
}
