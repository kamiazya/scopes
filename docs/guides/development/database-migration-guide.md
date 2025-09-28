# Database Migration Guide

## Overview

This guide explains how to add database migrations to the Scopes project using SQLDelight. The migration system automatically updates database schemas when the application version changes.

## Architecture

The migration system consists of three main components:

1. **DatabaseMigrationManager** (`platform/infrastructure`) - Handles migration execution
2. **ApplicationVersion** (`platform/infrastructure`) - Tracks schema versions
3. **Migration Files** (`.sqm` files) - Contains SQL migration statements

## Adding a New Migration

### Step 1: Create Migration File

Create a new migration file in the appropriate context's `migrations` directory. The file should be named `{version}.sqm` where `{version}` is the current schema version.

```
contexts/{context}/infrastructure/src/main/sqldelight/migrations/{version}.sqm
```

Example locations:
- `contexts/scope-management/infrastructure/src/main/sqldelight/migrations/1.sqm`
- `contexts/event-store/infrastructure/src/main/sqldelight/migrations/1.sqm`
- `contexts/device-synchronization/infrastructure/src/main/sqldelight/migrations/1.sqm`

### Step 2: Write Migration SQL

Add your SQL migration statements to the file:

```sql
-- Migration from version 1 to version 2
-- Description of what this migration does

-- Add new column
ALTER TABLE scopes ADD COLUMN status TEXT DEFAULT 'active';

-- Create new index
CREATE INDEX IF NOT EXISTS idx_scopes_status ON scopes(status);

-- Create new table
CREATE TABLE IF NOT EXISTS scope_tags (
    scope_id TEXT NOT NULL,
    tag TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    PRIMARY KEY (scope_id, tag),
    FOREIGN KEY (scope_id) REFERENCES scopes(id) ON DELETE CASCADE
);
```

### Step 3: Update Schema Version

Edit `ApplicationVersion.kt` to increment the schema version:

```kotlin
object SchemaVersions {
    const val SCOPE_MANAGEMENT = 2L  // Incremented from 1L
    const val EVENT_STORE = 1L
    const val DEVICE_SYNCHRONIZATION = 1L
    const val USER_PREFERENCES = 1L
}
```

### Step 4: Add Version Mapping

Add a new entry to `versionHistory`:

```kotlin
val versionHistory = listOf(
    VersionMapping(
        appVersion = "0.1.0",
        scopeManagementSchema = 1L,
        eventStoreSchema = 1L,
        deviceSyncSchema = 1L,
        userPreferencesSchema = 1L
    ),
    // New entry for the migration
    VersionMapping(
        appVersion = "0.2.0",
        scopeManagementSchema = 2L,  // Updated schema version
        eventStoreSchema = 1L,
        deviceSyncSchema = 1L,
        userPreferencesSchema = 1L
    )
)
```

### Step 5: Update Application Version

Update the `CURRENT_VERSION` constant:

```kotlin
const val CURRENT_VERSION = "0.2.0"  // Updated from "0.1.0"
```

## Migration Guidelines

### DO:
- ✅ Test migrations thoroughly with production-like data
- ✅ Keep migrations idempotent (safe to run multiple times)
- ✅ Use `IF NOT EXISTS` for creating tables/indexes
- ✅ Provide default values for new NOT NULL columns
- ✅ Write clear comments explaining the migration purpose
- ✅ Consider data migration needs (not just schema changes)

### DON'T:
- ❌ Use `BEGIN/END TRANSACTION` in migration files (handled automatically)
- ❌ Drop columns without considering data loss
- ❌ Make breaking changes without a migration strategy
- ❌ Forget to update both schema version and app version
- ❌ Skip testing rollback scenarios

## Custom Migration Logic

For complex data migrations, use callbacks:

```kotlin
// In SqlDelightDatabaseProvider.kt
val callbacks = mapOf(
    2L to DatabaseMigrationManager.MigrationCallback { driver ->
        // Custom migration logic
        driver.execute(
            null,
            "UPDATE scopes SET status = 'archived' WHERE updated_at < ?",
            1,
            bindLong(0, thirtyDaysAgo)
        )
    }
)

migrationManager.migrate(
    driver = driver,
    schema = ScopeManagementDatabase.Schema,
    targetVersion = ApplicationVersion.SchemaVersions.SCOPE_MANAGEMENT,
    callbacks = callbacks
)
```

## Testing Migrations

### Unit Test

Create a test for your migration:

```kotlin
class MigrationTest : DescribeSpec({
    describe("Migration from v1 to v2") {
        it("should add status column") {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

            // Create v1 schema
            driver.execute(null, "CREATE TABLE scopes (id TEXT PRIMARY KEY)", 0)
            driver.execute(null, "PRAGMA user_version = 1", 0)

            // Run migration
            val manager = DatabaseMigrationManager.createDefault()
            manager.migrate(driver, TestSchema, 2L)

            // Verify new column exists
            val hasColumn = driver.executeQuery(
                null,
                "PRAGMA table_info(scopes)",
                mapper = { cursor ->
                    QueryResult.Value(
                        buildList {
                            while (cursor.next().value) {
                                add(cursor.getString(1) ?: "")
                            }
                        }.contains("status")
                    )
                },
                0
            ).value

            hasColumn shouldBe true
        }
    }
})
```

### Manual Testing

1. Copy a production database to test environment
2. Run the application with new version
3. Verify migration succeeds
4. Check data integrity
5. Test rollback if needed

## Rollback Strategy

While SQLDelight doesn't support automatic rollback, you can:

1. **Backup before migration**: Always backup production databases
2. **Write reverse migrations**: Create SQL scripts to undo changes
3. **Use transactions**: Migrations run in transactions and rollback on failure
4. **Test thoroughly**: Prevent the need for rollbacks

## Common Migration Scenarios

### Adding a Column

```sql
ALTER TABLE scopes ADD COLUMN priority INTEGER DEFAULT 0;
```

### Adding an Index

```sql
CREATE INDEX IF NOT EXISTS idx_scopes_priority ON scopes(priority);
```

### Renaming a Column

```sql
-- SQLite doesn't support RENAME COLUMN directly
-- Create new column, copy data, drop old column
ALTER TABLE scopes ADD COLUMN new_name TEXT;
UPDATE scopes SET new_name = old_name;
-- In next migration: ALTER TABLE scopes DROP COLUMN old_name;
```

### Adding a Foreign Key

```sql
-- SQLite requires recreating the table for new foreign keys
-- Better to include in initial schema design
```

### Data Migration

```sql
-- Update existing data
UPDATE scopes SET status = 'active' WHERE status IS NULL;
```

## Troubleshooting

### Migration Fails

1. Check SQL syntax in migration file
2. Verify schema version consistency
3. Check for constraint violations
4. Review migration logs

### Version Mismatch

If database version > application version:
- Database is from newer application version
- May need to upgrade application
- Check version history

### Performance Issues

- Add indexes after data migration
- Use batch operations for large updates
- Consider running migrations during maintenance windows

## Best Practices

1. **Version Control**: Always commit migration files with schema changes
2. **Documentation**: Document migration purpose and impact
3. **Backward Compatibility**: Consider older app versions during migration
4. **Testing**: Test with production-like data volumes
5. **Monitoring**: Log migration progress and timing
6. **Atomic Changes**: Keep migrations small and focused

## Related Documentation

- [SQLDelight Documentation](https://sqldelight.github.io/sqldelight/2.0.2/multiplatform_sqlite/migrations/)
- [Clean Architecture Guide](../explanation/clean-architecture.md)
- [Testing Guide](./testing.md)