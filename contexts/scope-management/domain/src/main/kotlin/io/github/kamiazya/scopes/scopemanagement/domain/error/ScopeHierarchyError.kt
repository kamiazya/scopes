package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Constraint violations related to Scope hierarchy.
 */
sealed class ScopeHierarchyError : ScopesError() {

    data class CircularReference(override val occurredAt: Instant, val scopeId: ScopeId, val parentId: ScopeId) : ScopeHierarchyError()

    data class CircularPath(override val occurredAt: Instant, val scopeId: ScopeId, val cyclePath: List<ScopeId>) : ScopeHierarchyError()

    data class MaxDepthExceeded(override val occurredAt: Instant, val scopeId: ScopeId, val attemptedDepth: Int, val maximumDepth: Int) :
        ScopeHierarchyError()

    data class MaxChildrenExceeded(override val occurredAt: Instant, val parentScopeId: ScopeId, val currentChildrenCount: Int, val maximumChildren: Int) :
        ScopeHierarchyError()

    data class SelfParenting(override val occurredAt: Instant, val scopeId: ScopeId) : ScopeHierarchyError()

    data class ParentNotFound(override val occurredAt: Instant, val scopeId: ScopeId, val parentId: ScopeId) : ScopeHierarchyError()

    data class InvalidParentId(override val occurredAt: Instant, val invalidId: String) : ScopeHierarchyError()

    data class ScopeInHierarchyNotFound(override val occurredAt: Instant, val scopeId: ScopeId) : ScopeHierarchyError()

    data class HasChildren(val scopeId: ScopeId, override val occurredAt: Instant = Clock.System.now()) : ScopeHierarchyError()
}
