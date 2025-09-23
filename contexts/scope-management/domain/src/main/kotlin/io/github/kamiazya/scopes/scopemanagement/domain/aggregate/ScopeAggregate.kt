package io.github.kamiazya.scopes.scopemanagement.domain.aggregate

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateResult
import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateRoot
import io.github.kamiazya.scopes.platform.domain.event.EventEnvelope
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.event.AliasAssigned
import io.github.kamiazya.scopes.scopemanagement.domain.event.AliasNameChanged
import io.github.kamiazya.scopes.scopemanagement.domain.event.AliasRemoved
import io.github.kamiazya.scopes.scopemanagement.domain.event.CanonicalAliasReplaced
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeArchived
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectAdded
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectRemoved
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectsCleared
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectsUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeCreated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeDeleted
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeDescriptionUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeEvent
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeParentChanged
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeRestored
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeTitleUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeStatus
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Internal data structure for managing aliases within the ScopeAggregate.
 * This replaces the external ScopeAlias Entity.
 */
data class AliasRecord(val aliasId: AliasId, val aliasName: AliasName, val aliasType: AliasType, val createdAt: Instant, val updatedAt: Instant)

/**
 * Scope aggregate root implementing event sourcing pattern.
 *
 * This aggregate encapsulates all business logic related to Scopes,
 * ensuring that all state changes go through proper domain events.
 * It maintains consistency boundaries and enforces business rules.
 *
 * Design principles:
 * - All state mutations must generate events
 * - Business rules are validated before generating events
 * - The aggregate can be reconstructed from its event history
 * - Commands return new instances (immutability)
 * - Scope state is managed internally (no external Entity dependency)
 */
data class ScopeAggregate(
    override val id: AggregateId,
    override val version: AggregateVersion,
    val createdAt: Instant,
    val updatedAt: Instant,
    // Core Scope properties (previously in Scope Entity)
    val scopeId: ScopeId?,
    val title: ScopeTitle?,
    val description: ScopeDescription?,
    val parentId: ScopeId?,
    val status: ScopeStatus,
    val aspects: Aspects,
    // Alias management (previously external Entity)
    val aliases: Map<AliasId, AliasRecord> = emptyMap(),
    val canonicalAliasId: AliasId? = null,
    // Aggregate-level state
    val isDeleted: Boolean = false,
    val isArchived: Boolean = false,
) : AggregateRoot<ScopeAggregate, ScopeEvent>() {

    companion object {
        /**
         * Reconstructs a ScopeAggregate from a list of domain events.
         * This is used for event sourcing replay.
         */
        private fun extractScopeId(event: ScopeEvent): ScopeId = when (event) {
            is ScopeCreated -> event.scopeId
            is ScopeDeleted -> event.scopeId
            is ScopeArchived -> event.scopeId
            is ScopeRestored -> event.scopeId
            is ScopeTitleUpdated -> event.scopeId
            is ScopeDescriptionUpdated -> event.scopeId
            is ScopeParentChanged -> event.scopeId
            is ScopeAspectAdded -> event.scopeId
            is ScopeAspectRemoved -> event.scopeId
            is ScopeAspectsCleared -> event.scopeId
            is ScopeAspectsUpdated -> event.scopeId
            is AliasAssigned -> event.scopeId
            is AliasRemoved -> event.scopeId
            is AliasNameChanged -> event.scopeId
            is CanonicalAliasReplaced -> event.scopeId
        }

        fun fromEvents(events: List<ScopeEvent>): Either<ScopesError, ScopeAggregate?> = either {
            if (events.isEmpty()) {
                return@either null
            }

            // Start with an empty aggregate and apply each event
            var aggregate: ScopeAggregate? = null

            for (event in events) {
                aggregate = when (event) {
                    is ScopeCreated -> {
                        // Initialize aggregate from creation event
                        ScopeAggregate(
                            id = event.aggregateId,
                            version = event.aggregateVersion,
                            createdAt = event.occurredAt,
                            updatedAt = event.occurredAt,
                            scopeId = event.scopeId,
                            title = event.title,
                            description = event.description,
                            parentId = event.parentId,
                            status = ScopeStatus.default(),
                            aspects = Aspects.empty(),
                            aliases = emptyMap(),
                            canonicalAliasId = null,
                            isDeleted = false,
                            isArchived = false,
                        )
                    }
                    else -> {
                        // Apply event to existing aggregate
                        aggregate?.applyEvent(event) ?: raise(
                            ScopeError.InvalidEventSequence(
                                scopeId = extractScopeId(event),
                                expectedEventType = "ScopeCreated",
                                actualEventType = event::class.simpleName ?: "UnknownEvent",
                                reason = "Cannot apply event without ScopeCreated event first",
                            ),
                        )
                    }
                }
            }

            aggregate
        }

        /**
         * Creates a new scope aggregate for a create command.
         * Generates a ScopeCreated event after validation.
         */
        fun create(
            title: String,
            description: String? = null,
            parentId: ScopeId? = null,
            scopeId: ScopeId? = null,
            now: Instant = Clock.System.now(),
        ): Either<ScopesError, ScopeAggregate> = either {
            val validatedTitle = ScopeTitle.create(title).bind()
            val validatedDescription = ScopeDescription.create(description).bind()
            val scopeId = scopeId ?: ScopeId.generate()
            val aggregateId = scopeId.toAggregateId().bind()
            val eventId = EventId.generate()

            val event = ScopeCreated(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = now,
                aggregateVersion = AggregateVersion.initial().increment(),
                scopeId = scopeId,
                title = validatedTitle,
                description = validatedDescription,
                parentId = parentId,
            )

            val initialAggregate = ScopeAggregate(
                id = aggregateId,
                version = AggregateVersion.initial(),
                createdAt = now,
                updatedAt = now,
                scopeId = null,
                title = null,
                description = null,
                parentId = null,
                status = ScopeStatus.default(),
                aspects = Aspects.empty(),
                aliases = emptyMap(),
                canonicalAliasId = null,
                isDeleted = false,
                isArchived = false,
            )

            initialAggregate.raiseEvent(event)
        }

        /**
         * Creates a scope using decide/evolve pattern.
         * Returns an AggregateResult with the new aggregate and pending events.
         */
        fun handleCreate(
            title: String,
            description: String? = null,
            parentId: ScopeId? = null,
            scopeId: ScopeId? = null,
            now: Instant = Clock.System.now(),
        ): Either<ScopesError, AggregateResult<ScopeAggregate, ScopeEvent>> = either {
            val validatedTitle = ScopeTitle.create(title).bind()
            val validatedDescription = ScopeDescription.create(description).bind()
            val scopeId = scopeId ?: ScopeId.generate()
            val aggregateId = scopeId.toAggregateId().bind()

            val initialAggregate = ScopeAggregate(
                id = aggregateId,
                version = AggregateVersion.initial(),
                createdAt = now,
                updatedAt = now,
                scopeId = null,
                title = null,
                description = null,
                parentId = null,
                status = ScopeStatus.default(),
                aspects = Aspects.empty(),
                aliases = emptyMap(),
                canonicalAliasId = null,
                isDeleted = false,
                isArchived = false,
            )

            // Decide phase - create events with dummy version
            val event = ScopeCreated(
                aggregateId = aggregateId,
                eventId = EventId.generate(),
                occurredAt = now,
                aggregateVersion = AggregateVersion.initial(), // Dummy version
                scopeId = scopeId,
                title = validatedTitle,
                description = validatedDescription,
                parentId = parentId,
            )

            val pendingEvents = listOf(EventEnvelope.Pending(event))

            // Evolve phase - apply events to aggregate
            val evolvedAggregate = pendingEvents.fold(initialAggregate) { aggregate, eventEnvelope ->
                aggregate.applyEvent(eventEnvelope.event)
            }

            AggregateResult(
                aggregate = evolvedAggregate,
                events = pendingEvents,
                baseVersion = AggregateVersion.initial(),
            )
        }

        /**
         * Creates a scope with a canonical alias using decide/evolve pattern.
         * Returns an AggregateResult with the new aggregate and pending events.
         */
        fun handleCreateWithAlias(
            title: String,
            description: String? = null,
            parentId: ScopeId? = null,
            aliasName: AliasName,
            scopeId: ScopeId? = null,
            now: Instant = Clock.System.now(),
        ): Either<ScopesError, AggregateResult<ScopeAggregate, ScopeEvent>> = either {
            val validatedTitle = ScopeTitle.create(title).bind()
            val validatedDescription = ScopeDescription.create(description).bind()
            val scopeId = scopeId ?: ScopeId.generate()
            val aggregateId = scopeId.toAggregateId().bind()
            val aliasId = AliasId.generate()

            val initialAggregate = ScopeAggregate(
                id = aggregateId,
                version = AggregateVersion.initial(),
                createdAt = now,
                updatedAt = now,
                scopeId = null,
                title = null,
                description = null,
                parentId = null,
                status = ScopeStatus.default(),
                aspects = Aspects.empty(),
                aliases = emptyMap(),
                canonicalAliasId = null,
                isDeleted = false,
                isArchived = false,
            )

            // Create events - first scope creation, then alias assignment
            val scopeCreatedEvent = ScopeCreated(
                aggregateId = aggregateId,
                eventId = EventId.generate(),
                occurredAt = now,
                aggregateVersion = AggregateVersion.initial(), // Dummy version
                scopeId = scopeId,
                title = validatedTitle,
                description = validatedDescription,
                parentId = parentId,
            )

            val aliasAssignedEvent = AliasAssigned(
                aggregateId = aggregateId,
                eventId = EventId.generate(),
                occurredAt = now,
                aggregateVersion = AggregateVersion.initial(), // Dummy version
                aliasId = aliasId,
                aliasName = aliasName,
                scopeId = scopeId,
                aliasType = AliasType.CANONICAL,
            )

            val pendingEvents = listOf(
                EventEnvelope.Pending(scopeCreatedEvent),
                EventEnvelope.Pending(aliasAssignedEvent),
            )

            // Evolve phase - apply events to aggregate
            val evolvedAggregate = pendingEvents.fold(initialAggregate) { aggregate, eventEnvelope ->
                aggregate.applyEvent(eventEnvelope.event)
            }

            AggregateResult(
                aggregate = evolvedAggregate,
                events = pendingEvents,
                baseVersion = AggregateVersion.initial(),
            )
        }

        /**
         * Creates a scope with automatic alias generation.
         * This version eliminates external dependency on AliasGenerationService
         * by using internal alias generation logic based on the scope ID.
         */
        fun handleCreateWithAutoAlias(
            title: String,
            description: String? = null,
            parentId: ScopeId? = null,
            scopeId: ScopeId? = null,
            now: Instant = Clock.System.now(),
        ): Either<ScopesError, AggregateResult<ScopeAggregate, ScopeEvent>> = either {
            val validatedTitle = ScopeTitle.create(title).bind()
            val validatedDescription = ScopeDescription.create(description).bind()
            val scopeId = scopeId ?: ScopeId.generate()
            val aggregateId = scopeId.toAggregateId().bind()
            val aliasId = AliasId.generate()

            // Generate alias internally using scope ID as seed
            val generatedAliasName = generateAliasFromScopeId(scopeId).bind()

            val initialAggregate = ScopeAggregate(
                id = aggregateId,
                version = AggregateVersion.initial(),
                createdAt = now,
                updatedAt = now,
                scopeId = null,
                title = null,
                description = null,
                parentId = null,
                status = ScopeStatus.default(),
                aspects = Aspects.empty(),
                aliases = emptyMap(),
                canonicalAliasId = null,
                isDeleted = false,
                isArchived = false,
            )

            // Create events - first scope creation, then alias assignment
            val scopeCreatedEvent = ScopeCreated(
                aggregateId = aggregateId,
                eventId = EventId.generate(),
                occurredAt = now,
                aggregateVersion = AggregateVersion.initial().increment(),
                scopeId = scopeId,
                title = validatedTitle,
                description = validatedDescription,
                parentId = parentId,
            )

            val aliasAssignedEvent = AliasAssigned(
                aggregateId = aggregateId,
                eventId = EventId.generate(),
                occurredAt = now,
                aggregateVersion = AggregateVersion.initial().increment().increment(),
                aliasId = aliasId,
                aliasName = generatedAliasName,
                scopeId = scopeId,
                aliasType = AliasType.CANONICAL,
            )

            val pendingEvents = listOf(
                EventEnvelope.Pending(scopeCreatedEvent),
                EventEnvelope.Pending(aliasAssignedEvent),
            )

            // Evolve phase - apply events to aggregate
            val evolvedAggregate = pendingEvents.fold(initialAggregate) { aggregate, eventEnvelope ->
                val appliedAggregate = aggregate.applyEvent(eventEnvelope.event)
                // Debug: Ensure the aggregate is not null after applying event
                appliedAggregate ?: error("Aggregate became null after applying event: ${eventEnvelope.event}")
            }

            AggregateResult(
                aggregate = evolvedAggregate,
                events = pendingEvents,
                baseVersion = AggregateVersion.initial(),
            )
        }

        /**
         * Generates an alias name based on the scope ID.
         * This provides deterministic alias generation without external dependencies.
         */
        private fun generateAliasFromScopeId(scopeId: ScopeId): Either<ScopesError, AliasName> = either {
            // Simple deterministic alias generation using scope ID hash
            val adjectives = listOf("quick", "bright", "gentle", "swift", "calm", "bold", "quiet", "wise", "brave", "kind")
            val nouns = listOf("river", "mountain", "ocean", "forest", "star", "moon", "cloud", "wind", "light", "stone")

            val hash = scopeId.value.hashCode()
            val adjIndex = kotlin.math.abs(hash) % adjectives.size
            val nounIndex = kotlin.math.abs(hash / adjectives.size) % nouns.size
            val suffix = kotlin.math.abs(hash / (adjectives.size * nouns.size)) % 1000

            val aliasString = "${adjectives[adjIndex]}-${nouns[nounIndex]}-${suffix.toString().padStart(3, '0')}"
            AliasName.create(aliasString).bind()
        }

        /**
         * Creates an empty aggregate for event replay.
         * Used when loading an aggregate from the event store.
         */
        fun empty(aggregateId: AggregateId): ScopeAggregate = ScopeAggregate(
            id = aggregateId,
            version = AggregateVersion.initial(),
            createdAt = Instant.DISTANT_PAST,
            updatedAt = Instant.DISTANT_PAST,
            scopeId = null,
            title = null,
            description = null,
            parentId = null,
            status = ScopeStatus.default(),
            aspects = Aspects.empty(),
            aliases = emptyMap(),
            canonicalAliasId = null,
            isDeleted = false,
            isArchived = false,
        )
    }

    /**
     * Updates the scope title after validation.
     * Ensures the scope exists and is not deleted.
     */
    fun updateTitle(title: String, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensureNotNull(this@ScopeAggregate.title) {
            ScopeError.NotFound(scopeId!!)
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }

        val newTitle = ScopeTitle.create(title).bind()
        if (this@ScopeAggregate.title == newTitle) {
            return@either this@ScopeAggregate
        }

        val event = ScopeTitleUpdated(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            scopeId = scopeId!!,
            oldTitle = this@ScopeAggregate.title!!,
            newTitle = newTitle,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Decides whether to update the title (decide phase).
     * Returns pending events or empty list if no change needed.
     */
    fun decideUpdateTitle(title: String, now: Instant = Clock.System.now()): Either<ScopesError, List<EventEnvelope.Pending<ScopeEvent>>> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensureNotNull(this@ScopeAggregate.title) {
            ScopeError.NotFound(scopeId!!)
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }

        val newTitle = ScopeTitle.create(title).bind()
        if (this@ScopeAggregate.title == newTitle) {
            return@either emptyList()
        }

        val event = ScopeTitleUpdated(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = AggregateVersion.initial(), // Dummy version
            scopeId = scopeId!!,
            oldTitle = this@ScopeAggregate.title!!,
            newTitle = newTitle,
        )

        listOf(EventEnvelope.Pending(event))
    }

    /**
     * Handles update title command using decide/evolve pattern.
     * Returns an AggregateResult with the updated aggregate and pending events.
     */
    fun handleUpdateTitle(title: String, now: Instant = Clock.System.now()): Either<ScopesError, AggregateResult<ScopeAggregate, ScopeEvent>> = either {
        val pendingEvents = decideUpdateTitle(title, now).bind()

        if (pendingEvents.isEmpty()) {
            return@either AggregateResult(
                aggregate = this@ScopeAggregate,
                events = emptyList(),
                baseVersion = version,
            )
        }

        // Evolve phase - apply events to aggregate
        val evolvedAggregate = pendingEvents.fold(this@ScopeAggregate) { agg, envelope ->
            agg.applyEvent(envelope.event)
        }

        AggregateResult(
            aggregate = evolvedAggregate,
            events = pendingEvents,
            baseVersion = version,
        )
    }

    /**
     * Handles description update command in Event Sourcing pattern.
     * This follows the decide/evolve pattern similar to handleUpdateTitle.
     */
    fun handleUpdateDescription(description: String?, now: Instant = Clock.System.now()): Either<ScopesError, AggregateResult<ScopeAggregate, ScopeEvent>> =
        either {
            val pendingEvents = decideUpdateDescription(description, now).bind()

            if (pendingEvents.isEmpty()) {
                return@either AggregateResult(
                    aggregate = this@ScopeAggregate,
                    events = emptyList(),
                    baseVersion = version,
                )
            }

            // Evolve phase - apply events to aggregate
            val evolvedAggregate = pendingEvents.fold(this@ScopeAggregate) { agg, envelope ->
                agg.applyEvent(envelope.event)
            }

            AggregateResult(
                aggregate = evolvedAggregate,
                events = pendingEvents,
                baseVersion = version,
            )
        }

    /**
     * Decides if description update should occur and generates appropriate events.
     */
    fun decideUpdateDescription(description: String?, now: Instant = Clock.System.now()): Either<ScopesError, List<EventEnvelope.Pending<ScopeEvent>>> =
        either {
            ensureNotNull(scopeId) {
                ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
            }
            ensure(!isDeleted) {
                ScopeError.AlreadyDeleted(scopeId!!)
            }

            val newDescription = ScopeDescription.create(description).bind()
            if (this@ScopeAggregate.description == newDescription) {
                return@either emptyList()
            }

            val event = ScopeDescriptionUpdated(
                aggregateId = id,
                eventId = EventId.generate(),
                occurredAt = now,
                aggregateVersion = version.increment(),
                scopeId = scopeId!!,
                oldDescription = this@ScopeAggregate.description,
                newDescription = newDescription,
            )

            listOf(EventEnvelope.Pending(event))
        }

    /**
     * Updates the scope description after validation.
     */
    fun updateDescription(description: String?, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }

        val newDescription = ScopeDescription.create(description).bind()
        if (this@ScopeAggregate.description == newDescription) {
            return@either this@ScopeAggregate
        }

        val event = ScopeDescriptionUpdated(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            scopeId = scopeId!!,
            oldDescription = this@ScopeAggregate.description,
            newDescription = newDescription,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Changes the parent of the scope.
     * Validates hierarchy constraints before applying the change.
     */
    fun changeParent(newParentId: ScopeId?, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }

        if (this@ScopeAggregate.parentId == newParentId) {
            return@either this@ScopeAggregate
        }

        val event = ScopeParentChanged(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            scopeId = scopeId!!,
            oldParentId = this@ScopeAggregate.parentId,
            newParentId = newParentId,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Deletes the scope.
     * Soft delete that marks the scope as deleted.
     */
    fun delete(now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }

        val event = ScopeDeleted(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            scopeId = scopeId!!,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Handles delete command in Event Sourcing pattern.
     * This follows the decide/evolve pattern similar to other handle methods.
     */
    fun handleDelete(now: Instant = Clock.System.now()): Either<ScopesError, AggregateResult<ScopeAggregate, ScopeEvent>> = either {
        val pendingEvents = decideDelete(now).bind()

        if (pendingEvents.isEmpty()) {
            return@either AggregateResult(
                aggregate = this@ScopeAggregate,
                events = emptyList(),
                baseVersion = version,
            )
        }

        // Evolve phase - apply events to aggregate
        val evolvedAggregate = pendingEvents.fold(this@ScopeAggregate) { agg, envelope ->
            agg.applyEvent(envelope.event)
        }

        AggregateResult(
            aggregate = evolvedAggregate,
            events = pendingEvents,
            baseVersion = version,
        )
    }

    /**
     * Decides if delete should occur and generates appropriate events.
     */
    fun decideDelete(now: Instant = Clock.System.now()): Either<ScopesError, List<EventEnvelope.Pending<ScopeEvent>>> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }

        val event = ScopeDeleted(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            scopeId = scopeId!!,
        )

        listOf(EventEnvelope.Pending(event))
    }

    /**
     * Archives the scope.
     * Archived scopes are hidden but can be restored.
     */
    fun archive(now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }
        ensure(!isArchived) {
            ScopeError.AlreadyArchived(scopeId!!)
        }

        val event = ScopeArchived(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            scopeId = scopeId!!,
            reason = null,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Restores an archived scope.
     */
    fun restore(now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }
        ensure(isArchived) {
            ScopeError.NotArchived(scopeId!!)
        }

        val event = ScopeRestored(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            scopeId = scopeId!!,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    // ===== ALIAS MANAGEMENT =====

    /**
     * Adds a new alias to the scope.
     * The first alias added becomes the canonical alias.
     */
    fun addAlias(aliasName: AliasName, aliasType: AliasType = AliasType.CUSTOM, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> =
        either {
            ensureNotNull(scopeId) {
                ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
            }
            ensure(!isDeleted) {
                ScopeError.AlreadyDeleted(scopeId!!)
            }

            // Check if alias name already exists
            val existingAlias = aliases.values.find { it.aliasName == aliasName }
            ensure(existingAlias == null) {
                ScopeError.DuplicateAlias(aliasName.value, scopeId!!)
            }

            val aliasId = AliasId.generate()
            val finalAliasType = if (canonicalAliasId == null) AliasType.CANONICAL else aliasType

            val event = AliasAssigned(
                aggregateId = id,
                eventId = EventId.generate(),
                occurredAt = now,
                aggregateVersion = version.increment(),
                aliasId = aliasId,
                aliasName = aliasName,
                scopeId = scopeId!!,
                aliasType = finalAliasType,
            )

            this@ScopeAggregate.raiseEvent(event)
        }

    /**
     * Removes an alias from the scope.
     * Canonical aliases cannot be removed, only replaced.
     */
    fun removeAlias(aliasId: AliasId, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }

        val aliasRecord = aliases[aliasId]
        ensureNotNull(aliasRecord) {
            ScopeError.AliasNotFound(aliasId.value, scopeId!!)
        }

        // Cannot remove canonical alias
        ensure(aliasRecord.aliasType != AliasType.CANONICAL) {
            ScopeError.CannotRemoveCanonicalAlias(aliasId.value, scopeId!!)
        }

        val event = AliasRemoved(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            aliasId = aliasId,
            aliasName = aliasRecord.aliasName,
            scopeId = scopeId!!,
            aliasType = aliasRecord.aliasType,
            removedAt = now,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Replaces the canonical alias with a new one.
     * The old canonical alias becomes a custom alias.
     */
    fun replaceCanonicalAlias(newAliasName: AliasName, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensureNotNull(canonicalAliasId) {
            ScopeError.NoCanonicalAlias(scopeId!!)
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }

        val currentCanonical = aliases[canonicalAliasId!!]!!
        val newAliasId = AliasId.generate()

        val event = CanonicalAliasReplaced(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            scopeId = scopeId!!,
            oldAliasId = canonicalAliasId!!,
            oldAliasName = currentCanonical.aliasName,
            newAliasId = newAliasId,
            newAliasName = newAliasName,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Gets the canonical alias for the scope.
     */
    fun getCanonicalAlias(): AliasRecord? = canonicalAliasId?.let { aliases[it] }

    /**
     * Gets all custom aliases for the scope.
     */
    fun getCustomAliases(): List<AliasRecord> = aliases.values.filter { it.aliasType == AliasType.CUSTOM }

    /**
     * Gets all aliases for the scope.
     */
    fun getAllAliases(): List<AliasRecord> = aliases.values.toList()

    /**
     * Finds an alias by name.
     */
    fun findAliasByName(aliasName: AliasName): AliasRecord? = aliases.values.find { it.aliasName == aliasName }

    /**
     * Changes the name of an existing alias.
     * Both canonical and custom aliases can be renamed.
     */
    fun changeAliasName(aliasId: AliasId, newAliasName: AliasName, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }

        val aliasRecord = aliases[aliasId]
        ensureNotNull(aliasRecord) {
            ScopeError.AliasNotFound(aliasId.value, scopeId!!)
        }

        // Check if new alias name already exists
        val existingAlias = aliases.values.find { it.aliasName == newAliasName && it.aliasId != aliasId }
        ensure(existingAlias == null) {
            ScopeError.DuplicateAlias(newAliasName.value, scopeId!!)
        }

        val event = AliasNameChanged(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            aliasId = aliasId,
            scopeId = scopeId!!,
            oldAliasName = aliasRecord.aliasName,
            newAliasName = newAliasName,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    // ===== ASPECT MANAGEMENT =====

    /**
     * Adds an aspect value to the scope.
     */
    fun addAspect(aspectKey: AspectKey, aspectValues: NonEmptyList<AspectValue>, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> =
        either {
            ensureNotNull(scopeId) {
                ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
            }
            ensure(!isDeleted) {
                ScopeError.AlreadyDeleted(scopeId!!)
            }

            val event = ScopeAspectAdded(
                aggregateId = id,
                eventId = EventId.generate(),
                occurredAt = now,
                aggregateVersion = version.increment(),
                scopeId = scopeId!!,
                aspectKey = aspectKey,
                aspectValues = aspectValues,
            )

            this@ScopeAggregate.raiseEvent(event)
        }

    /**
     * Removes an aspect from the scope.
     */
    fun removeAspect(aspectKey: AspectKey, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }

        // Check if aspect exists
        ensure(aspects.contains(aspectKey)) {
            ScopeError.AspectNotFound(aspectKey.value, scopeId!!)
        }

        val event = ScopeAspectRemoved(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            scopeId = scopeId!!,
            aspectKey = aspectKey,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Clears all aspects from the scope.
     */
    fun clearAspects(now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }

        val event = ScopeAspectsCleared(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            scopeId = scopeId!!,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Updates multiple aspects at once.
     */
    fun updateAspects(newAspects: Aspects, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeId) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scopeId!!)
        }

        if (aspects == newAspects) {
            return@either this@ScopeAggregate
        }

        val event = ScopeAspectsUpdated(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,
            aggregateVersion = version.increment(),
            scopeId = scopeId!!,
            oldAspects = aspects,
            newAspects = newAspects,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Applies an event to update the aggregate state.
     * This is the core of the event sourcing pattern.
     *
     * Note: Each event application increments the version by 1.
     * We don't use event.aggregateVersion directly, but instead
     * increment based on the number of events applied.
     */
    override fun applyEvent(event: ScopeEvent): ScopeAggregate = when (event) {
        is ScopeCreated -> copy(
            version = version.increment(),
            createdAt = event.occurredAt,
            updatedAt = event.occurredAt,
            scopeId = event.scopeId,
            title = event.title,
            description = event.description,
            parentId = event.parentId,
            status = ScopeStatus.default(),
            aspects = Aspects.empty(),
        )

        is ScopeTitleUpdated -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            title = event.newTitle,
        )

        is ScopeDescriptionUpdated -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            description = event.newDescription,
        )

        is ScopeParentChanged -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            parentId = event.newParentId,
        )

        is ScopeDeleted -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            isDeleted = true,
        )

        is ScopeArchived -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            isArchived = true,
        )

        is ScopeRestored -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            isArchived = false,
        )

        // Alias Events
        is AliasAssigned -> {
            val aliasRecord = AliasRecord(
                aliasId = event.aliasId,
                aliasName = event.aliasName,
                aliasType = event.aliasType,
                createdAt = event.occurredAt,
                updatedAt = event.occurredAt,
            )
            copy(
                version = version.increment(),
                updatedAt = event.occurredAt,
                aliases = aliases + (event.aliasId to aliasRecord),
                canonicalAliasId = if (event.aliasType == AliasType.CANONICAL) event.aliasId else canonicalAliasId,
            )
        }

        is AliasRemoved -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            aliases = aliases - event.aliasId,
        )

        is CanonicalAliasReplaced -> {
            // Add new canonical alias and demote old to custom
            val oldAliasRecord = aliases[event.oldAliasId]!!.copy(
                aliasType = AliasType.CUSTOM,
                updatedAt = event.occurredAt,
            )
            val newAliasRecord = AliasRecord(
                aliasId = event.newAliasId,
                aliasName = event.newAliasName,
                aliasType = AliasType.CANONICAL,
                createdAt = event.occurredAt,
                updatedAt = event.occurredAt,
            )
            copy(
                version = version.increment(),
                updatedAt = event.occurredAt,
                aliases = aliases + (event.oldAliasId to oldAliasRecord) + (event.newAliasId to newAliasRecord),
                canonicalAliasId = event.newAliasId,
            )
        }

        is AliasNameChanged -> {
            val updatedAlias = aliases[event.aliasId]!!.copy(
                aliasName = event.newAliasName,
                updatedAt = event.occurredAt,
            )
            copy(
                version = version.increment(),
                updatedAt = event.occurredAt,
                aliases = aliases + (event.aliasId to updatedAlias),
            )
        }

        // Aspect Events
        is ScopeAspectAdded -> {
            val updatedAspects = aspects.add(event.aspectKey, event.aspectValues)
            copy(
                version = version.increment(),
                updatedAt = event.occurredAt,
                aspects = updatedAspects,
            )
        }

        is ScopeAspectRemoved -> {
            val updatedAspects = aspects.remove(event.aspectKey)
            copy(
                version = version.increment(),
                updatedAt = event.occurredAt,
                aspects = updatedAspects,
            )
        }

        is ScopeAspectsCleared -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            aspects = Aspects.empty(),
        )

        is ScopeAspectsUpdated -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            aspects = event.newAspects,
        )
    }

    fun validateVersion(expectedVersion: Long, now: Instant = Clock.System.now()): Either<ScopesError, Unit> = either {
        val versionValue = version.value
        if (versionValue.toLong() != expectedVersion) {
            raise(
                ScopeError.VersionMismatch(
                    scopeId = scopeId ?: ScopeId.create(id.value.substringAfterLast("/")).bind(),
                    expectedVersion = expectedVersion,
                    actualVersion = versionValue.toLong(),
                ),
            )
        }
    }
}
