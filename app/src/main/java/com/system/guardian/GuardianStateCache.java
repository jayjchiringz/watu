package com.system.guardian;

public class GuardianStateCache {
    public static boolean lastKnownState = true;
    public static long lastCheckedTime = 0;

    public static boolean localOverrideEnabled = true;
    public static boolean useLocalOverride = false;

    // 🔁 New: log suppression helpers
    public static String lastLog = "";            // General last log tag
    public static String lastServiceKill = "";    // Last service killed
    public static long lastSuppressionTime = 0L;  // For timing-based throttles if needed
}
