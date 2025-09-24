package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.UpdateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.UpdateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.EventPublisher
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import kotlinx.datetime.Clock

private typealias PendingEventEnvelope = io.github.kamiazya.scopes.platform.domain.event.EventEnvelope.Pending<
    io.github.kamiazya.scopes.platform.domain.event.DomainEvent,
    >

/**
 * Handler for UpdateScope command using Event Sourcing pattern.
 *
 * This handler uses the event-sourced approach where:
 * - Updates are handled through ScopeAggregate methods
 * - All changes go through domain events
 * - EventSourcingRepository handles persistence
 * - No separate repositories needed
 */
class UpdateScopeHandler(
    private val eventSourcingRepository: EventSourcingRepository<DomainEvent>,
    private val eventPublisher: EventPublisher,
    private val scopeRepository: ScopeRepository,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : CommandHandler<UpdateScopeCommand, ScopeContractError, UpdateScopeResult> {

    override suspend operator fun invoke(command: UpdateScopeCommand): Either<ScopeContractError, UpdateScopeResult> =
        either<ScopeContractError, UpdateScopeResult> {
            logCommandStart(command)

            transactionManager.inTransaction<ScopeContractError, UpdateScopeResult> {
                either<ScopeContractError, UpdateScopeResult> {
                    val baseAggregate = loadExistingAggregate(command.id).bind()
                    val updateResult = applyUpdates(baseAggregate, command).bind()
                    persistChangesIfNeeded(updateResult.aggregate, updateResult.events, baseAggregate).bind()
                    buildResult(updateResult.aggregate, command.id)
                }
            }.bind()
        }.onLeft { error -> logCommandFailure(error) }

    private fun logCommandStart(command: UpdateScopeCommand) {
        logger.info(
            "Updating scope using EventSourcing pattern",
            mapOf(
                "scopeId" to command.id,
                "hasTitle" to (command.title != null).toString(),
                "hasDescription" to (command.description != null).toString(),
            ),
        )
    }

    private suspend fun loadExistingAggregate(
        scopeIdString: String,
    ): Either<ScopeContractError, io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate> = either {
        // Parse scope ID
        val scopeId = ScopeId.create(scopeIdString).mapLeft { idError ->
            logger.warn("Invalid scope ID format", mapOf("scopeId" to scopeIdString))
            applicationErrorMapper.mapDomainError(
                idError,
                ErrorMappingContext(attemptedValue = scopeIdString),
            )
        }.bind()

        // Load current aggregate from events
        val aggregateId = scopeId.toAggregateId().mapLeft { error ->
            applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
        }.bind()

        val events = eventSourcingRepository.getEvents(aggregateId).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
        }.bind()

        // Reconstruct aggregate from events using fromEvents method
        val scopeEvents = events.filterIsInstance<io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeEvent>()
        val baseAggregate = io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate.fromEvents(scopeEvents).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
        }.bind()

        baseAggregate ?: run {
            logger.warn("Scope not found", mapOf("scopeId" to scopeIdString))
            raise(
                applicationErrorMapper.mapDomainError(
                    io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError.NotFound(scopeId),
                    ErrorMappingContext(attemptedValue = scopeIdString),
                ),
            )
        }
    }

    private data class HandlerResult(
        val aggregate: io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate,
        val events: List<PendingEventEnvelope>,
    )

    /**
     * Converts domain event envelopes to pending event envelopes for persistence.
     */
    private fun toPendingEventEnvelopes(
        events: List<io.github.kamiazya.scopes.platform.domain.event.EventEnvelope.Pending<io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeEvent>>
    ): List<PendingEventEnvelope> = events.map { envelope ->
        PendingEventEnvelope(envelope.event as io.github.kamiazya.scopes.platform.domain.event.DomainEvent)
    }

    private suspend fun applyUpdates(
        initialAggregate: io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate,
        command: UpdateScopeCommand,
    ): Either<ScopeContractError, HandlerResult> = either {
        var currentAggregate = initialAggregate
        val eventsToSave = mutableListOf<PendingEventEnvelope>()

        // Apply title update if provided
        command.title?.let { title ->
            // First validate title uniqueness before applying the update
            validateTitleUniqueness(currentAggregate, title).bind()

            val titleUpdateResult = currentAggregate.handleUpdateTitle(title, Clock.System.now()).mapLeft { error ->
                applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
            }.bind()

            currentAggregate = titleUpdateResult.aggregate
            eventsToSave.addAll(toPendingEventEnvelopes(titleUpdateResult.events))
        }

        // Apply description update if provided
        command.description?.let { description ->
            val descriptionUpdateResult = currentAggregate.handleUpdateDescription(description, Clock.System.now()).mapLeft { error ->
                applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
            }.bind()

            currentAggregate = descriptionUpdateResult.aggregate
            eventsToSave.addAll(toPendingEventEnvelopes(descriptionUpdateResult.events))
        }

        HandlerResult(currentAggregate, eventsToSave)
    }

    private suspend fun persistChangesIfNeeded(
        currentAggregate: io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate,
        eventsToSave: List<PendingEventEnvelope>,
        baseAggregate: io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate,
    ): Either<ScopeContractError, Unit> = either {
        if (eventsToSave.isNotEmpty()) {
            eventSourcingRepository.saveEventsWithVersioning(
                aggregateId = currentAggregate.id,
                events = eventsToSave,
                expectedVersion = baseAggregate.version.value.toInt(),
            ).mapLeft { error ->
                applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
            }.bind()

            // Project events to RDB in the same transaction
            val domainEvents = eventsToSave.map { envelope -> envelope.event }
            eventPublisher.projectEvents(domainEvents).mapLeft { error ->
                logger.error(
                    "Failed to project update events to RDB",
                    mapOf(
                        "error" to error.toString(),
                        "eventCount" to domainEvents.size.toString(),
                    ),
                )
                applicationErrorMapper.mapToContractError(error)
            }.bind()

            logger.info(
                "Scope updated successfully using EventSourcing",
                mapOf(
                    "hasChanges" to "true",
                    "eventsCount" to eventsToSave.size.toString(),
                ),
            )
        }
    }

    private fun buildResult(
        currentAggregate: io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate,
        scopeIdString: String,
    ): UpdateScopeResult {
        // Extract scope data from aggregate for result mapping
        val scope = io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope(
            id = currentAggregate.scopeId!!,
            title = currentAggregate.title!!,
            description = currentAggregate.description,
            parentId = currentAggregate.parentId,
            status = currentAggregate.status,
            aspects = currentAggregate.aspects,
            createdAt = currentAggregate.createdAt,
            updatedAt = currentAggregate.updatedAt,
        )

        // Extract canonical alias from aggregate - required by operational policy
        val canonicalAlias = currentAggregate.canonicalAliasId?.let { id ->
            currentAggregate.aliases[id]?.aliasName?.value
        } ?: error(
            "Missing canonical alias for scope ${currentAggregate.scopeId?.value ?: scopeIdString}. " +
                "This indicates a data inconsistency between aggregate and projections.",
        )

        val result = ScopeMapper.toUpdateScopeResult(scope, canonicalAlias)

        logger.info(
            "Scope update workflow completed",
            mapOf(
                "scopeId" to scope.id.value,
                "title" to scope.title.value,
            ),
        )

        return result
    }

    private fun logCommandFailure(error: ScopeContractError) {
        logger.error(
            "Failed to update scope using EventSourcing",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: error::class.toString()),
                "message" to error.toString(),
            ),
        )
    }

    private suspend fun validateTitleUniqueness(
        aggregate: io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate,
        newTitle: String,
    ): Either<ScopeContractError, Unit> = either {
        // Don't check if the title hasn't changed
        if (aggregate.title?.value == newTitle) {
            return@either
        }

        // Parse and validate the new title
        val validatedTitle = ScopeTitle.create(newTitle)
            .mapLeft { titleError ->
                applicationErrorMapper.mapDomainError(
                    titleError,
                    ErrorMappingContext(attemptedValue = newTitle),
                )
            }.bind()

        // Check if another scope with the same title exists in the same parent context
        val existingScopeId = scopeRepository.findIdByParentIdAndTitle(
            aggregate.parentId,
            validatedTitle.value,
        ).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
        }.bind()

        // Ensure no other scope has this title (or it's our own scope)
        ensure(existingScopeId == null || existingScopeId == aggregate.scopeId) {
            applicationErrorMapper.mapToContractError(
                io.github.kamiazya.scopes.scopemanagement.application.error.ScopeUniquenessError.DuplicateTitle(
                    title = validatedTitle.value,
                    parentScopeId = aggregate.parentId?.value,
                    existingScopeId = existingScopeId!!.value,
                ),
            )
        }
    }
}
