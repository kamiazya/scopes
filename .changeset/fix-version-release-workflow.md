---
"scopes": patch
---

Fix version-and-release workflow to handle GitHub API changes

The workflow was checking for a 'merged' field that no longer exists in the GitHub API response.
Updated to check 'merged_at' field instead to properly detect merged Version PRs.

This fixes the issue where release tags were not being created automatically after merging Version PRs.
EOF < /dev/null
