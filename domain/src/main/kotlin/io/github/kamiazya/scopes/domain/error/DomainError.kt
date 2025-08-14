package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import arrow.core.NonEmptyList

/**
 * Domain-level errors following functional DDD principles.
 * All business rule violations and domain validation errors.
 */
sealed class DomainError
