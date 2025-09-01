package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Konsist rules for cursor-based pagination validation in CLI commands.
 * Ensures that cursor options are always used in pairs and not mixed with offset paging.
 */
class CliPaginationRulesTest : StringSpec({

    "cursor pagination options should be validated and not combined with offset" {
        Konsist
            .scopeFromDirectory("interfaces/cli")
            .files
            .filter { it.name.contains("Command") && it.text.contains("--after-created-at") }
            .assertTrue { file ->
                file.text.contains("--after-id") &&
                    file.text.contains("afterTimeProvided xor afterIdProvided") &&
                    file.text.contains("--offset cannot be used with cursor options")
            }
    }
})
