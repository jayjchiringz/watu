Write-Host "`n=== Starting Patch JAR Build ===`n"

$projectRoot = Get-Location
$patchSrc = "app\src\main\java\com\system\guardian\dex_patch_build"
$buildOut = "$patchSrc\build\out"
$libsDir = "$projectRoot\app\libs"
$gradleCache = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1"
$androidJar = Join-Path $env:LOCALAPPDATA "Android\Sdk\platforms\android-35\android.jar"
$appClasses = "$projectRoot\app\build\intermediates\javac\debug\compileDebugJavaWithJavac\classes"

$dependencies = @()
$dependencies += @{ group = "androidx.core"; name = "core"; version = "1.16.0" }
$dependencies += @{ group = "androidx.work"; name = "work-runtime"; version = "2.10.0" }
$dependencies += @{ group = "androidx.annotation"; name = "annotation"; version = "1.9.1" }
$dependencies += @{ group = "com.squareup.okhttp3"; name = "okhttp"; version = "4.10.0" }
$dependencies += @{ group = "com.squareup.okio"; name = "okio"; version = "3.0.0" }
$dependencies += @{ group = "com.google.firebase"; name = "firebase-messaging"; version = "24.1.1" }

if (-not (Test-Path $androidJar)) {
    Write-Error "android.jar not found: $androidJar"
    exit 1
}

if (-not (Test-Path $appClasses)) {
    Write-Error "App classes not found: $appClasses"
    exit 1
}

if (-not (Test-Path $libsDir)) {
    New-Item -ItemType Directory -Path $libsDir | Out-Null
    Write-Host "Created app/libs"
}

foreach ($dep in $dependencies) {
    $libName = "$($dep.name).jar"
    $libPath = Join-Path $libsDir $libName

    if (Test-Path $libPath) {
        Write-Host "✅ Found pre-downloaded $libName in libs — skipping extraction."
        continue
    }

    $aarFilename = "$($dep.name)-$($dep.version).aar"
    $aarPath = Join-Path $libsDir $aarFilename

    if (Test-Path $aarPath) {
        Write-Host "📦 Found AAR $aarFilename in libs — extracting..."
        $extractPath = Join-Path $libsDir "$($dep.name)-extract"
        if (Test-Path $extractPath) {
            Remove-Item -Recurse -Force $extractPath
        }

        Copy-Item -Path $aarPath -Destination "$aarPath.zip" -Force
        Expand-Archive -Force -Path "$aarPath.zip" -DestinationPath $extractPath
        Remove-Item "$aarPath.zip" -Force

        $classesJar = Join-Path $extractPath "classes.jar"
        if (Test-Path $classesJar) {
            Copy-Item $classesJar $libPath -Force
            Write-Host "✅ Extracted $libName from AAR."
        } else {
            Write-Error "❌ classes.jar not found in $aarFilename"
        }

        continue
    }

    $groupPath = $dep.group -replace "\.", "\\"
    $versionPath = Join-Path (Join-Path (Join-Path $gradleCache $groupPath) $dep.name) $dep.version
    Write-Host "🔍 Checking Gradle cache: $versionPath"

    if (-not (Test-Path $versionPath)) {
        Write-Error "❌ Missing Gradle cache: $versionPath"
        continue
    }

    $hashFolder = Get-ChildItem -Directory $versionPath | Select-Object -First 1
    if (-not $hashFolder) {
        Write-Error "❌ No hash folder in: $versionPath"
        continue
    }

    $aarFile = Get-ChildItem -Path $hashFolder.FullName -Filter "*.aar" | Select-Object -First 1
    if ($aarFile) {
        $extractPath = Join-Path $hashFolder.FullName "extract"
        $zipAar = "$($aarFile.FullName).zip"
        Copy-Item -Path $aarFile.FullName -Destination $zipAar -Force
        Expand-Archive -Force -Path $zipAar -DestinationPath $extractPath
        Remove-Item $zipAar -Force

        $classesJar = Join-Path $extractPath "classes.jar"
        if (Test-Path $classesJar) {
            Copy-Item $classesJar $libPath -Force
            Write-Host "✅ Extracted $($dep.name).jar from AAR"
        } else {
            Write-Error "❌ No classes.jar in AAR for $($dep.name)"
        }
    } else {
        $jarFile = Get-ChildItem -Path $hashFolder.FullName -Filter "*.jar" | Select-Object -First 1
        if ($jarFile) {
            Copy-Item $jarFile.FullName $libPath -Force
            Write-Host "✅ Copied $($dep.name).jar directly"
        } else {
            Write-Error "❌ No AAR or JAR file found for $($dep.name)"
        }
    }
}

Set-Location $patchSrc
if (Test-Path .\build) {
    Remove-Item -Recurse -Force .\build
}
New-Item -ItemType Directory -Path .\build\out | Out-Null

$libJars = Get-ChildItem "$libsDir\*.jar" | ForEach-Object { $_.FullName }
$classpath = "$androidJar;$appClasses;" + ($libJars -join ";")

$javacArgs = @(
    "-classpath", $classpath,
    "-d", "build\out",
    "..\CrashLogger.java",
    "..\OverlayBlocker.java",
    "..\DexLoader.java",
    "..\NetworkUtils.java",
    "..\GuardianStateCache.java",
    "..\core\LogUploader.java",
    "..\firebase\GuardianMessagingService.java",
    "..\dex_patch_build\PatchOverride.java"
)

Write-Host "Compiling patch classes..."
& javac @javacArgs

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed"
    exit 1
}

Write-Host "Packaging patch.jar..."
jar cf patch.jar -C build\out .

if (Test-Path ".\patch.jar") {
    Write-Host "`npatch.jar created successfully!"
} else {
    Write-Error "Failed to create patch.jar"
    exit 1
}

Write-Host "`nPatch JAR Build Completed.`n"
