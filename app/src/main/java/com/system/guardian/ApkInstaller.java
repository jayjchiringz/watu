package com.system.guardian;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Utility for installing APKs via FileProvider
 */
public class ApkInstaller {

    public static void installApk(Context context, File apkFile) {
        if (apkFile == null || !apkFile.exists()) return;

        Uri apkUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                apkFile
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
