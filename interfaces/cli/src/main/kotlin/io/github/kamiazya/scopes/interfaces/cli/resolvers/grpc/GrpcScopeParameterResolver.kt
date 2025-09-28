package io.github.kamiazya.scopes.interfaces.cli.resolvers.grpc

import arrow.core.Either
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.interfaces.cli.transport.Transport

/**
 * gRPC-specific implementation of ScopeParameterResolver.
 * 
 * This resolver uses the Transport layer to resolve scope aliases
 * to scope IDs when using gRPC transport.
 */
class GrpcScopeParameterResolver(
    private val transport: Transport
) {
    
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
                transport.resolveAlias(parameter).map { result ->
                    result.scopeId
                }
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
}