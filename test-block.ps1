FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:buildPatchJarDebug'.
> Compilation failed; see the compiler output below.
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\firebase\GuardianMessagingService.java:3: error: static import only from classes and interfaces
  import static android.content.Context.DEVICE_POLICY_SERVICE;
  ^
  Note: Recompile with -Xlint:deprecation for details.
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\firebase\GuardianMessagingService.java:33: error: cannot access Service
  public class GuardianMessagingService extends FirebaseMessagingService {
                                                ^
    class file for android.app.Service not found
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\CrashLogger.java:13: error: cannot find symbol
      public static void log(Context context, String tag, String msg) {
                             ^
    symbol:   class Context
    location: class CrashLogger
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\CrashLogger.java:17: error: cannot find symbol
      public static void log(Context context, String tag, String msg, int level) {
                             ^
    symbol:   class Context
    location: class CrashLogger
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\CrashLogger.java:46: error: cannot find symbol
      public static void flush(Context context) {
                               ^
    symbol:   class Context
    location: class CrashLogger
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\OverlayBlocker.java:19: error: cannot find symbol
      private static View overlayView;
                     ^
    symbol:   class View
    location: class OverlayBlocker
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\OverlayBlocker.java:23: error: cannot find symbol
      public static void ensureOverlay(Context context) {
                                       ^
    symbol:   class Context
    location: class OverlayBlocker
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\OverlayBlocker.java:35: error: cannot find symbol
      public static void show(Context context) {
                              ^
    symbol:   class Context
    location: class OverlayBlocker
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\OverlayBlocker.java:82: error: cannot find symbol
      public static void hide(Context context) {
                              ^
    symbol:   class Context
    location: class OverlayBlocker
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\DexLoader.java:22: error: cannot find symbol
      public static void schedulePatchLoad(Context context, File dexFile) {
                                           ^
    symbol:   class Context
    location: class DexLoader
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\DexLoader.java:26: error: cannot find symbol
      public static void schedulePatchLoad(Context context, File dexFile, boolean force) {
                                           ^
    symbol:   class Context
    location: class DexLoader
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\DexLoader.java:30: error: cannot find symbol
      private static void loadAndPatch(Context context, File dexFile, boolean force) {
                                       ^
    symbol:   class Context
    location: class DexLoader
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\NetworkUtils.java:30: error: cannot find symbol
      public static JSONObject getJsonFromUrl(String urlStr, Context context) {
                                                             ^
    symbol:   class Context
    location: class NetworkUtils
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\NetworkUtils.java:30: error: cannot find symbol
      public static JSONObject getJsonFromUrl(String urlStr, Context context) {
                    ^
    symbol:   class JSONObject
    location: class NetworkUtils
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\NetworkUtils.java:103: error: cannot find symbol
      public static File downloadFile(Context context, String fileUrl, String filename) throws Exception {
                                      ^
    symbol:   class Context
    location: class NetworkUtils
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\NetworkUtils.java:144: error: cannot find symbol
      private static String getDeviceToken(Context context) {
                                           ^
    symbol:   class Context
    location: class NetworkUtils
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\NetworkUtils.java:148: error: cannot find symbol
      public static void sendJsonToServer(String url, JSONObject payload) throws IOException {
                                                      ^
    symbol:   class JSONObject
    location: class NetworkUtils
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\core\LogUploader.java:20: error: cannot find symbol
      public static void uploadLog(Context context, String logText) {
                                   ^
    symbol:   class Context
    location: class LogUploader
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\core\LogUploader.java:42: error: cannot find symbol
      private static boolean attemptUpload(Context context, String logText) {
                                           ^
    symbol:   class Context
    location: class LogUploader
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\core\LogUploader.java:79: error: cannot find symbol
      private static void queueLog(Context context, String logText) {
                                   ^
    symbol:   class Context
    location: class LogUploader
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\core\LogUploader.java:88: error: cannot find symbol
      public static void processQueue(Context context) {
                                      ^
    symbol:   class Context
    location: class LogUploader
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\firebase\GuardianMessagingService.java:19: error: cannot find symbol
  import com.system.guardian.AdminReceiver;
                            ^
    symbol:   class AdminReceiver
    location: package com.system.guardian
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\firebase\GuardianMessagingService.java:20: error: cannot find symbol
  import com.system.guardian.ControlPollerWorker;
                            ^
    symbol:   class ControlPollerWorker
    location: package com.system.guardian
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\dex_patch_build\PatchOverride.java:29: error: cannot find symbol
      private static final Handler retryHandler = new Handler(Looper.getMainLooper());
                           ^
    symbol:   class Handler
    location: class PatchOverride
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\dex_patch_build\PatchOverride.java:35: error: cannot find symbol
      public static void applyPatch(Context context) {
                                    ^
    symbol:   class Context
    location: class PatchOverride
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\dex_patch_build\PatchOverride.java:85: error: cannot find symbol
      private static void killWatu(Context context) {
                                   ^
    symbol:   class Context
    location: class PatchOverride
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\dex_patch_build\PatchOverride.java:99: error: cannot find symbol
      private static void scanRunningServices(Context context) {
                                              ^
    symbol:   class Context
    location: class PatchOverride
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\dex_patch_build\PatchOverride.java:117: error: cannot find symbol
      private static void scanActiveNotifications(Context context) {
                                                  ^
    symbol:   class Context
    location: class PatchOverride
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\dex_patch_build\PatchOverride.java:138: error: cannot find symbol
      private static void monitorKeyguardStatus(Context context) {
                                                ^
    symbol:   class Context
    location: class PatchOverride
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\dex_patch_build\PatchOverride.java:151: error: cannot find symbol
      private static void logRunningProcesses(Context context) {
                                              ^
    symbol:   class Context
    location: class PatchOverride
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\dex_patch_build\PatchOverride.java:168: error: cannot find symbol
      private static String getTopApp(Context context) {
                                      ^
    symbol:   class Context
    location: class PatchOverride
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\dex_patch_build\PatchOverride.java:190: error: cannot find symbol
      private static void logWatuStatus(Context context, String prefix) {
                                        ^
    symbol:   class Context
    location: class PatchOverride
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\dex_patch_build\PatchOverride.java:208: error: cannot find symbol
      private static boolean isWatuRunning(Context context) {
                                           ^
    symbol:   class Context
    location: class PatchOverride
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\dex_patch_build\PatchOverride.java:224: error: cannot find symbol
      private static boolean isTopActivityWatu(Context context) {
                                               ^
    symbol:   class Context
    location: class PatchOverride
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\OverlayBlocker.java:18: error: cannot find symbol
      @SuppressLint("StaticFieldLeak")
       ^
    symbol:   class SuppressLint
    location: class OverlayBlocker
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\OverlayBlocker.java:34: error: cannot find symbol
      @SuppressLint("ObsoleteSdkInt")
       ^
    symbol:   class SuppressLint
    location: class OverlayBlocker
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\NetworkUtils.java:143: error: cannot find symbol
      @SuppressLint("HardwareIds")
       ^
    symbol:   class SuppressLint
    location: class NetworkUtils
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\CrashLogger.java:14: error: cannot find symbol
          log(context, tag, msg, Log.INFO); // Default
                                 ^
    symbol:   variable Log
    location: class CrashLogger
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\CrashLogger.java:20: error: cannot find symbol
          Log.println(level, "GuardianLogger", fullMsg.trim());
          ^
    symbol:   variable Log
    location: class CrashLogger
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\CrashLogger.java:24: error: cannot find symbol
                  FileOutputStream fos = context.openFileOutput("crashlog.txt", Context.MODE_APPEND);
                                                                                ^
    symbol:   variable Context
    location: class CrashLogger
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\CrashLogger.java:32: error: cannot find symbol
                  Log.e("GuardianLogger", "Γ¥î Logging failed", e);
                  ^
    symbol:   variable Log
    location: class CrashLogger
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\CrashLogger.java:50: error: cannot find symbol
                  Log.d("GuardianLogger", "≡ƒôñ Log queue flush complete");
                  ^
    symbol:   variable Log
    location: class CrashLogger
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\CrashLogger.java:52: error: cannot find symbol
                  Log.e("GuardianLogger", "Γ¥î Flush failed", e);
                  ^
    symbol:   variable Log
    location: class CrashLogger
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\OverlayBlocker.java:24: error: cannot find symbol
          if (!Settings.canDrawOverlays(context)) {
               ^
    symbol:   variable Settings
    location: class OverlayBlocker
  C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main\app\src\main\java\com\system\guardian\OverlayBlocker.java:26: error: cannot find symbol
                  CrashLogger.log(context, "OverlayBlocker", "Γ¥î Missing overlay permission. Aborting overlay.", Log.WARN);
                                                                                                                ^
    symbol:   variable Log
    location: class OverlayBlocker
  Note: Some input files use or override a deprecated API.
  Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
  100 errors

* Try:
> Check your code and dependencies to fix the compilation error(s)
> Run with --scan to get full insights.

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD FAILED in 42s
15 actionable tasks: 4 executed, 11 from cache
PS C:\Users\dell\Downloads\Compressed\interceptor_final_project_2\interceptor_fixed_with_main>