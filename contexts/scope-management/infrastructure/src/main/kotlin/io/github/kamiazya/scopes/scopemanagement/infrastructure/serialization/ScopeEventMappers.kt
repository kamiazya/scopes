package io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization

import arrow.core.toNonEmptyListOrNull
import io.github.kamiazya.scopes.platform.domain.event.EventMetadata
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects

/**
 * Helper object for mapping between domain types and their serializable representations.
 */
object ScopeEventMappers {

    fun mapMetadata(metadata: EventMetadata): SerializableEventMetadata = SerializableEventMetadata(
        correlationId = metadata.correlationId,
        causationId = metadata.causationId?.value,
        userId = metadata.userId,
        timestamp = null, // The platform EventMetadata doesn't have timestamp
        additionalData = metadata.custom,
    )

    fun mapMetadataFromSurrogate(surrogate: SerializableEventMetadata): EventMetadata = EventMetadata(
        correlationId = surrogate.correlationId,
        causationId = surrogate.causationId?.let { EventId.from(it).fold({ error("Invalid EventId: $it") }, { it }) },
        userId = surrogate.userId,
        custom = surrogate.additionalData,
    )

    fun mapAspectsToSurrogate(aspects: Aspects): Map<String, List<String>> = aspects.toMap().mapKeys { it.key.value }.mapValues { entry ->
        entry.value.map { it.value }
    }

    fun mapAspectsFromSurrogate(surrogate: Map<String, List<String>>): Aspects {
        val aspectMap = surrogate.mapNotNull { (key, values) ->
            val aspectKey = AspectKey.create(key).getOrNull() ?: return@mapNotNull null
            val aspectValues = values.mapNotNull { value ->
                AspectValue.create(value).getOrNull()
            }
            val nonEmptyValues = aspectValues.toNonEmptyListOrNull() ?: return@mapNotNull null
            aspectKey to nonEmptyValues
        }.toMap()
        return Aspects.from(aspectMap)
    }
}
