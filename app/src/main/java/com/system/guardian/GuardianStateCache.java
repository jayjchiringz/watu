package com.system.guardian;

public class GuardianStateCache {
    public static boolean lastKnownState = true;
    public static long lastCheckedTime = 0;

    public static boolean localOverrideEnabled = true; // default ON
    public static boolean useLocalOverride = false;     // disabled by default
}
