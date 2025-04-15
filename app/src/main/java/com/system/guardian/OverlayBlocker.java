package com.system.guardian;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;

public class OverlayBlocker {
    private static View overlayView;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void show(Context context) {
        if (overlayView != null) return;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        overlayView = new FrameLayout(context);
        overlayView.setBackgroundColor(0x00000000); // Fully transparent

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;

        wm.addView(overlayView, params);
        CrashLogger.log(context, "OverlayBlocker", "🛡️ Invisible shield deployed");
    }

    public static void hide(Context context) {
        if (overlayView != null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(overlayView);
            overlayView = null;
            CrashLogger.log(context, "OverlayBlocker", "🧯 Shield disabled");
        }
    }
}
