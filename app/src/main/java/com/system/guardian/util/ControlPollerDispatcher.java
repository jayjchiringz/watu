package com.system.guardian.util;

import android.content.Context;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.system.guardian.ControlPollerWorker;

/**
 * Dispatches the ControlPollerWorker via WorkManager, replacing deprecated JobIntentService.
 */
public class ControlPollerDispatcher {

    public static void startWorker(Context context) {
        WorkRequest workRequest = new OneTimeWorkRequest.Builder(ControlPollerWorker.class)
                .build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }
}
