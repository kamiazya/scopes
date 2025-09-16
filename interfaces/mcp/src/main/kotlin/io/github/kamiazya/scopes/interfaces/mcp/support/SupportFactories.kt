package io.github.kamiazya.scopes.interfaces.mcp.support

/**
 * Factory function to create an ErrorMapper instance.
 * This allows external modules to create instances without accessing internal implementations.
 */
fun createErrorMapper(): ErrorMapper = DefaultErrorMapper()

/**
 * Factory function to create an ArgumentCodec instance.
 * This allows external modules to create instances without accessing internal implementations.
 */
fun createArgumentCodec(): ArgumentCodec = DefaultArgumentCodec()

/**
 * Factory function to create an IdempotencyService instance.
 * This allows external modules to create instances without accessing internal implementations.
 */
fun createIdempotencyService(argumentCodec: ArgumentCodec): IdempotencyService = DefaultIdempotencyService(argumentCodec)
