package io.github.kamiazya.scopes.scopemanagement.application.dto

/**
 * DTO containing a list of aliases for a scope.
 *
 * @property scopeId The ID of the scope
 * @property aliases List of alias information, sorted with canonical first
 * @property totalCount Total number of aliases for the scope
 */
data class AliasListDto(val scopeId: String, val aliases: List<AliasInfoDto>, val totalCount: Int) : DTO
