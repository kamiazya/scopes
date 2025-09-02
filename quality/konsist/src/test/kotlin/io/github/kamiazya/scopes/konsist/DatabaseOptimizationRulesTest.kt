package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Konsist rules for database optimization patterns
 * Ensures proper database optimization practices are followed
 */
class DatabaseOptimizationRulesTest :
    DescribeSpec({
        describe("Database Optimization Rules") {

            it("repository methods returning lists should use batch loading for related entities") {
                Konsist.scopeFromProduction()
                    .files
                    .withNameEndingWith("Repository.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        // Look for functions that return lists and query database
                        (function.returnType?.text?.contains("List<") == true) &&
                            (
                                function.text.contains("executeAsList()") ||
                                    function.text.contains("findAll") ||
                                    function.text.contains("findBy")
                                )
                    }
                    .assertTrue { function ->
                        // Should either use batch loading or have a comment explaining why not
                        val hasBatchLoading = function.text.contains("loadAspectsForScopes") ||
                            function.text.contains("batch") ||
                            function.text.contains("findByScopeIds") ||
                            function.text.contains("IN (")

                        val hasExplanatoryComment = function.text.contains("// No") ||
                            function.text.contains("// Single") ||
                            function.text.contains("// Direct")

                        hasBatchLoading ||
                            hasExplanatoryComment ||
                            // Allow simple mapping without additional queries
                            !function.text.contains("map")
                    }
            }

            it("SQLite IN clause queries should handle variable limits") {
                Konsist.scopeFromProduction()
                    .files
                    .withNameEndingWith("Repository.kt", "Queries.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        // Look for functions using IN clause with lists
                        function.text.contains("IN (") ||
                            function.text.contains("findBy") &&
                            function.text.contains("Ids")
                    }
                    .assertTrue { function ->
                        // Should handle SQLite's 999 variable limit
                        val hasChunking = function.text.contains("chunked") ||
                            function.text.contains("SQLITE_VARIABLE_LIMIT") ||
                            function.text.contains("999")

                        val hasSmallListGuarantee = function.text.contains("// Small list") ||
                            function.text.contains("// Limited") ||
                            function.parameters.any { it.text.contains("max") }

                        hasChunking || hasSmallListGuarantee
                    }
            }

            it("database scripts should use proper backup methods") {
                Konsist.scopeFromProject()
                    .files
                    .withNameEndingWith(".sh")
                    .filter { file ->
                        file.text.contains("sqlite") && file.text.contains("backup")
                    }
                    .assertTrue { file ->
                        // Should use sqlite3 .backup command for WAL safety
                        val hasProperBackup = file.text.contains(".backup") ||
                            file.text.contains("sqlite3") &&
                            file.text.contains("backup")

                        val hasUnsafeBackup = file.text.contains("cp ") &&
                            file.text.contains(".db")

                        hasProperBackup || !hasUnsafeBackup
                    }
            }

            it("database configuration should use consistent property names") {
                Konsist.scopeFromProduction()
                    .files
                    .filter { file ->
                        file.text.contains("DatabaseConfig") ||
                            file.text.contains("database.url") ||
                            file.text.contains("jdbc.url")
                    }
                    .assertTrue { file ->
                        // Should use consistent naming pattern
                        val usesDriver = file.text.contains("database.driver")
                        val usesJdbcUrl = file.text.contains("jdbc.url")

                        // Should not mix patterns
                        !(usesDriver && usesJdbcUrl)
                    }
            }
        }
    })
