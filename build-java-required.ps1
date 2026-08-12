$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host " BUILDING PROFILE B: JAVA-REQUIRED"
Write-Host "========================================"

Write-Host "`n[1/4] Cleaning and Building Fat JAR..."
.\mvnw.cmd clean package

if (-not (Test-Path "target\updater-1.0-SNAPSHOT.jar")) {
    Write-Error "Maven build failed. Shaded JAR not found."
    exit 1
}

if (Test-Path "dist\WoLUpdater-JavaRequired") {
    Write-Host "`n[2/4] Cleaning old Java-Required dist directory..."
    Remove-Item -Recurse -Force "dist\WoLUpdater-JavaRequired"
}

Write-Host "`n[3/4] Compiling native launcher..."
$cscPath = "$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if (-not (Test-Path $cscPath)) {
    $cscPath = "$env:WINDIR\Microsoft.NET\Framework\v4.0.30319\csc.exe"
}

if (-not (Test-Path $cscPath)) {
    Write-Error "Could not find csc.exe compiler for C#. Make sure .NET Framework 4.0+ is installed."
    exit 1
}

# Compile to a temporary location first
$exeTemp = "target\WarsOfLibertyUpdater.exe"
& $cscPath /nologo /target:winexe /out:"$exeTemp" /win32icon:"src\main\resources\wol.ico" "src\launcher\WoLUpdaterLauncher.cs"

if ($LASTEXITCODE -ne 0 -or -not (Test-Path $exeTemp)) {
    Write-Error "Failed to compile the launcher."
    exit 1
}

Write-Host "`n[4/4] Assembling distribution..."
New-Item -ItemType Directory -Path "dist\WoLUpdater-JavaRequired\app" -Force | Out-Null
Copy-Item "target\updater-1.0-SNAPSHOT.jar" "dist\WoLUpdater-JavaRequired\app\"
Copy-Item $exeTemp "dist\WoLUpdater-JavaRequired\"

Write-Host "`nBuild complete!"

# Calculate sizes
$appSize = [math]::Round(((Get-ChildItem -Path "dist\WoLUpdater-JavaRequired\app" -Recurse -File | Measure-Object -Property Length -Sum).Sum / 1MB), 2)
$exeSize = [math]::Round(((Get-ChildItem -Path "dist\WoLUpdater-JavaRequired\WarsOfLibertyUpdater.exe" | Measure-Object -Property Length -Sum).Sum / 1MB), 2)
$totalSize = [math]::Round(((Get-ChildItem -Path "dist\WoLUpdater-JavaRequired" -Recurse -File | Measure-Object -Property Length -Sum).Sum / 1MB), 2)

Write-Host "========================================"
Write-Host " SIZE REPORT: JAVA-REQUIRED PROFILE"
Write-Host "========================================"
Write-Host "Launcher (EXE)       : $exeSize MB"
Write-Host "App (Fat JAR)        : $appSize MB"
Write-Host "Total Size           : $totalSize MB"
Write-Host "Location             : dist\WoLUpdater-JavaRequired"
Write-Host "========================================"
