package io.github.kamiazya.scopes.scopemanagement.application.dto.aspect
/**
 * Request for validating a single aspect value.
 */
data class ValidateAspectValueRequest(val key: String, val value: String)

/**
 * Request for validating multiple aspect values.
 */
data class ValidateAspectValuesRequest(val values: Map<String, List<String>>)
