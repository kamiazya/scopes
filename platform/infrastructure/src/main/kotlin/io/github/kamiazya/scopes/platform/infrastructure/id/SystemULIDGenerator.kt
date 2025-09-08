package io.github.kamiazya.scopes.platform.infrastructure.id

import io.github.kamiazya.scopes.platform.commons.id.ULID
import io.github.kamiazya.scopes.platform.commons.id.ULIDGenerator
import com.github.guepardoapps.kulid.ULID as KULID

/**
 * System ULID generator that uses the KULID library.
 * This is the production implementation of ULIDGenerator.
 */
class SystemULIDGenerator : ULIDGenerator {
    override fun generate(): ULID = ULID.fromString(KULID.random())
}
