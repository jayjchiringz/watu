package com.system.guardian;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.system.guardian.core.LogUploader;

public class OverlayBlocker {
    @SuppressLint("StaticFieldLeak")
    private static View overlayView;

    public static void show(Context context) {
        if (overlayView != null) return;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        overlayView = new FrameLayout(context);
        overlayView.setBackgroundColor(0x00000000); // fully transparent

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;

        wm.addView(overlayView, params);
        CrashLogger.log(context, "OverlayBlocker", "🛡️ Invisible shield deployed");
        LogUploader.uploadLog(context, "🛡️ OverlayBlocker activated");
    }

    public static void hide(Context context) {
        if (overlayView != null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(overlayView);
            overlayView = null;
            CrashLogger.log(context, "OverlayBlocker", "🧯 Shield disabled");
            LogUploader.uploadLog(context, "🧯 OverlayBlocker deactivated");
        }
    }
}
