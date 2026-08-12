$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host " BUILDING PROFILE A: MINIMAL / SELF-CONTAINED"
Write-Host "========================================"

Write-Host "`n[1/6] Cleaning and Building Fat JAR..."
.\mvnw.cmd clean package

if (-not (Test-Path "target\updater-1.0-SNAPSHOT.jar")) {
    Write-Error "Maven build failed. Shaded JAR not found."
    exit 1
}

if (Test-Path "dist\WoLUpdater-Minimal") {
    Write-Host "`n[2/6] Cleaning old Minimal dist directory..."
    Remove-Item -Recurse -Force "dist\WoLUpdater-Minimal"
}

if (Test-Path "jpackage-input") {
    Remove-Item -Recurse -Force "jpackage-input"
}
New-Item -ItemType Directory -Path "jpackage-input" | Out-Null
Copy-Item "target\updater-1.0-SNAPSHOT.jar" "jpackage-input\"

if (Test-Path "minimal-jre") {
    Remove-Item -Recurse -Force "minimal-jre"
}

Write-Host "`n[3/6] Detecting required Java modules..."
# Run jdeps to find required modules from the fat jar
$jdepsOutput = (jdeps --print-module-deps --ignore-missing-deps --multi-release 17 target\updater-1.0-SNAPSHOT.jar)

# Ensure common required modules that jdeps might miss dynamically are included
$modules = $jdepsOutput.Trim() + ",java.xml,jdk.crypto.ec,jdk.charsets,java.naming,java.desktop,jdk.unsupported,java.prefs,java.logging"
Write-Host "Required modules: $modules"

Write-Host "`n[4/6] Creating minimal JRE with jlink..."
jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules $modules --output minimal-jre

Write-Host "`n[5/6] Compiling native launcher..."
$cscPath = "$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if (-not (Test-Path $cscPath)) {
    $cscPath = "$env:WINDIR\Microsoft.NET\Framework\v4.0.30319\csc.exe"
}

if (-not (Test-Path $cscPath)) {
    Write-Error "Could not find csc.exe compiler for C#. Make sure .NET Framework 4.0+ is installed."
    exit 1
}

$exeTemp = "target\WarsOfLibertyUpdater.exe"
& $cscPath /nologo /target:winexe /out:"$exeTemp" /win32icon:"src\main\resources\wol.ico" "src\launcher\WoLUpdaterLauncher.cs"

if ($LASTEXITCODE -ne 0 -or -not (Test-Path $exeTemp)) {
    Write-Error "Failed to compile the launcher."
    exit 1
}

Write-Host "`n[6/6] Assembling Minimal distribution..."
New-Item -ItemType Directory -Path "dist\WoLUpdater-Minimal\app" -Force | Out-Null
Copy-Item "target\updater-1.0-SNAPSHOT.jar" "dist\WoLUpdater-Minimal\app\"
Copy-Item $exeTemp "dist\WoLUpdater-Minimal\"
Copy-Item -Recurse "minimal-jre" "dist\WoLUpdater-Minimal\runtime"

Write-Host "`nBuild complete!"

# Calculate sizes
$runtimeSize = [math]::Round(((Get-ChildItem -Path "dist\WoLUpdater-Minimal\runtime" -Recurse -File | Measure-Object -Property Length -Sum).Sum / 1MB), 2)
$appSize = [math]::Round(((Get-ChildItem -Path "dist\WoLUpdater-Minimal\app" -Recurse -File | Measure-Object -Property Length -Sum).Sum / 1MB), 2)
$exeSize = [math]::Round(((Get-ChildItem -Path "dist\WoLUpdater-Minimal\WarsOfLibertyUpdater.exe" | Measure-Object -Property Length -Sum).Sum / 1MB), 2)
$totalSize = [math]::Round(((Get-ChildItem -Path "dist\WoLUpdater-Minimal" -Recurse -File | Measure-Object -Property Length -Sum).Sum / 1MB), 2)

Write-Host "========================================"
Write-Host " SIZE REPORT: MINIMAL PROFILE"
Write-Host "========================================"
Write-Host "Launcher (EXE)       : $exeSize MB"
Write-Host "Runtime (minimal-jre): $runtimeSize MB"
Write-Host "App (Fat JAR)        : $appSize MB"
Write-Host "Total Size           : $totalSize MB"
Write-Host "Location             : dist\WoLUpdater-Minimal"
Write-Host "========================================"
