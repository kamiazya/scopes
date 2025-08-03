package com.kamiazya.scopes.application.usecase

import com.kamiazya.scopes.domain.entity.Scope
import com.kamiazya.scopes.domain.entity.ScopeId
import com.kamiazya.scopes.domain.repository.ScopeRepository

/**
 * Use case for creating new Scope entities.
 * Encapsulates business logic for scope creation.
 */
class CreateScopeUseCase(
    private val scopeRepository: ScopeRepository,
) {
    suspend fun execute(request: CreateScopeRequest): CreateScopeResponse {
        val scope =
            Scope(
                id = request.id ?: ScopeId.generate(),
                title = request.title,
                description = request.description,
                parentId = request.parentId,
                metadata = request.metadata,
            )

        // Validate parent exists if specified
        val parentId = scope.parentId
        if (parentId != null) {
            scopeRepository.findById(parentId)
                ?: throw IllegalArgumentException("Parent scope not found: $parentId")
        }

        val createdScope = scopeRepository.save(scope)
        return CreateScopeResponse(createdScope)
    }
}

data class CreateScopeRequest(
    val id: ScopeId? = null,
    val title: String,
    val description: String? = null,
    val parentId: ScopeId? = null,
    val metadata: Map<String, String> = emptyMap(),
)

data class CreateScopeResponse(
    val scope: Scope,
)
