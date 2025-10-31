---
"scopes": minor
---

Add automatic database migration system for SQLDelight databases

- Implement DatabaseMigrationManager with thread-safe migration execution
- Add version tracking using SQLite PRAGMA user_version
- Support for custom migration callbacks at specific versions
- Automatic schema creation for fresh databases
- Fail-fast behavior when database version is newer than application
- Database-level locking to prevent concurrent migrations across processes
- Proper resource management and exception handling
- Migration files (.sqm) for all bounded contexts (scope-management, event-store, device-synchronization)
- Comprehensive documentation and examples in database-migration-guide.md