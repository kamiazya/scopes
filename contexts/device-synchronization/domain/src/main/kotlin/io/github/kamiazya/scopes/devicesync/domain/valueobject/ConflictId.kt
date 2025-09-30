package io.github.kamiazya.scopes.devicesync.domain.valueobject

import io.github.kamiazya.scopes.platform.commons.id.ULID
import org.jmolecules.ddd.types.Identifier

/**
 * Identity for SyncConflict entity.
 */
@JvmInline
value class ConflictId(val value: String) : Identifier {
    companion object {
        fun generate(): ConflictId = ConflictId(ULID.generate().value)
    }
}
