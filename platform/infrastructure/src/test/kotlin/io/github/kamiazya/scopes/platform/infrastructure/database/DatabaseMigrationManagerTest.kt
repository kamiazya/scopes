package io.github.kamiazya.scopes.platform.infrastructure.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class DatabaseMigrationManagerTest : DescribeSpec({
    describe("DatabaseMigrationManager") {
        lateinit var migrationManager: DatabaseMigrationManager
        lateinit var driver: SqlDriver

        beforeEach {
            migrationManager = DatabaseMigrationManager.createDefault()
            // Create in-memory database for testing
            driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        }

        afterEach {
            driver.close()
        }

        describe("migrate") {
            it("should create new schema when database is fresh") {
                // Given
                val schema = mockk<SqlSchema<Any>>(relaxed = true)
                val targetVersion = 1L

                // When
                migrationManager.migrate(driver, schema, targetVersion)

                // Then
                verify(exactly = 1) { schema.create(driver) }

                // Check version was set
                val version = driver.executeQuery(
                    null,
                    "PRAGMA user_version",
                    mapper = { cursor ->
                        if (cursor.next()) cursor.getLong(0) ?: 0L else 0L
                    },
                    0
                )
                version shouldBe targetVersion
            }

            it("should migrate schema when current version is lower") {
                // Given
                val schema = mockk<SqlSchema<Any>>(relaxed = true)
                val currentVersion = 1L
                val targetVersion = 3L

                // Set initial version
                driver.execute(null, "PRAGMA user_version = $currentVersion", 0)

                // When
                migrationManager.migrate(driver, schema, targetVersion)

                // Then
                verify(exactly = 1) {
                    schema.migrate(driver, currentVersion, targetVersion, any())
                }

                // Check version was updated
                val version = driver.executeQuery(
                    null,
                    "PRAGMA user_version",
                    mapper = { cursor ->
                        if (cursor.next()) cursor.getLong(0) ?: 0L else 0L
                    },
                    0
                )
                version shouldBe targetVersion
            }

            it("should not migrate when database is up to date") {
                // Given
                val schema = mockk<SqlSchema<Any>>(relaxed = true)
                val targetVersion = 2L

                // Set current version to target
                driver.execute(null, "PRAGMA user_version = $targetVersion", 0)

                // When
                migrationManager.migrate(driver, schema, targetVersion)

                // Then
                verify(exactly = 0) { schema.create(driver) }
                verify(exactly = 0) { schema.migrate(any(), any(), any(), any()) }
            }

            it("should execute custom callbacks during migration") {
                // Given
                val schema = mockk<SqlSchema<Any>>(relaxed = true)
                val currentVersion = 1L
                val targetVersion = 3L
                var callbackExecuted = false

                val callbacks = mapOf(
                    2L to DatabaseMigrationManager.MigrationCallback { _ ->
                        callbackExecuted = true
                    }
                )

                // Set initial version
                driver.execute(null, "PRAGMA user_version = $currentVersion", 0)

                // Configure schema.migrate to call the after callback
                every {
                    schema.migrate(driver, currentVersion, targetVersion, any())
                } answers {
                    val afterCallback = arg<(Long) -> Unit>(3)
                    afterCallback(2L) // Simulate migration to version 2
                }

                // When
                migrationManager.migrate(driver, schema, targetVersion, callbacks)

                // Then
                callbackExecuted shouldBe true
            }

            it("should rollback on migration failure") {
                // Given
                val schema = mockk<SqlSchema<Any>>(relaxed = true)
                val currentVersion = 1L
                val targetVersion = 2L

                // Set initial version
                driver.execute(null, "PRAGMA user_version = $currentVersion", 0)

                // Configure schema to throw exception
                every {
                    schema.migrate(driver, currentVersion, targetVersion, any())
                } throws RuntimeException("Migration failed")

                // When
                try {
                    migrationManager.migrate(driver, schema, targetVersion)
                } catch (e: DatabaseMigrationException) {
                    // Expected
                    e.message shouldNotBe null
                }

                // Then - version should remain unchanged
                val version = driver.executeQuery(
                    null,
                    "PRAGMA user_version",
                    mapper = { cursor ->
                        if (cursor.next()) cursor.getLong(0) ?: 0L else 0L
                    },
                    0
                )
                version shouldBe currentVersion
            }

            it("should fail fast when database version is newer than target version") {
                // Given
                val schema = mockk<SqlSchema<Any>>(relaxed = true)
                val currentVersion = 5L
                val targetVersion = 3L

                // Set database version higher than target
                driver.execute(null, "PRAGMA user_version = $currentVersion", 0)

                // When & Then
                val exception = shouldThrow<DatabaseMigrationException> {
                    migrationManager.migrate(driver, schema, targetVersion)
                }

                exception.message shouldBe "Database version ($currentVersion) is newer than application version ($targetVersion). Please update the application."

                // Verify no schema operations were attempted
                verify(exactly = 0) { schema.create(driver) }
                verify(exactly = 0) { schema.migrate(any(), any(), any(), any()) }

                // Version should remain unchanged
                val version = driver.executeQuery(
                    null,
                    "PRAGMA user_version",
                    mapper = { cursor ->
                        if (cursor.next()) cursor.getLong(0) ?: 0L else 0L
                    },
                    0
                )
                version shouldBe currentVersion
            }
        }

        describe("getCurrentVersion") {
            it("should return 0 for new database") {
                // When
                val version = driver.executeQuery(
                    null,
                    "PRAGMA user_version",
                    mapper = { cursor ->
                        if (cursor.next()) cursor.getLong(0) ?: 0L else 0L
                    },
                    0
                )

                // Then
                version shouldBe 0L
            }

            it("should return correct version after setting") {
                // Given
                val expectedVersion = 5L
                driver.execute(null, "PRAGMA user_version = $expectedVersion", 0)

                // When
                val version = driver.executeQuery(
                    null,
                    "PRAGMA user_version",
                    mapper = { cursor ->
                        if (cursor.next()) cursor.getLong(0) ?: 0L else 0L
                    },
                    0
                )

                // Then
                version shouldBe expectedVersion
            }
        }
    }
})