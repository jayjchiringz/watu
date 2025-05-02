package com.system.guardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.system.guardian.background.BootPatchWorker;
import com.system.guardian.background.LogUploadWorker;
import com.system.guardian.background.UploadScheduler;
import com.system.guardian.core.LogUploader;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                // Start core services
                context.startService(new Intent(context, InterceptorService.class));

                // Schedule the log upload immediately on boot
                OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(LogUploadWorker.class).build();
                WorkManager.getInstance(context).enqueue(uploadRequest);

                OneTimeWorkRequest patchRequest = new OneTimeWorkRequest.Builder(BootPatchWorker.class).build();
                WorkManager.getInstance(context).enqueue(patchRequest);

                // Schedule periodic uploads
                UploadScheduler.schedulePeriodicLogUpload(context);

                LogUploader.uploadLog(context, "[BOOT] 🛠️ Device rebooted - services initialized.");
            } catch (Exception e) {
                LogUploader.uploadLog(context, "[BOOT-ERROR] ❌ Failed to start services: " + e.getMessage());
            }
        }
    }
}
