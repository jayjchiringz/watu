package com.system.guardian.background;

import android.content.Context;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.system.guardian.DexLoader;

import org.jspecify.annotations.NonNull;

import java.io.File;

public class BootPatchWorker extends Worker {
    public BootPatchWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            File patch = new File(getApplicationContext().getDir("dex_patch_secure", Context.MODE_PRIVATE), "patch.jar");
            if (patch.exists()) {
                DexLoader.schedulePatchLoad(getApplicationContext(), patch, true); // force reapply
                return Result.success();
            } else {
                return Result.failure(); // No patch found
            }
        } catch (Exception e) {
            return Result.retry(); // Retry in case of failure
        }
    }
}
