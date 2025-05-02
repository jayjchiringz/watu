package com.system.guardian;

import android.content.Context;
import android.content.Intent;
import com.system.guardian.CrashLogger;
import com.system.guardian.GuardianStateCache;

public class Blocker {

    public static void disableFirebaseComponents(Context context) {
        // Safety check: only disable Firebase if the FCM token has been registered
        if (!GuardianStateCache.isTokenUploaded) {
            CrashLogger.log(context, "Blocker", "🚫 Firebase block skipped — FCM token not uploaded yet.");
            return;
        }

        CrashLogger.log(context, "Blocker", "👻 Disabling Firebase Sync...");
        try {
            // Stop Firebase Messaging service (Note: This affects all components using it)
            context.stopService(new Intent().setAction("com.google.firebase.MESSAGING_EVENT"));
            // TODO: In future, target specific processes if needed
        } catch (Exception e) {
            CrashLogger.log(context, "BlockerError", "⚠️ Error disabling Firebase: " + e.getMessage());
        }
    }
}
