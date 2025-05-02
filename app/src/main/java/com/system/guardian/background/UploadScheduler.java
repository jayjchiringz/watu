package com.system.guardian.background;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class UploadScheduler {

    public static final String UPLOAD_WORK_NAME = "PeriodicLogUpload";

    public static void schedulePeriodicLogUpload(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest logUploadWork = new PeriodicWorkRequest.Builder(
                LogUploadWorker.class,
                15, TimeUnit.MINUTES // 📌 Minimum allowed by Android
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UPLOAD_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                logUploadWork
        );
    }
}
