package io.github.kamiazya.scopes.interfaces.mcp.support

import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.platform.observability.logging.Slf4jLogger

/**
 * Factory function to create an ErrorMapper instance.
 * This allows external modules to create instances without accessing internal implementations.
 */
fun createErrorMapper(logger: Logger = Slf4jLogger("ErrorMapper")): ErrorMapper = DefaultErrorMapper(logger)

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
