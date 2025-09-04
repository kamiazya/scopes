package io.github.kamiazya.scopes.interfaces.cli.resolvers

import arrow.core.Either
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery as ContractGetScopeByAliasQuery

/**
 * Resolves scope parameters that can be either a ULID or an alias.
 *
 * This utility helps CLI commands accept both formats transparently:
 * - ULIDs: Direct scope identifiers (e.g., "01HZQB5QKM0WDG7ZBHSPKT3N2Y")
 * - Aliases: Human-readable names (e.g., "project-name", "epic-feature")
 */
class ScopeParameterResolver(private val scopeManagementPort: ScopeManagementQueryPort) {
    /**
     * Resolves a parameter to a scope ID.
     *
     * @param parameter The parameter to resolve (ULID or alias)
     * @return Either an error or the resolved scope ID
     */
    suspend fun resolve(parameter: String): Either<ScopeContractError, String> {
        // First, check if it's a valid ULID
        return when {
            isValidUlid(parameter) -> {
                // It's a ULID, return as-is
                parameter.right()
            }
            else -> {
                // Try to resolve as alias
                resolveAlias(parameter)
            }
        }
    }

    /**
     * Checks if a string is a valid ULID format.
     * ULIDs are 26 characters long and contain only valid ULID characters.
     */
    private fun isValidUlid(value: String): Boolean {
        if (value.length != 26) return false

        // ULID uses Crockford's base32: 0123456789ABCDEFGHJKMNPQRSTVWXYZ
        val validChars = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        return value.all { it.uppercaseChar() in validChars }
    }

    /**
     * Resolves an alias to a scope ID.
     */
    private suspend fun resolveAlias(alias: String): Either<ScopeContractError, String> {
        val query = ContractGetScopeByAliasQuery(aliasName = alias)
        return scopeManagementPort.getScopeByAlias(query).map { scopeResult ->
            scopeResult.id
        }
    }
}
