package com.system.guardian.background;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.system.guardian.CrashLogger;

public class LogUploadWorker extends Worker {

    private static final String TAG = "LogUploadWorker";

    public LogUploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "🚀 Upload task started");

        try {
            CrashLogger.flush(getApplicationContext());
            Log.i(TAG, "✅ Log upload complete");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "❌ Log upload failed", e);
            return Result.retry();  // Retry on failure
        }
    }
}
