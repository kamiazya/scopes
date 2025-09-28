# Enable Foreign Key Constraints in Test Databases

## Issue

The AI review correctly identified that foreign key constraints are not enabled for in-memory test databases, which could lead to test-production parity issues. While production databases enable foreign keys via `ManagedSqlDriver.createWithDefaults()`, in-memory test databases created by `createInMemoryDatabase()` do not.

## Attempted Fix

Adding `PRAGMA foreign_keys = ON` to in-memory database creation causes 23 test failures in `SqlDelightScopeAliasRepositoryTest` and 2 in `SqlDelightScopeRepositoryTest`. These tests are inserting alias records without corresponding scope records, violating foreign key constraints.

## Required Changes

1. **Enable foreign keys in all in-memory databases**:
   - Add `driver.execute(null, "PRAGMA foreign_keys = ON", 0)` after driver creation
   - Apply to all `createInMemoryDatabase()` methods across bounded contexts

2. **Fix failing tests**:
   - Update tests to properly create parent scope records before aliases
   - Ensure test data respects referential integrity
   - Consider using test fixtures or builders for proper test data setup

## Benefits

- Test-production parity: Tests will catch foreign key violations
- Data integrity: Prevents orphaned records in production
- Better test quality: Forces tests to use realistic data relationships

## Implementation Notes

The foreign key pragma must be set:
- After creating the driver
- Before creating the schema
- For each new connection (SQLite default is OFF)

## Related Files

- `contexts/scope-management/infrastructure/src/main/kotlin/io/github/kamiazya/scopes/scopemanagement/infrastructure/sqldelight/SqlDelightDatabaseProvider.kt`
- `contexts/event-store/infrastructure/src/main/kotlin/io/github/kamiazya/scopes/eventstore/infrastructure/sqldelight/SqlDelightDatabaseProvider.kt`
- `contexts/device-synchronization/infrastructure/src/main/kotlin/io/github/kamiazya/scopes/devicesync/infrastructure/sqldelight/SqlDelightDatabaseProvider.kt`
- Test files that need updating for proper foreign key compliance