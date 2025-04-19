package com.system.guardian;

import android.app.PendingIntent;
import android.app.PackageInstaller;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class SilentApkInstaller {

    public static void installSilently(Context context, File apkFile) {
        try {
            PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL
            );

            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);

            try (InputStream in = context.getContentResolver().openInputStream(Uri.fromFile(apkFile));
                 OutputStream out = session.openWrite("guardian_update", 0, -1)) {
                byte[] buffer = new byte[65536];
                int c;
                while ((c = in.read(buffer)) != -1) {
                    out.write(buffer, 0, c);
                }
                session.fsync(out);
            }

            Intent intent = new Intent(context, InstallResultReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            session.commit(pendingIntent.getIntentSender());
            session.close();

            Log.i("SilentInstaller", "Silent install session committed.");
        } catch (Exception e) {
            Log.e("SilentInstaller", "Silent install failed", e);
        }
    }
}
