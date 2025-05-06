package com.system.guardian;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

/**
 * Legacy-named service. Internally forwards work to ControlPollerWorker (WorkManager).
 * Avoids deprecated JobIntentService behavior entirely.
 */
public class ControlPollerService extends JobIntentService {

    private static final int JOB_ID = 1001;

    // Kept for compatibility — still used by external components
    public static void enqueueWork(Context context, Intent work) {
        // No JobIntentService behavior used inside — just proxy to WorkManager
        WorkRequest workRequest = new OneTimeWorkRequest.Builder(ControlPollerWorker.class).build();
        WorkManager.getInstance(context).enqueue(workRequest);

        CrashLogger.log(context, "ControlPollerService", "✅ Enqueued ControlPollerWorker via WorkManager");
    }

    // Never actually called unless someone uses startService directly (rare)
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Context context = getApplicationContext();
        CrashLogger.log(context, "ControlPollerService", "⚠️ onHandleWork() should not be called. Use enqueueWork()");

        // Fail-safe fallback to Worker
        WorkRequest workRequest = new OneTimeWorkRequest.Builder(ControlPollerWorker.class).build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }
}
