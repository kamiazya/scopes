package com.kamiazya.scopes.application.service

import com.kamiazya.scopes.domain.entity.Scope
import com.kamiazya.scopes.domain.entity.ScopeId
import com.kamiazya.scopes.domain.repository.ScopeRepository
import com.kamiazya.scopes.application.usecase.CreateScopeRequest
import com.kamiazya.scopes.application.usecase.CreateScopeResponse
import com.kamiazya.scopes.application.usecase.CreateScopeUseCase

/**
 * Application service that orchestrates domain use cases.
 * Acts as a facade for the presentation layer.
 */
class ScopeService(
    private val scopeRepository: ScopeRepository,
    private val createScopeUseCase: CreateScopeUseCase,
) {
    suspend fun createScope(request: CreateScopeRequest): CreateScopeResponse = createScopeUseCase.execute(request)

    suspend fun getScope(id: ScopeId): Scope? = scopeRepository.findById(id)

    suspend fun getAllScopes(): List<Scope> = scopeRepository.findAll()

    suspend fun getChildScopes(parentId: ScopeId): List<Scope> = scopeRepository.findByParentId(parentId)

    suspend fun updateScope(scope: Scope): Scope? = scopeRepository.update(scope)

    suspend fun deleteScope(id: ScopeId): Boolean = scopeRepository.delete(id)
}
