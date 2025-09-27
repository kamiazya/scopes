---
"scopes": patch
---

Fix automatic release workflow triggering after Version PR merges

- Integrated release triggering into version-and-release.yml to work around GitHub Actions limitations
- GitHub Actions security prevents workflows from triggering other workflows when using GITHUB_TOKEN
- Modified release.yml to only support manual workflow_dispatch
- Automatic releases now work correctly after Version PR merges without requiring Personal Access Tokens