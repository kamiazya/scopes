---
"scopes": patch
---

fix: resolve GitHub Actions release failure by adding redundant trigger mechanisms

- Add push trigger to release.yml for automatic releases on tag push
- Update tag resolution logic to handle both workflow_dispatch and push triggers
- Remove failing manual release trigger job that was causing HTTP 422 errors
- Ensures reliable releases by providing multiple trigger mechanisms
