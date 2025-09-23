package io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization

import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle

/**
 * Helper functions for deserializing domain value objects from their string representations.
 * These functions handle the Either results from factory methods and throw meaningful errors
 * when deserialization fails.
 */
internal object ScopeEventSerializerHelpers {

    fun deserializeScopeId(value: String): ScopeId = ScopeId.create(value).fold(
        { error -> error("Invalid ScopeId: $value - $error") },
        { it },
    )

    fun deserializeScopeTitle(value: String): ScopeTitle = ScopeTitle.create(value).fold(
        { error -> error("Invalid ScopeTitle: $value - $error") },
        { it },
    )

    fun deserializeScopeDescription(value: String?): ScopeDescription? = value?.let {
        ScopeDescription.create(it).fold(
            { error -> error("Invalid ScopeDescription: $it - $error") },
            { it },
        )
    }

    fun deserializeAliasId(value: String): AliasId = AliasId.create(value).fold(
        { error -> error("Invalid AliasId: $value - $error") },
        { it },
    )

    fun deserializeAliasName(value: String): AliasName = AliasName.create(value).fold(
        { error -> error("Invalid AliasName: $value - $error") },
        { it },
    )

    fun deserializeAspectKey(value: String): AspectKey = AspectKey.create(value).fold(
        { error -> error("Invalid AspectKey: $value - $error") },
        { it },
    )

    fun deserializeAspectValue(value: String): AspectValue = AspectValue.create(value).fold(
        { error -> error("Invalid AspectValue: $value - $error") },
        { it },
    )

    fun deserializeAggregateVersion(value: Long): AggregateVersion = AggregateVersion.from(value).fold(
        { error -> error("Invalid AggregateVersion: $value - $error") },
        { it },
    )

    fun deserializeAggregateId(value: String): AggregateId = AggregateId.from(value).fold(
        { error -> error("Invalid AggregateId: $value - $error") },
        { it },
    )

    fun deserializeEventId(value: String): EventId = EventId.from(value).fold(
        { error -> error("Invalid EventId: $value - $error") },
        { it },
    )
}
