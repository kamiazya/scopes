package io.github.kamiazya.scopes.domain.event

import arrow.core.Either
import io.github.kamiazya.scopes.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.domain.error.AggregateIdError
import io.github.kamiazya.scopes.domain.valueobject.AggregateId
import io.github.kamiazya.scopes.domain.valueobject.EventId
import io.github.kamiazya.scopes.domain.valueobject.AliasId
import io.github.kamiazya.scopes.domain.valueobject.AliasName
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.AliasType
import kotlinx.datetime.Instant

/**
 * Events related to ScopeAlias aggregate.
 */
sealed class AliasEvent : DomainEvent()

/**
 * Event fired when an alias is assigned to a scope.
 */
data class AliasAssigned(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val aliasId: AliasId,
    val aliasName: AliasName,
    val scopeId: ScopeId,
    val aliasType: AliasType
) : AliasEvent() {
    companion object {
        fun from(alias: ScopeAlias, eventId: EventId): Either<AggregateIdError, AliasAssigned> {
            return alias.id.toAggregateId().map { aggregateId ->
                AliasAssigned(
                    aggregateId = aggregateId,
                    eventId = eventId,
                    occurredAt = alias.createdAt,
                    version = 1,
                    aliasId = alias.id,
                    aliasName = alias.aliasName,
                    scopeId = alias.scopeId,
                    aliasType = alias.aliasType
                )
            }
        }
    }
}

/**
 * Event fired when an alias is removed from a scope.
 */
data class AliasRemoved(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val aliasId: AliasId,
    val aliasName: AliasName,
    val scopeId: ScopeId,
    val aliasType: AliasType,
    val removedAt: Instant
) : AliasEvent()

/**
 * Event fired when an alias name is changed.
 * This is typically used when updating custom aliases.
 */
data class AliasNameChanged(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val aliasId: AliasId,
    val scopeId: ScopeId,
    val oldAliasName: AliasName,
    val newAliasName: AliasName
) : AliasEvent()

/**
 * Event fired when a canonical alias is replaced with a new one.
 * This happens when regenerating the canonical alias for a scope.
 */
data class CanonicalAliasReplaced(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val scopeId: ScopeId,
    val oldAliasId: AliasId,
    val oldAliasName: AliasName,
    val newAliasId: AliasId,
    val newAliasName: AliasName
) : AliasEvent()
