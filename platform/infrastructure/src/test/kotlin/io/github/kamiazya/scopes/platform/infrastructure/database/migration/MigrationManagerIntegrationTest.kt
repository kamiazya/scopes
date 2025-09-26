package io.github.kamiazya.scopes.platform.infrastructure.database.migration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.kamiazya.scopes.platform.db.PlatformDatabase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import java.io.File

class MigrationManagerIntegrationTest :
    DescribeSpec({

        describe("MigrationManager Integration") {

            fun createTestSetup(): Triple<JdbcSqliteDriver, PlatformDatabase, MigrationManager> {
                // Create temporary database
                val tempFile = File.createTempFile("test_migration_", ".db")
                tempFile.deleteOnExit()

                val driver = JdbcSqliteDriver("jdbc:sqlite:${tempFile.absolutePath}")

                // Create the database schema
                PlatformDatabase.Schema.create(driver)
                val database = PlatformDatabase(driver)

                val executor = SqlDelightMigrationExecutor(driver)
                val repository = SqlDelightSchemaVersionStore(database)

                // Define test migrations
                val testMigrations = listOf(
                    object : SqlMigration(
                        version = 1,
                        description = "Create users table",
                    ) {
                        override val sql = listOf(
                            """
                        CREATE TABLE users (
                            id INTEGER PRIMARY KEY,
                            username TEXT NOT NULL
                        )
                            """.trimIndent(),
                        )
                    },
                    object : SqlMigration(
                        version = 2,
                        description = "Add email to users",
                    ) {
                        override val sql = listOf(
                            "ALTER TABLE users ADD COLUMN email TEXT",
                        )
                    },
                )

                val migrationManager = DefaultMigrationManager(
                    executor = executor,
                    repository = repository,
                    migrationProvider = { testMigrations },
                )

                return Triple(driver, database, migrationManager)
            }

            it("should report initial status with no migrations applied") {
                runTest {
                    val (driver, _, migrationManager) = createTestSetup()

                    try {
                        val result = migrationManager.getStatus()

                        result.shouldBeInstanceOf<arrow.core.Either.Right<MigrationStatusReport>>()
                        val status = result.value

                        status.currentVersion shouldBe 0L
                        status.availableMigrations shouldHaveSize 2
                        status.appliedMigrations.shouldBeEmpty()
                        status.pendingMigrations shouldHaveSize 2
                        status.isUpToDate shouldBe false
                        status.hasGaps shouldBe false
                    } finally {
                        driver.close()
                    }
                }
            }

            it("should apply all migrations successfully") {
                runTest {
                    val (driver, _, migrationManager) = createTestSetup()

                    try {
                        val result = migrationManager.migrateUp()

                        result.shouldBeInstanceOf<arrow.core.Either.Right<MigrationSummary>>()
                        val migrationResult = result.value

                        migrationResult.executedMigrations shouldHaveSize 2
                        migrationResult.fromVersion shouldBe 0L
                        migrationResult.toVersion shouldBe 2L

                        // Verify status after migration
                        val status = migrationManager.getStatus()
                        status.shouldBeInstanceOf<arrow.core.Either.Right<MigrationStatusReport>>()
                        status.value.currentVersion shouldBe 2L
                        status.value.isUpToDate shouldBe true
                    } finally {
                        driver.close()
                    }
                }
            }

            it("should migrate to specific version") {
                runTest {
                    val (driver, _, migrationManager) = createTestSetup()

                    try {
                        val result = migrationManager.migrateTo(1L)

                        result.shouldBeInstanceOf<arrow.core.Either.Right<MigrationSummary>>()
                        val migrationResult = result.value

                        migrationResult.executedMigrations shouldHaveSize 1
                        migrationResult.toVersion shouldBe 1L

                        // Verify only first migration was applied
                        val status = migrationManager.getStatus()
                        status.shouldBeInstanceOf<arrow.core.Either.Right<MigrationStatusReport>>()
                        status.value.currentVersion shouldBe 1L
                        status.value.pendingMigrations shouldHaveSize 1
                    } finally {
                        driver.close()
                    }
                }
            }

            it("should validate migration sequence") {
                runTest {
                    val (driver, _, migrationManager) = createTestSetup()

                    try {
                        // Apply migrations
                        migrationManager.migrateUp()

                        val result = migrationManager.validate()

                        result.shouldBeInstanceOf<arrow.core.Either.Right<SequenceValidationReport>>()
                        val validation = result.value

                        validation.isValid shouldBe true
                        validation.gaps.shouldBeEmpty()
                        validation.inconsistencies.shouldBeEmpty()
                    } finally {
                        driver.close()
                    }
                }
            }
        }
    })
