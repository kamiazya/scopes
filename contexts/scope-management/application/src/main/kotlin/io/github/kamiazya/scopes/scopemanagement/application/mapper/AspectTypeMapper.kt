package io.github.kamiazya.scopes.scopemanagement.application.mapper

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType

/**
 * Extension function to convert AspectType to the lowercase string representation
 * expected by the contract layer.
 */
fun AspectType.toTypeString(): String = when (this) {
    is AspectType.Text -> "text"
    is AspectType.Numeric -> "numeric"
    is AspectType.BooleanType -> "boolean"
    is AspectType.Duration -> "duration"
    is AspectType.Ordered -> "ordered"
}
