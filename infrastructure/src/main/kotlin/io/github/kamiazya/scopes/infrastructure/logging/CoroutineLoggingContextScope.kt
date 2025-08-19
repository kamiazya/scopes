package io.github.kamiazya.scopes.infrastructure.logging

import io.github.kamiazya.scopes.application.logging.LoggingContext
import io.github.kamiazya.scopes.application.logging.LoggingContextScope
import io.github.kamiazya.scopes.application.logging.LoggingCoroutineContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Implementation of LoggingContextScope using Kotlin coroutines.
 * Propagates logging context through the coroutine context.
 */
class CoroutineLoggingContextScope : LoggingContextScope {

    override suspend fun <T> withContext(context: LoggingContext, block: suspend () -> T): T {
        return withContext(LoggingCoroutineContext(context)) {
            block()
        }
    }

    override suspend fun <T> withAdditionalMetadata(metadata: Map<String, Any>, block: suspend () -> T): T {
        val currentContext = coroutineContext[LoggingCoroutineContext]?.loggingContext
            ?: LoggingContext()

        val newContext = currentContext.withMetadata(metadata)

        return withContext(LoggingCoroutineContext(newContext)) {
            block()
        }
    }

    override suspend fun getCurrentContext(): LoggingContext? {
        return coroutineContext[LoggingCoroutineContext]?.loggingContext
    }
}

