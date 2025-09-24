---
"scopes": patch
---

Fix release workflow to prevent incorrect tag creation

- Fixed Version and Release workflow to only create tags when Version PR is merged
- Added tag format validation to Release workflow to prevent branch names being used as tags
- Improved error messages to guide users on correct tag format
