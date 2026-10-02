$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Running AniFlow Automated Test Suites   " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

.\gradlew.bat test --stacktrace

Write-Host "✅ All tests passed successfully!" -ForegroundColor Green
