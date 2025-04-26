# Path to your built APK (adjust if different)
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"

# Package name from manifest or source
$package = "com.system.guardian"

Write-Host "`n📦 Reinstalling app on emulator..."
adb uninstall $package | Out-Null
adb install -r $apkPath

Write-Host "✅ App reinstalled."

Write-Host "`n🔐 Granting runtime permissions..."
$permissions = @(
    "android.permission.DISABLE_KEYGUARD",
    "android.permission.KILL_BACKGROUND_PROCESSES",
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_LOCATION",
    "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
    "android.permission.RECEIVE_BOOT_COMPLETED",
    "android.permission.INTERNET",
    "android.permission.REQUEST_INSTALL_PACKAGES",
    "android.permission.BLUETOOTH_CONNECT",
    "android.permission.READ_EXTERNAL_STORAGE"
)

foreach ($perm in $permissions) {
    adb shell pm grant $package $perm
    Write-Host "✅ Granted: $perm"
}

Write-Host "`n⚠️ Requesting special permissions manually..."

# SYSTEM_ALERT_WINDOW (requires user interaction via intent)
adb shell am start -a android.settings.ACTION_MANAGE_OVERLAY_PERMISSION -d "package:$package"

# USAGE_STATS (requires user interaction via settings)
adb shell am start -a android.settings.USAGE_ACCESS_SETTINGS

Write-Host "`n🚀 Launching app..."
adb shell monkey -p $package -c android.intent.category.LAUNCHER 1

Write-Host "`n🎯 Emulator test init complete. Watch your emulator screen."
