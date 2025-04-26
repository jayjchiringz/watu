# =============================
# PATCH JAR BUILDER (FINAL)
# =============================

Write-Host "`n=== Starting Patch JAR Build ===`n"

# Paths
$projectRoot = Get-Location
$patchSrc = "app\src\main\java\com\system\guardian\dex_patch_build"
$buildOut = "$patchSrc\build\out"
$libsDir = "$projectRoot\app\libs"
$gradleCache = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1"
$androidJar = "C:\Users\dell\AppData\Local\Android\Sdk\platforms\android-35\android.jar"
$appClasses = "$projectRoot\build\intermediates\javac\debug\classes"

# Dependencies (only core + work)
$dependencies = @(
    @{ group = "androidx.core"; name = "core"; version = "1.16.0" },
    @{ group = "androidx.work"; name = "work-runtime"; version = "2.10.0" },
    @{ group = "com.google.guava"; name = "guava"; version = "31.1-android" },
    @{ group = "org.jetbrains.kotlin"; name = "kotlin-stdlib"; version = "1.9.10" },
    @{ group = "androidx.annotation"; name = "annotation"; version = "1.7.1" }
)

# Step 1: Ensure libs dir
if (-not (Test-Path $libsDir)) {
    New-Item -ItemType Directory -Path $libsDir | Out-Null
    Write-Host "Created app/libs"
}

# Step 2: Extract AAR → classes.jar → app/libs
foreach ($dep in $dependencies) {
    $groupPath = $dep.group
    $versionPath = Join-Path (Join-Path (Join-Path $gradleCache $groupPath) $dep.name) $dep.version
    if (-not (Test-Path $versionPath)) {
        Write-Error "❌ Missing: $versionPath"
        continue
    }

    $hashFolder = Get-ChildItem -Directory $versionPath | Select-Object -First 1
    if (-not $hashFolder) {
        Write-Error "❌ No hash folder in: $versionPath"
        continue
    }

    $aarFile = Get-ChildItem -Path $hashFolder.FullName -Filter "*.aar" | Select-Object -First 1
    if (-not $aarFile) {
        Write-Error "❌ No AAR file for $($dep.name)"
        continue
    }

    $extractPath = Join-Path $hashFolder.FullName "extract"
    $zipAar = "$($aarFile.FullName).zip"
    Copy-Item -Path $aarFile.FullName -Destination $zipAar -Force
    Expand-Archive -Force -Path $zipAar -DestinationPath $extractPath
    Remove-Item $zipAar -Force

    $classesJar = Join-Path $extractPath "classes.jar"
    if (Test-Path $classesJar) {
        $dest = Join-Path $libsDir "$($dep.name).jar"
        Copy-Item $classesJar $dest -Force
        Write-Host "✅ Extracted $($dep.name).jar"
    } else {
        Write-Error "❌ No classes.jar in $($dep.name)"
    }
}

# Step 3: Clean build output
Set-Location $patchSrc
if (Test-Path .\build) { Remove-Item -Recurse -Force .\build }
New-Item -ItemType Directory -Path .\build\out | Out-Null

# Step 4: Build classpath
$libJars = Get-ChildItem "$libsDir\*.jar" | ForEach-Object { $_.FullName }
$classpath = "$androidJar;$appClasses;" + ($libJars -join ";")

# Step 5: Compile
$javacArgs = @(
    "-classpath", $classpath,
    "-d", "build\out",
    "..\CrashLogger.java",
    "..\OverlayBlocker.java",
    "..\core\LogUploader.java",
    "..\DexLoader.java",
    "..\ControlPollerService.java",
    "..\ControlPollerWorker.java",
    "..\NetworkUtils.java",
    "..\dex_patch_build\PatchOverride.java",
    "..\dex_patch_build\PatchInstaller.java"
)

Write-Host "Compiling patch classes..."
& javac @javacArgs

if ($LASTEXITCODE -ne 0) {
    Write-Error "❌ Compilation failed"
    exit 1
}

# Step 6: Create patch.jar
Write-Host "Packaging patch.jar..."
jar cf patch.jar -C build\out .

if (Test-Path ".\patch.jar") {
    Write-Host "✅ patch.jar created successfully"
} else {
    Write-Error "❌ Failed to create patch.jar"
    exit 1
}

Write-Host "`n🎯 Patch JAR Build Completed.`n"
