package com.kamiazya.scopes.application.service

import com.kamiazya.scopes.application.error.ApplicationError
import com.kamiazya.scopes.application.usecase.CreateScopeRequest
import com.kamiazya.scopes.application.usecase.CreateScopeResponse
import com.kamiazya.scopes.application.usecase.CreateScopeUseCase
import arrow.core.Either
import com.kamiazya.scopes.domain.entity.Scope
import com.kamiazya.scopes.domain.entity.ScopeId
import com.kamiazya.scopes.domain.repository.ScopeRepository

/**
 * Application service that orchestrates domain use cases.
 * Acts as a facade for the presentation layer.
 * Follows functional DDD principles with explicit error handling.
 */
class ScopeService(
    private val scopeRepository: ScopeRepository,
    private val createScopeUseCase: CreateScopeUseCase,
) {

    suspend fun createScope(request: CreateScopeRequest): Either<ApplicationError, CreateScopeResponse> =
        createScopeUseCase.execute(request)

    suspend fun getScope(id: ScopeId): Either<ApplicationError, Scope?> =
        scopeRepository.findById(id)
            .mapLeft { ApplicationError.fromRepositoryError(it) }

    suspend fun getAllScopes(): Either<ApplicationError, List<Scope>> =
        scopeRepository.findAll()
            .mapLeft { ApplicationError.fromRepositoryError(it) }

    suspend fun getChildScopes(parentId: ScopeId): Either<ApplicationError, List<Scope>> =
        scopeRepository.findByParentId(parentId)
            .mapLeft { ApplicationError.fromRepositoryError(it) }

    suspend fun getRootScopes(): Either<ApplicationError, List<Scope>> =
        scopeRepository.findRootScopes()
            .mapLeft { ApplicationError.fromRepositoryError(it) }

    suspend fun deleteScope(id: ScopeId): Either<ApplicationError, Unit> =
        scopeRepository.deleteById(id)
            .mapLeft { ApplicationError.fromRepositoryError(it) }

    suspend fun scopeExists(id: ScopeId): Either<ApplicationError, Boolean> =
        scopeRepository.existsById(id)
            .mapLeft { ApplicationError.fromRepositoryError(it) }
}

