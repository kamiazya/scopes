package io.github.kamiazya.scopes.platform.domain.id

import io.github.kamiazya.scopes.platform.commons.id.ULID
import io.github.kamiazya.scopes.platform.commons.id.ULIDGenerator

/**
 * Test ULID generator that allows controlling ID generation for deterministic testing.
 * This implementation is useful for domain testing where IDs need to be predictable.
 */
class TestULIDGenerator(private val predefinedIds: Iterator<String>) : ULIDGenerator {

    override fun generate(): ULID = ULID.fromString(predefinedIds.next())

    companion object {
        fun withSequence(vararg ids: String): TestULIDGenerator = TestULIDGenerator(ids.iterator())

        fun withPattern(pattern: String, count: Int): TestULIDGenerator =
            TestULIDGenerator((1..count).map { "${pattern}${it.toString().padStart(22, '0')}" }.iterator())
    }
}
