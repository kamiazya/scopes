---
"scopes": minor
---

feat: add platform-specific bundle packages for easier downloads

Add platform-specific bundle packages containing SBOM, binaries, installation scripts, and comparison hashes as compressed archives. This reduces download confusion for users by providing clear, environment-specific packages (~20MB each) instead of requiring users to navigate 28 individual assets. Includes proper Windows PowerShell installer integration and maintains all existing security features (SLSA Level 3, SHA256 verification).