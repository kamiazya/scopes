package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Architecture rule: Error types must not contain occurredAt property.
 *
 * Rationale: Timestamps belong to events/audit logs, not error models.
 */
class ErrorTypeNoOccurredAtTest :
    DescribeSpec({
        describe("Error types should not expose occurredAt") {
            it("production error classes/interfaces have no occurredAt property") {
                Konsist.scopeFromProduction()
                    .classes()
                    .filter { klass ->
                        val name = klass.name ?: ""
                        name.endsWith("Error") || name.endsWith("Errors")
                    }
                    .assertTrue { klass ->
                        klass.properties().none { it.name == "occurredAt" }
                    }
            }
        }
    })
