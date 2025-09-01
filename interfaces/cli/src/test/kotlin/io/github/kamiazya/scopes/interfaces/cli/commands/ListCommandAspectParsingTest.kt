package io.github.kamiazya.scopes.interfaces.cli.commands

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldContainExactly

class ListCommandAspectParsingTest : DescribeSpec({
    describe("parseAspectFilters") {
        it("parses key:value and key=value formats") {
            val result = parseAspectFilters(listOf("priority:high", "status=active"))

            result.shouldContainExactly(
                mapOf(
                    "priority" to listOf("high"),
                    "status" to listOf("active"),
                ),
            )
        }

        it("trims whitespace around keys and values") {
            val result = parseAspectFilters(listOf("  priority  :  high  ", " status = active "))

            result.shouldContainExactly(
                mapOf(
                    "priority" to listOf("high"),
                    "status" to listOf("active"),
                ),
            )
        }

        it("groups multiple values for the same key") {
            val result = parseAspectFilters(listOf("priority:high", "priority=critical", "status:ready"))

            // Order of values is not important
            result["priority"]!!.shouldContainExactlyInAnyOrder("high", "critical")
            result["status"]!!.shouldContainExactlyInAnyOrder("ready")
        }

        it("ignores invalid entries without key or value") {
            val result = parseAspectFilters(listOf(":nope", "=nope", "keyonly:", "valu e", ""))

            result.shouldContainExactly(emptyMap())
        }
    }
})

