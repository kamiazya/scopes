package io.github.kamiazya.scopes.platform.infrastructure.database.migration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import java.io.File

class MigrationExecutorTest :
    DescribeSpec({

        describe("SqlDelightMigrationExecutor") {

            fun createTestExecutor(): Pair<JdbcSqliteDriver, MigrationExecutor> {
                val tempFile = File.createTempFile("test_executor_", ".db")
                tempFile.deleteOnExit()

                val driver = JdbcSqliteDriver("jdbc:sqlite:${tempFile.absolutePath}")
                val executor = SqlDelightMigrationExecutor(driver)

                return driver to executor
            }

            afterSpec {
                // Clean up any remaining resources
            }

            it("should execute single SQL statement") {
                runTest {
                    val (driver, executor) = createTestExecutor()

                    try {
                        val sql = """
                        CREATE TABLE test_table (
                            id INTEGER PRIMARY KEY,
                            name TEXT
                        )
                        """.trimIndent()

                        val result = executor.executeSql(sql)
                        result.shouldBeRight()

                        // Verify table exists
                        val tableExists = executor.tableExists("test_table").shouldBeRight()
                        tableExists shouldBe true
                    } finally {
                        driver.close()
                    }
                }
            }

            it("should execute multiple SQL statements") {
                runTest {
                    val (driver, executor) = createTestExecutor()

                    try {
                        val statements = listOf(
                            "CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)",
                            "CREATE TABLE posts (id INTEGER PRIMARY KEY, user_id INTEGER, content TEXT)",
                            "CREATE INDEX idx_posts_user ON posts(user_id)",
                        )

                        val result = executor.executeSql(statements)
                        result.shouldBeRight()

                        // Verify tables exist
                        val usersExists = executor.tableExists("users").shouldBeRight()
                        usersExists shouldBe true

                        val postsExists = executor.tableExists("posts").shouldBeRight()
                        postsExists shouldBe true
                    } finally {
                        driver.close()
                    }
                }
            }

            it("should handle error in SQL statements atomically") {
                runTest {
                    val (driver, executor) = createTestExecutor()

                    try {
                        val statements = listOf(
                            "CREATE TABLE test_error (id INTEGER PRIMARY KEY)",
                            "INVALID SQL STATEMENT", // This will cause an error
                        )

                        val result = executor.executeSql(statements)
                        val error = result.shouldBeLeft().shouldBeInstanceOf<MigrationError.SqlExecutionError>()
                        error.sql shouldBe "INVALID SQL STATEMENT"

                        // With atomic execution, no statements should be committed
                        val tableExists = executor.tableExists("test_error").shouldBeRight()
                        tableExists shouldBe false
                    } finally {
                        driver.close()
                    }
                }
            }

            it("should create schema_versions table") {
                runTest {
                    val (driver, executor) = createTestExecutor()

                    try {
                        val result = executor.ensureSchemaVersionsTable()
                        result.shouldBeRight()

                        // Verify table exists
                        val tableExists = executor.tableExists("schema_versions").shouldBeRight()
                        tableExists shouldBe true
                    } finally {
                        driver.close()
                    }
                }
            }

            it("should handle empty statement lists") {
                runTest {
                    val (driver, executor) = createTestExecutor()

                    try {
                        val result = executor.executeSql(emptyList())
                        result.shouldBeRight()
                    } finally {
                        driver.close()
                    }
                }
            }
        }
    })
