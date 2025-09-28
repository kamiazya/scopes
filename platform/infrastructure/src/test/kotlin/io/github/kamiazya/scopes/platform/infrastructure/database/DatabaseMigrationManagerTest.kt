package io.github.kamiazya.scopes.platform.infrastructure.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

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
                val targetVersion = 1L
                val createCalled = mutableListOf<SqlDriver>()
                val schema = object : SqlSchema<QueryResult<Unit>> {
                    override val version = targetVersion
                    
                    override fun create(driver: SqlDriver): QueryResult<Unit> {
                        createCalled.add(driver)
                        return QueryResult.Value(Unit)
                    }
                    
                    override fun migrate(
                        driver: SqlDriver,
                        oldVersion: Long,
                        newVersion: Long,
                        vararg callbacks: AfterVersion
                    ): QueryResult<Unit> {
                        return QueryResult.Value(Unit)
                    }
                }

                // When
                migrationManager.migrate(driver, schema, targetVersion)

                // Then
                createCalled.size shouldBe 1
                createCalled.first() shouldBe driver

                // Check version was set
                val version = driver.executeQuery<Long>(
                    null,
                    "PRAGMA user_version",
                    mapper = { cursor ->
                        QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
                    },
                    0
                ).value
                version shouldBe targetVersion
            }

            it("should migrate schema when current version is lower") {
                // Given
                val currentVersion = 1L
                val targetVersion = 3L
                val migrateCalled = mutableListOf<Triple<SqlDriver, Long, Long>>()
                
                val schema = object : SqlSchema<QueryResult<Unit>> {
                    override val version = targetVersion
                    
                    override fun create(driver: SqlDriver): QueryResult<Unit> {
                        return QueryResult.Value(Unit)
                    }
                    
                    override fun migrate(
                        driver: SqlDriver,
                        oldVersion: Long,
                        newVersion: Long,
                        vararg callbacks: AfterVersion
                    ): QueryResult<Unit> {
                        migrateCalled.add(Triple(driver, oldVersion, newVersion))
                        return QueryResult.Value(Unit)
                    }
                }

                // Set initial version
                driver.execute(null, "PRAGMA user_version = $currentVersion", 0)

                // When
                migrationManager.migrate(driver, schema, targetVersion)

                // Then
                migrateCalled.size shouldBe 1
                val (migrateDriver, oldVer, newVer) = migrateCalled.first()
                migrateDriver shouldBe driver
                oldVer shouldBe currentVersion
                newVer shouldBe targetVersion

                // Check version was updated
                val version = driver.executeQuery<Long>(
                    null,
                    "PRAGMA user_version",
                    mapper = { cursor ->
                        QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
                    },
                    0
                ).value
                version shouldBe targetVersion
            }

            it("should not migrate when database is up to date") {
                // Given
                val targetVersion = 2L
                var createCalled = false
                var migrateCalled = false
                
                val schema = object : SqlSchema<QueryResult<Unit>> {
                    override val version = targetVersion
                    
                    override fun create(driver: SqlDriver): QueryResult<Unit> {
                        createCalled = true
                        return QueryResult.Value(Unit)
                    }
                    
                    override fun migrate(
                        driver: SqlDriver,
                        oldVersion: Long,
                        newVersion: Long,
                        vararg callbacks: AfterVersion
                    ): QueryResult<Unit> {
                        migrateCalled = true
                        return QueryResult.Value(Unit)
                    }
                }

                // Set current version to target
                driver.execute(null, "PRAGMA user_version = $targetVersion", 0)

                // When
                migrationManager.migrate(driver, schema, targetVersion)

                // Then
                createCalled shouldBe false
                migrateCalled shouldBe false
            }

            it("should execute custom callbacks during migration") {
                // Given
                val currentVersion = 1L
                val targetVersion = 3L
                var callbackExecuted = false

                val callbacks = mapOf(
                    2L to DatabaseMigrationManager.MigrationCallback { _ ->
                        callbackExecuted = true
                    }
                )

                val schema = object : SqlSchema<QueryResult<Unit>> {
                    override val version = targetVersion
                    
                    override fun create(driver: SqlDriver): QueryResult<Unit> {
                        return QueryResult.Value(Unit)
                    }
                    
                    override fun migrate(
                        driver: SqlDriver,
                        oldVersion: Long,
                        newVersion: Long,
                        vararg callbacks: AfterVersion
                    ): QueryResult<Unit> {
                        return QueryResult.Value(Unit)
                    }
                }

                // Set initial version
                driver.execute(null, "PRAGMA user_version = $currentVersion", 0)

                // When
                migrationManager.migrate(driver, schema, targetVersion, callbacks)

                // Then
                callbackExecuted shouldBe true
            }

            it("should rollback on migration failure") {
                // Given
                val currentVersion = 1L
                val targetVersion = 2L
                
                val schema = object : SqlSchema<QueryResult<Unit>> {
                    override val version = targetVersion
                    
                    override fun create(driver: SqlDriver): QueryResult<Unit> {
                        return QueryResult.Value(Unit)
                    }
                    
                    override fun migrate(
                        driver: SqlDriver,
                        oldVersion: Long,
                        newVersion: Long,
                        vararg callbacks: AfterVersion
                    ): QueryResult<Unit> {
                        throw RuntimeException("Migration failed")
                    }
                }

                // Set initial version
                driver.execute(null, "PRAGMA user_version = $currentVersion", 0)

                // When
                try {
                    migrationManager.migrate(driver, schema, targetVersion)
                } catch (e: IllegalStateException) {
                    // Expected
                    e.message shouldNotBe null
                }

                // Then - version should remain unchanged
                val version = driver.executeQuery<Long>(
                    null,
                    "PRAGMA user_version",
                    mapper = { cursor ->
                        QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
                    },
                    0
                ).value
                version shouldBe currentVersion
            }

            it("should fail fast when database version is newer than target version") {
                // Given
                val currentVersion = 5L
                val targetVersion = 3L
                var createCalled = false
                var migrateCalled = false
                
                val schema = object : SqlSchema<QueryResult<Unit>> {
                    override val version = targetVersion
                    
                    override fun create(driver: SqlDriver): QueryResult<Unit> {
                        createCalled = true
                        return QueryResult.Value(Unit)
                    }
                    
                    override fun migrate(
                        driver: SqlDriver,
                        oldVersion: Long,
                        newVersion: Long,
                        vararg callbacks: AfterVersion
                    ): QueryResult<Unit> {
                        migrateCalled = true
                        return QueryResult.Value(Unit)
                    }
                }

                // Set database version higher than target
                driver.execute(null, "PRAGMA user_version = $currentVersion", 0)

                // When & Then
                val exception = shouldThrow<IllegalStateException> {
                    migrationManager.migrate(driver, schema, targetVersion)
                }

                exception.message shouldBe "Migration failed: Database version ($currentVersion) is newer than application version ($targetVersion). Please update the application to a newer version."

                // Verify no schema operations were attempted
                createCalled shouldBe false
                migrateCalled shouldBe false

                // Version should remain unchanged
                val version = driver.executeQuery<Long>(
                    null,
                    "PRAGMA user_version",
                    mapper = { cursor ->
                        QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
                    },
                    0
                ).value
                version shouldBe currentVersion
            }
        }

        describe("getCurrentVersion") {
            it("should return 0 for new database") {
                // When
                val version = driver.executeQuery<Long>(
                    null,
                    "PRAGMA user_version",
                    mapper = { cursor ->
                        QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
                    },
                    0
                ).value

                // Then
                version shouldBe 0L
            }

            it("should return correct version after setting") {
                // Given
                val expectedVersion = 5L
                driver.execute(null, "PRAGMA user_version = $expectedVersion", 0)

                // When
                val version = driver.executeQuery<Long>(
                    null,
                    "PRAGMA user_version",
                    mapper = { cursor ->
                        QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
                    },
                    0
                ).value

                // Then
                version shouldBe expectedVersion
            }
        }
    }
})