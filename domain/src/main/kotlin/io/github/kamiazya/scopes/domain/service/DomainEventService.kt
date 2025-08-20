package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.domain.event.AliasAssigned
import io.github.kamiazya.scopes.domain.event.AliasRemoved
import io.github.kamiazya.scopes.domain.event.CanonicalAliasReplaced
import io.github.kamiazya.scopes.domain.event.ContextViewChanges
import io.github.kamiazya.scopes.domain.event.ContextViewCreated
import io.github.kamiazya.scopes.domain.event.ContextViewDeleted
import io.github.kamiazya.scopes.domain.event.ContextViewEvent
import io.github.kamiazya.scopes.domain.event.ScopeChanges
import io.github.kamiazya.scopes.domain.event.ScopeCreated
import io.github.kamiazya.scopes.domain.event.ScopeDeleted
import io.github.kamiazya.scopes.domain.event.ScopeEvent
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Service responsible for recording domain events.
 *
 * This service provides a high-level interface for recording business events
 * that occur within the domain. It serves as the foundation for event sourcing
 * and provides an audit trail of all significant domain actions.
 *
 * The service abstracts the complexity of event creation and ensures that all
 * necessary information is captured consistently across different event types.
 *
 * Implementation notes:
 * - The actual persistence of events is handled by infrastructure layer
 * - Event IDs should be globally unique (e.g., ULIDs)
 * - Version numbers are managed by the aggregate or repository
 */
interface DomainEventService {

    // Scope-related events

    /**
     * Records that a new Scope has been created.
     *
     * @param scope The newly created Scope entity
     * @return The created ScopeCreated event
     */
    suspend fun recordScopeCreated(scope: Scope): ScopeCreated

    /**
     * Records that a Scope has been updated with various changes.
     *
     * @param scopeId The ID of the updated Scope
     * @param changes The changes applied to the Scope
     * @param version The version number after the update
     * @return List of specific events for each type of change
     */
    suspend fun recordScopeUpdated(scopeId: ScopeId, changes: ScopeChanges, version: Int): List<ScopeEvent>

    /**
     * Records that a Scope has been deleted.
     *
     * @param scopeId The ID of the deleted Scope
     * @param version The final version number
     * @return The created ScopeDeleted event
     */
    suspend fun recordScopeDeleted(scopeId: ScopeId, version: Int): ScopeDeleted

    // Alias-related events

    /**
     * Records that an alias has been assigned to a Scope.
     *
     * @param alias The assigned ScopeAlias entity
     * @return The created AliasAssigned event
     */
    suspend fun recordAliasAssigned(alias: ScopeAlias): AliasAssigned

    /**
     * Records that an alias has been removed from a Scope.
     *
     * @param alias The removed ScopeAlias entity
     * @param version The version number at removal
     * @return The created AliasRemoved event
     */
    suspend fun recordAliasRemoved(alias: ScopeAlias, version: Int): AliasRemoved

    /**
     * Records that a canonical alias has been replaced.
     *
     * @param scopeId The Scope whose canonical alias was replaced
     * @param oldAlias The previous canonical alias
     * @param newAlias The new canonical alias
     * @param version The version number after replacement
     * @return The created CanonicalAliasReplaced event
     */
    suspend fun recordCanonicalAliasReplaced(
        scopeId: ScopeId,
        oldAlias: ScopeAlias,
        newAlias: ScopeAlias,
        version: Int,
    ): CanonicalAliasReplaced

    // ContextView-related events

    /**
     * Records that a new ContextView has been created.
     *
     * @param contextView The newly created ContextView entity
     * @return The created ContextViewCreated event
     */
    suspend fun recordContextViewCreated(contextView: ContextView): ContextViewCreated

    /**
     * Records that a ContextView has been updated.
     *
     * @param contextViewId The ID of the updated ContextView
     * @param changes The changes applied to the ContextView
     * @param version The version number after the update
     * @return List of specific events for each type of change
     */
    suspend fun recordContextViewUpdated(
        contextViewId: ContextViewId,
        changes: ContextViewChanges,
        version: Int,
    ): List<ContextViewEvent>

    /**
     * Records that a ContextView has been deleted.
     *
     * @param contextViewId The ID of the deleted ContextView
     * @param version The final version number
     * @return The created ContextViewDeleted event
     */
    suspend fun recordContextViewDeleted(contextViewId: ContextViewId, version: Int): ContextViewDeleted
}
