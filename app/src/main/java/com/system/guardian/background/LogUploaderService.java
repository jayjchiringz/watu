package com.system.guardian.background;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.app.JobIntentService;

import com.system.guardian.CrashLogger;

import org.jspecify.annotations.NonNull;

public class LogUploaderService extends JobIntentService {

    private static final String TAG = "LogUploaderService";
    private static final int JOB_ID = 1001;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, LogUploaderService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i(TAG, "🚀 Upload service started");

        try {
            CrashLogger.flush(getApplicationContext());
            Log.i(TAG, "✅ Log upload complete");
        } catch (Exception e) {
            Log.e(TAG, "❌ Log upload failed", e);
        }
    }
}
