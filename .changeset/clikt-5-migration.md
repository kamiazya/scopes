---
"scopes": patch
---

feat: Upgrade to Clikt 5.0.3 and migrate deprecated APIs

- Update Clikt version from 4.4.0 to 5.0.3 for improved CLI functionality
- Migrate all CLI commands to property-based configuration (Clikt 5.x requirement) 
- Fix deprecated echo(err=true) usage across multiple command files
- Update Native Image configuration for Mordant terminal interface compatibility
- Improve exit handling by using parse() instead of main() for proper error handling
