package io.github.kamiazya.scopes.apps.cli.bootstrap

import arrow.core.Either
import io.github.kamiazya.scopes.eventstore.domain.model.EventTypeMapping
import io.github.kamiazya.scopes.eventstore.infrastructure.mapping.DefaultEventTypeMapping
import io.github.kamiazya.scopes.platform.application.lifecycle.ApplicationBootstrapper
import io.github.kamiazya.scopes.platform.application.lifecycle.BootstrapError
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeArchived
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectAdded
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectRemoved
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectsCleared
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectsUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeCreated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeDeleted
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeDescriptionUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeParentChanged
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeRestored
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeTitleUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.event.AliasAssigned
import io.github.kamiazya.scopes.scopemanagement.domain.event.AliasRemoved
import io.github.kamiazya.scopes.scopemanagement.domain.event.AliasNameChanged
import io.github.kamiazya.scopes.scopemanagement.domain.event.CanonicalAliasReplaced

/**
 * Bootstrapper responsible for registering all domain event types.
 *
 * This ensures that:
 * 1. All events can be properly serialized/deserialized
 * 2. Legacy events (persisted with class names) can still be read
 * 3. New events are persisted with stable type IDs
 */
class EventTypeRegistrar(private val eventTypeMapping: EventTypeMapping, private val logger: Logger) : ApplicationBootstrapper {

    override val name: String = "EventTypeRegistrar"
    override val priority: Int = 200 // Run early, before other bootstrappers that might use events

    override suspend fun initialize(): Either<BootstrapError, Unit> = try {
        logger.info("Registering event types...")

        // Register scope management events
        // These use @EventTypeId annotations if present, otherwise fall back to class names
        registerScopeManagementEvents()

        // Register legacy mappings for backward compatibility
        registerLegacyMappings()

        logger.info("Event type registration completed")
        Either.Right(Unit)
    } catch (e: Exception) {
        Either.Left(
            BootstrapError(
                component = name,
                message = "Failed to register event types: ${e.message}",
                cause = e,
                isCritical = true, // Event registration is critical for the system
            ),
        )
    }

    private fun registerScopeManagementEvents() {
        val events = listOf(
            // Scope events
            ScopeCreated::class,
            ScopeDeleted::class,
            ScopeArchived::class,
            ScopeRestored::class,
            ScopeTitleUpdated::class,
            ScopeDescriptionUpdated::class,
            ScopeParentChanged::class,
            ScopeAspectAdded::class,
            ScopeAspectRemoved::class,
            ScopeAspectsCleared::class,
            ScopeAspectsUpdated::class,
            // Alias events
            AliasAssigned::class,
            AliasRemoved::class,
            AliasNameChanged::class,
            CanonicalAliasReplaced::class,
        )

        events.forEach { eventClass ->
            // This will use @EventTypeId if present, otherwise generate from class
            val typeId = eventTypeMapping.getTypeId(eventClass)
            logger.debug("Registered event type: ${eventClass.simpleName} -> $typeId")
        }
    }

    private fun registerLegacyMappings() {
        // For backward compatibility, register class name mappings
        // This allows reading events that were persisted before stable IDs were introduced
        if (eventTypeMapping is DefaultEventTypeMapping) {
            val legacyEvents = listOf(
                // Scope events
                ScopeCreated::class,
                ScopeDeleted::class,
                ScopeArchived::class,
                ScopeRestored::class,
                ScopeTitleUpdated::class,
                ScopeDescriptionUpdated::class,
                ScopeParentChanged::class,
                ScopeAspectAdded::class,
                ScopeAspectRemoved::class,
                ScopeAspectsCleared::class,
                ScopeAspectsUpdated::class,
                // Alias events
                AliasAssigned::class,
                AliasRemoved::class,
                AliasNameChanged::class,
                CanonicalAliasReplaced::class,
            )

            legacyEvents.forEach { eventClass ->
                eventTypeMapping.registerLegacyMapping(eventClass)
            }

            logger.info("Registered ${legacyEvents.size} legacy event mappings for backward compatibility")
        }
    }
}
