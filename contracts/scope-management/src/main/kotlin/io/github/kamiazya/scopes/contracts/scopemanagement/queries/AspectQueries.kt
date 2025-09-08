package io.github.kamiazya.scopes.contracts.scopemanagement.queries

/**
 * Query to get an aspect definition.
 */
public data class GetAspectDefinitionQuery(val key: String)

/**
 * Query to list aspect definitions.
 */
public object ListAspectDefinitionsQuery

/**
 * Query to validate aspect value.
 */
public data class ValidateAspectValueQuery(val key: String, val values: List<String>)
