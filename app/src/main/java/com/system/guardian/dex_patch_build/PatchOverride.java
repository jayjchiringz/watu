package com.system.guardian.dex_patch_build;

import android.content.Context;
import android.widget.Toast;

public class PatchOverride {
    public static void applyPatch(Context context) {
        Toast.makeText(context, "✅ Minimal runtime patch active", Toast.LENGTH_LONG).show();
    }
}
