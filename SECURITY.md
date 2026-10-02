# Security Policy

## 1. Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## 2. Reporting a Vulnerability

If you discover a security vulnerability in AniFlow, please report it privately:

- **Email**: `security@aniflow.app` (or submit a Private Vulnerability Advisory on GitHub)
- **Do NOT open a public GitHub issue** for undisclosed security vulnerabilities.
- We will acknowledge receipt within 48 hours and provide a timeline for patch release and disclosure.

---

## 3. Threat Model & Security Posture

1. **Untrusted Provider Data**:
   - All titles, uploader handles, magnet URIs, and metadata from external sources (such as Nyaa) are treated as **untrusted user input**.
   - Input is passed through `PathSanitizer` before any filesystem write to prevent Path Traversal (`../../`) attacks.
2. **Secrets & Credentials**:
   - Keystores, signing passwords, and private tokens must **never** be committed to the repository.
   - Any sensitive authentication headers or tracker passkeys are dynamically redacted via `LogRedactor`.
3. **Configuration & Backup Security**:
   - Configuration exports exclude private accounts and tokens.
   - Configuration imports are size-limited (max 5MB) and AST depth-limited (max 10 levels) to prevent memory exhaustion and recursion crashes.
