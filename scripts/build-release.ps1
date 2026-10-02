$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Building AniFlow Production Artifacts   " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

if (-not $env:KEYSTORE_FILE) {
    Write-Host "⚠️ Warning: KEYSTORE_FILE is not set. Building with debug fallback key." -ForegroundColor Yellow
}

.\gradlew.bat clean assembleRelease bundleRelease --stacktrace

Write-Host "📦 Artifacts generated in app/build/outputs/" -ForegroundColor Green
Get-ChildItem -Path "app/build/outputs/apk/release/*.apk" | Select-Object Name, Length
Get-ChildItem -Path "app/build/outputs/bundle/release/*.aab" | Select-Object Name, Length

Write-Host "✅ Build completed successfully!" -ForegroundColor Green
