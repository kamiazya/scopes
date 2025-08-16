package io.github.kamiazya.scopes.application.dto

/**
 * Result DTO for listing aliases.
 * 
 * Contains a list of aliases matching the query criteria.
 */
data class ListAliasesResult(
    val aliases: List<ScopeAliasDTO>
) : DTO