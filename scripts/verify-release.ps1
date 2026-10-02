# PowerShell Release Verification Script (Sections 61 & 101)
param(
    [string]$ExpectedVersion = "1.0.0"
)

Write-Host "=== Starting AniFlow Release Verification (v$ExpectedVersion) ===" -ForegroundColor Cyan

$hasErrors = $false

# 1. Version Consistency Check in app/build.gradle.kts
$buildGradle = Get-Content -Path "app/build.gradle.kts" -Raw
if ($buildGradle -match 'versionName\s*=\s*"([^"]+)"') {
    $foundVersion = $matches[1]
    if ($foundVersion -ne $ExpectedVersion) {
        Write-Host "[FAIL] Version mismatch: app/build.gradle.kts has '$foundVersion' but expected '$ExpectedVersion'" -ForegroundColor Red
        $hasErrors = $true
    } else {
        Write-Host "[PASS] Version in app/build.gradle.kts matches '$ExpectedVersion'" -ForegroundColor Green
    }
} else {
    Write-Host "[FAIL] Could not locate versionName in app/build.gradle.kts" -ForegroundColor Red
    $hasErrors = $true
}

# 2. Check CHANGELOG.md contains the expected version
$changelog = Get-Content -Path "CHANGELOG.md" -Raw
if ($changelog -match "##\s*\[?$ExpectedVersion\]?") {
    Write-Host "[PASS] Version '$ExpectedVersion' documented in CHANGELOG.md" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Version '$ExpectedVersion' missing from CHANGELOG.md" -ForegroundColor Red
    $hasErrors = $true
}

# 3. Check for obvious private secrets or keys in tracking
$patternPart1 = "BEGIN "
$patternPart2 = "PRIVATE KEY"
$secretSearchRegex = $patternPart1 + ".*" + $patternPart2
$trackedFiles = Get-ChildItem -Path . -Recurse -File -Exclude "*.apk", "*.aab", "*.jar", "*.so", "*.png", "*.jpg" | Where-Object { 
    $_.FullName -notmatch '\\build\\' -and 
    $_.FullName -notmatch '\\\.gradle\\' -and 
    $_.FullName -notmatch '\\\.git\\' -and 
    $_.FullName -notmatch '\\scripts\\' 
}
foreach ($file in $trackedFiles) {
    $content = Get-Content -Path $file.FullName -Raw -ErrorAction SilentlyContinue
    if ($content) {
        if ($content -match $secretSearchRegex) {
            Write-Host "[CRITICAL FAIL] Potential private key detected in: $($file.FullName)" -ForegroundColor Red
            $hasErrors = $true
        }
    }
}

# 4. Mandatory Files Check
$requiredFiles = @("README.md", "LICENSE", "SECURITY.md", "CONTRIBUTING.md", "docs/release.md", ".gitignore")
foreach ($req in $requiredFiles) {
    if (Test-Path $req) {
        Write-Host "[PASS] Required file exists: $req" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Missing required file: $req" -ForegroundColor Red
        $hasErrors = $true
    }
}

if ($hasErrors) {
    Write-Host "=== Release Verification FAILED ===" -ForegroundColor Red
    exit 1
} else {
    Write-Host "=== Release Verification PASSED Successfully ===" -ForegroundColor Green
    exit 0
}
