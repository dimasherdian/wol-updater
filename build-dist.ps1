$ErrorActionPreference = "Stop"

Write-Host "Building Maven project (Fat JAR)..."
.\mvnw.cmd clean package

if (-not (Test-Path "target\updater-1.0-SNAPSHOT.jar")) {
    Write-Error "Maven build failed. Shaded JAR not found."
    exit 1
}

if (Test-Path "dist") {
    Write-Host "Cleaning old dist directory..."
    Remove-Item -Recurse -Force "dist"
}

if (Test-Path "jpackage-input") {
    Remove-Item -Recurse -Force "jpackage-input"
}
New-Item -ItemType Directory -Path "jpackage-input" | Out-Null
Copy-Item "target\updater-1.0-SNAPSHOT.jar" "jpackage-input\"

Write-Host "Running jpackage to create standalone app-image..."
jpackage --type app-image `
         --name "WoLUpdater" `
         --input jpackage-input `
         --main-jar updater-1.0-SNAPSHOT.jar `
         --main-class com.wol.updater.presentation.MainApp `
         --dest dist `
         --java-options "-Xmx512m"

if (Test-Path "dist\WoLUpdater") {
    Write-Host "Build complete! Standalone distribution created at: dist\WoLUpdater"
} else {
    Write-Error "jpackage failed to create the distribution directory."
}
