---
"scopes": patch
---

Fix automatic release workflow triggering after Version PR merges

- Integrated release triggering into version-and-release.yml to work around GitHub Actions limitations
- GitHub Actions security prevents workflows from triggering other workflows when using GITHUB_TOKEN
- Modified release.yml to only support manual workflow_dispatch
- Updated permissions to `actions: write` to allow workflow dispatch via gh CLI
- Fixed gh CLI authentication by using GH_TOKEN environment variable instead of GITHUB_TOKEN
- Automatic releases now work correctly after Version PR merges without requiring Personal Access Tokens
