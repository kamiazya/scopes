package io.github.kamiazya.scopes.domain.error

/**
 * Infrastructure errors wrapped as domain errors.
 */
data class DomainInfrastructureError(val repositoryError: RepositoryError) : DomainError()