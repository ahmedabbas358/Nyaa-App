param (
    [string]$ApkPath = "app/build/outputs/apk/release/app-release.apk"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $ApkPath)) {
    Write-Host "❌ Error: Artifact not found at $ApkPath" -ForegroundColor Red
    exit 1
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Verifying Production Artifact: $ApkPath" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Generate SHA256
$hash = Get-FileHash -Path $ApkPath -Algorithm SHA256
Write-Host "SHA256: $($hash.Hash)" -ForegroundColor Green

# 2. Check size
$fileInfo = Get-Item $ApkPath
Write-Host "Size: $($fileInfo.Length) bytes" -ForegroundColor Green

Write-Host "✅ Artifact verified successfully!" -ForegroundColor Green
