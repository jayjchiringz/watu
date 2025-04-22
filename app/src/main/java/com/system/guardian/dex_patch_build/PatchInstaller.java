package com.system.guardian.dex_patch_build;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.io.File;

public class PatchInstaller {
    public static void install(Context context, File apkFile) {
        try {
            Uri apkUri = Uri.fromFile(apkFile); // Less secure, but works for testing
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            System.out.println("✅ PatchInstaller installed update.apk");

        } catch (Exception e) {
            System.err.println("❌ PatchInstaller error: " + e.getMessage());
        }
    }
}
