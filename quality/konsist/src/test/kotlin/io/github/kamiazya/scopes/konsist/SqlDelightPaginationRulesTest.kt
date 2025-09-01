package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Konsist rules to prevent in-memory pagination on SQLDelight results
 * within the scope-management infrastructure repositories.
 */
class SqlDelightPaginationRulesTest : DescribeSpec({
    describe("SQLDelight pagination should be handled at the database layer") {
        it("scope-management repos should not call drop/take after executeAsList()") {
            val badPattern = Regex("(?s)executeAsList\\(\\).*?\\.(drop|take)\\(")

            Konsist
                .scopeFromProject()
                .files
                .filter {
                    it.path.contains("contexts/scope-management/infrastructure/") &&
                        it.path.contains("/repository/") &&
                        it.path.endsWith(".kt")
                }
                .assertTrue { file ->
                    !badPattern.containsMatchIn(file.text)
                }
        }
    }
})

