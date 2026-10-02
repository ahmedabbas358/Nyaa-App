$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " AniFlow Static Analysis & Architecture" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

Write-Host "1. Running Android Lint..." -ForegroundColor Yellow
.\gradlew.bat lintDebug

Write-Host "2. Running Architecture Verification Tests..." -ForegroundColor Yellow
.\gradlew.bat :testing:test --tests "com.aniflow.testing.ArchitectureVerificationTests"

Write-Host "✅ All static analysis and architecture checks passed!" -ForegroundColor Green
