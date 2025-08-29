package io.github.kamiazya.scopes.scopemanagement.infrastructure.sqlite.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectCriteria
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectCriterion
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ComparisonOperator
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.LogicalOperator
import kotlinx.serialization.Serializable

/**
 * Serializable representation of AspectCriteria for database storage.
 */
@Serializable
sealed class SerializableAspectCriteria {
    @Serializable
    data class Single(val key: String, val operator: String, val value: String) : SerializableAspectCriteria()

    @Serializable
    data class Compound(val left: SerializableAspectCriteria, val operator: String, val right: SerializableAspectCriteria) : SerializableAspectCriteria()
}

/**
 * Extension functions to convert between domain and serializable representations.
 */
fun AspectCriteria.toSerializable(): SerializableAspectCriteria = when (this) {
    is AspectCriteria.Single -> SerializableAspectCriteria.Single(
        key = criterion.key.value,
        operator = criterion.operator.name,
        value = criterion.value.value,
    )
    is AspectCriteria.Compound -> SerializableAspectCriteria.Compound(
        left = left.toSerializable(),
        operator = operator.name,
        right = right.toSerializable(),
    )
}

fun SerializableAspectCriteria.toDomain(): AspectCriteria = when (this) {
    is SerializableAspectCriteria.Single -> AspectCriteria.Single(
        AspectCriterion(
            key = AspectKey.create(key).fold(
                ifLeft = { throw IllegalArgumentException("Invalid aspect key: $it") },
                ifRight = { it },
            ),
            operator = ComparisonOperator.valueOf(operator),
            value = AspectValue.create(value).fold(
                ifLeft = { throw IllegalArgumentException("Invalid aspect value: $it") },
                ifRight = { it },
            ),
        ),
    )
    is SerializableAspectCriteria.Compound -> AspectCriteria.Compound(
        left = left.toDomain(),
        operator = LogicalOperator.valueOf(operator),
        right = right.toDomain(),
    )
}
