package com.system.guardian.dex_patch_build;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class PatchOverride {
    public static void applyPatch(Context context) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "✅ Patch applied successfully", Toast.LENGTH_LONG).show();
        });
    }
}
