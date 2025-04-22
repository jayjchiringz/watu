package com.system.guardian.dex_patch_build;

import android.content.Context;
import android.widget.Toast;

import com.system.guardian.CrashLogger;

public class PatchOverride {
    public static void applyPatch(Context context) {
        Toast.makeText(context, "✅ Minimal runtime patch active", Toast.LENGTH_LONG).show();
        CrashLogger.log(context, "PatchOverride", "✅ Minimal runtime patch applied");
        CrashLogger.flush(context); // 🔄 Ensure upload happens now
    }
}
