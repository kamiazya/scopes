package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Konsist rules to prevent order-sensitive assertions in repository tests
 * unless ordering is explicitly guaranteed by the repository contract.
 */
class RepositoryOrderingConsistencyTest : DescribeSpec({
    describe("Repository ordering consistency") {

        it("repository tests should avoid order-sensitive list equality for general queries") {
            val orderSensitiveMatchers = listOf(
                "shouldBe listOf(",
                "shouldContainExactly",
                "shouldBeExactly",
            )

            val repoCallsWithoutExplicitOrder = listOf(
                // General fetches where ordering should not be asserted strictly in tests
                ".findAll()",
                ".findAll(", // paginated overload
                ".listAll(",
                ".findByAliasNamePrefix(",
            )

            Konsist.scopeFromTest()
                .files
                .withNameEndingWith("RepositoryTest.kt")
                .flatMap { it.functions() }
                .assertTrue { function ->
                    val text = function.text
                    val usesOrderSensitiveMatcher = orderSensitiveMatchers.any { text.contains(it) }
                    val callsNonOrderedRepoMethod = repoCallsWithoutExplicitOrder.any { text.contains(it) }

                    // If a test calls a non-ordered repository method, it should not assert exact order
                    !(usesOrderSensitiveMatcher && callsNonOrderedRepoMethod)
                }
        }
    }
})

