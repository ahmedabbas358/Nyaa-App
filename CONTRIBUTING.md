# Contributing to AniFlow

Thank you for your interest in contributing to **AniFlow**! This document provides instructions for setting up your environment, adhering to project conventions, and submitting pull requests.

---

## 1. Prerequisites

- **Java Development Kit**: JDK 17 (Eclipse Temurin 17 recommended)
- **Android SDK**: Compile SDK 35, Build Tools 35.0.0, Min SDK 26
- **Build System**: Gradle 8.5+ (use the bundled `./gradlew` or `gradlew.bat`)
- **IDE**: Android Studio Ladybug (or newer) or Antigravity IDE

---

## 2. Architectural Boundaries & Clean Architecture Rules

AniFlow enforces strict multi-module architectural isolation:

1. **Domain Layer (`:domain`)**:
   - MUST remain pure Kotlin/JVM code.
   - STRICTLY FORBIDDEN to import `android.*`, `androidx.compose.*`, `androidx.room.*`, or network clients (`okhttp3.*`).
2. **Presentation Layer (`:feature:*`, `:core:ui`)**:
   - STRICTLY FORBIDDEN to directly access Room DAOs or Nyaa provider classes.
   - All state mutations and operations must flow through Domain Use Cases.
3. **Provider Layer (`:provider:*`)**:
   - Must only implement contracts defined in `:domain` or `:provider:core`.
   - Never leak HTML parsing DTOs into downstream layers.
4. **Security & Sanitization**:
   - Never write files using raw provider titles without passing through `PathSanitizer`.
   - Never log tokens, passwords, or tracker credentials.

---

## 3. Local Verification Commands

Before opening a pull request, run the following scripts:

### Linux / macOS
```bash
# 1. Run static analysis and architecture verification
./scripts/check.sh

# 2. Run unit and integration tests
./scripts/test.sh
```

### Windows (PowerShell)
```powershell
# 1. Run static analysis and architecture verification
.\scripts\check.ps1

# 2. Run unit and integration tests
.\scripts\test.ps1
```

---

## 4. Pull Request Guidelines

1. **Branch Naming**:
   - `feature/description` for new capabilities.
   - `fix/description` for bug fixes.
   - `refactor/description` for architectural refactoring.
2. **Commit Messages**: Follow Conventional Commits format (e.g. `feat(search): add sort by seeders`, `fix(download): prevent state transition race condition`).
3. **Continuous Integration**: Every PR must pass all 10 Quality Gates configured in `.github/workflows/ci.yml`.
