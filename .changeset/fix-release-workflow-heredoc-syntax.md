---
"scopes": patch
---

fix: resolve release workflow failure caused by heredoc syntax error

- Fix unquoted heredoc in release.yml that caused variable expansion issues during workflow execution
- This fixes the v0.0.5 release failure and ensures future releases complete successfully
- Resolves GitHub Actions workflow syntax error that prevented release automation