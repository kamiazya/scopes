package io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.scopemanagement.domain.event.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Provides serialization configuration for scope management domain events.
 *
 * This module registers all domain events from the scope-management context
 * for polymorphic serialization. Since domain layer classes cannot have
 * @Serializable annotations (per architecture rules), we handle registration
 * in the infrastructure layer using surrogate serializers.
 */
object ScopeEventSerializersModule {

    /**
     * Creates a SerializersModule configured with all scope management events.
     */
    fun create(): SerializersModule = SerializersModule {
        polymorphic(DomainEvent::class) {
            // Register ScopeEvent hierarchy
            polymorphic(ScopeEvent::class) {
                // Core scope events with surrogate serializers
                subclass(ScopeCreated::class, ScopeEventSerializers.ScopeCreatedSerializer)
                subclass(ScopeDeleted::class, ScopeEventSerializers.ScopeDeletedSerializer)
                subclass(ScopeArchived::class, ScopeEventSerializers.ScopeArchivedSerializer)
                subclass(ScopeRestored::class, ScopeEventSerializers.ScopeRestoredSerializer)
                subclass(ScopeTitleUpdated::class, ScopeEventSerializers.ScopeTitleUpdatedSerializer)
                subclass(ScopeDescriptionUpdated::class, ScopeEventSerializers.ScopeDescriptionUpdatedSerializer)
                subclass(ScopeParentChanged::class, ScopeEventSerializers.ScopeParentChangedSerializer)

                // Aspect-related events
                subclass(ScopeAspectAdded::class, ScopeEventSerializers.ScopeAspectAddedSerializer)
                subclass(ScopeAspectRemoved::class, ScopeEventSerializers.ScopeAspectRemovedSerializer)
                subclass(ScopeAspectsCleared::class, ScopeEventSerializers.ScopeAspectsClearedSerializer)
                subclass(ScopeAspectsUpdated::class, ScopeEventSerializers.ScopeAspectsUpdatedSerializer)

                // Register AliasEvent hierarchy
                polymorphic(AliasEvent::class) {
                    subclass(AliasAssigned::class, ScopeEventSerializers.AliasAssignedSerializer)
                    subclass(AliasRemoved::class, ScopeEventSerializers.AliasRemovedSerializer)
                    subclass(AliasNameChanged::class, ScopeEventSerializers.AliasNameChangedSerializer)
                    subclass(CanonicalAliasReplaced::class, ScopeEventSerializers.CanonicalAliasReplacedSerializer)
                }
            }

            // Register concrete event types at the DomainEvent level as well with their serializers
            subclass(ScopeCreated::class, ScopeEventSerializers.ScopeCreatedSerializer)
            subclass(ScopeDeleted::class, ScopeEventSerializers.ScopeDeletedSerializer)
            subclass(ScopeArchived::class, ScopeEventSerializers.ScopeArchivedSerializer)
            subclass(ScopeRestored::class, ScopeEventSerializers.ScopeRestoredSerializer)
            subclass(ScopeTitleUpdated::class, ScopeEventSerializers.ScopeTitleUpdatedSerializer)
            subclass(ScopeDescriptionUpdated::class, ScopeEventSerializers.ScopeDescriptionUpdatedSerializer)
            subclass(ScopeParentChanged::class, ScopeEventSerializers.ScopeParentChangedSerializer)
            subclass(ScopeAspectAdded::class, ScopeEventSerializers.ScopeAspectAddedSerializer)
            subclass(ScopeAspectRemoved::class, ScopeEventSerializers.ScopeAspectRemovedSerializer)
            subclass(ScopeAspectsCleared::class, ScopeEventSerializers.ScopeAspectsClearedSerializer)
            subclass(ScopeAspectsUpdated::class, ScopeEventSerializers.ScopeAspectsUpdatedSerializer)

            // Alias events
            subclass(AliasAssigned::class, ScopeEventSerializers.AliasAssignedSerializer)
            subclass(AliasRemoved::class, ScopeEventSerializers.AliasRemovedSerializer)
            subclass(AliasNameChanged::class, ScopeEventSerializers.AliasNameChangedSerializer)
            subclass(CanonicalAliasReplaced::class, ScopeEventSerializers.CanonicalAliasReplacedSerializer)
        }
    }
}
