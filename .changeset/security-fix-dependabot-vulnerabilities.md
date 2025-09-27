---
"scopes": patch
---

Fix medium and low severity security vulnerabilities in dependencies

- Update Apache Commons Lang3 to 3.18.0 to fix an uncontrolled recursion vulnerability
- Verify Logback 1.5.18 includes fixes for CVE-2024-12801 (SSRF) and CVE-2024-12798 (Expression Language injection)
- Add explicit commons-lang3 dependency to ensure secure version is used across all modules

This patch resolves all 3 open Dependabot security alerts (2 medium severity, 1 low severity) without breaking changes to the public API.
