package com.system.guardian.firebase;

import static android.app.admin.DevicePolicyManager.*;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.system.guardian.AdminReceiver;
import com.system.guardian.ControlPollerWorker;
import com.system.guardian.CrashLogger;
import com.system.guardian.core.LogUploader;

import java.util.Objects;

public class GuardianMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        CrashLogger.log(this, "FCM", "🔄 FCM Token refreshed: " + token);

        // Optional: Send token to your server
        LogUploader.uploadLog(this, "FCM token updated: " + token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        CrashLogger.log(this, "FirebaseMsg", "📩 Message received: " + remoteMessage.getData());
        LogUploader.uploadLog(this, "📩 Firebase message received: " + remoteMessage.getData());

        if (remoteMessage.getData().containsKey("lock") &&
                Objects.equals(remoteMessage.getData().get("lock"), "true")) {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = new ComponentName(this, AdminReceiver.class);

            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow();
                CrashLogger.log(this, "FirebaseLock", "🔒 Device locked via Firebase command.");
                LogUploader.uploadLog(this, "🔒 Device locked via FCM");
            }
        }

        if ("true".equals(remoteMessage.getData().get("apk_update"))) {
            CrashLogger.log(this, "FirebaseMsg", "🚀 APK update triggered from dashboard");
            LogUploader.uploadLog(this, "🚀 APK update trigger received via FCM");
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ControlPollerWorker.class).build();
            WorkManager.getInstance(this).enqueue(request);
        }
    }
}
