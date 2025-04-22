# PowerShell Script to Grant Valid Runtime Permissions to the Guardian App via ADB
$PACKAGE = "com.system.guardian"

Write-Output "Granting valid runtime permissions to $PACKAGE..."

# Only runtime permissions (the rest are auto-granted from manifest)
adb shell pm grant $PACKAGE android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant $PACKAGE android.permission.BLUETOOTH_CONNECT

Write-Output ""
Write-Output "Done Granted runtime permissions"
adb shell dumpsys package $PACKAGE | Select-String 'granted=true' | Select-String $PACKAGE
