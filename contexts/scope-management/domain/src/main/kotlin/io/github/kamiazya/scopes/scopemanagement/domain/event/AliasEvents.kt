package io.github.kamiazya.scopes.scopemanagement.domain.event

import arrow.core.Either
import io.github.kamiazya.scopes.eventstore.domain.valueobject.EventTypeId
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.AggregateIdError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Events related to ScopeAlias aggregate.
 */
sealed class AliasEvent : DomainEvent

/**
 * Event fired when an alias is assigned to a scope.
 */
@EventTypeId("scope-management.alias.assigned.v1")
data class AliasAssigned(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    val aliasId: AliasId,
    val aliasName: AliasName,
    val scopeId: ScopeId,
    val aliasType: AliasType,
) : AliasEvent() {
    companion object {
        fun from(alias: ScopeAlias, eventId: EventId): Either<AggregateIdError, AliasAssigned> = alias.id.toAggregateId().map { aggregateId ->
            AliasAssigned(
                aggregateId = aggregateId,
                eventId = eventId,
                aggregateVersion = AggregateVersion.initial().increment(),
                aliasId = alias.id,
                aliasName = alias.aliasName,
                scopeId = alias.scopeId,
                aliasType = alias.aliasType,
            )
        }
    }
}

/**
 * Event fired when an alias is removed from a scope.
 */
@EventTypeId("scope-management.alias.removed.v1")
data class AliasRemoved(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    val aliasId: AliasId,
    val aliasName: AliasName,
    val scopeId: ScopeId,
    val aliasType: AliasType,
) : AliasEvent()

/**
 * Event fired when an alias name is changed.
 * This is typically used when updating custom aliases.
 */
@EventTypeId("scope-management.alias.name-changed.v1")
data class AliasNameChanged(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    val aliasId: AliasId,
    val scopeId: ScopeId,
    val oldAliasName: AliasName,
    val newAliasName: AliasName,
) : AliasEvent()

/**
 * Event fired when a canonical alias is replaced with a new one.
 * This happens when regenerating the canonical alias for a scope.
 */
@EventTypeId("scope-management.alias.canonical-replaced.v1")
data class CanonicalAliasReplaced(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    val scopeId: ScopeId,
    val oldAliasId: AliasId,
    val oldAliasName: AliasName,
    val newAliasId: AliasId,
    val newAliasName: AliasName,
) : AliasEvent()
