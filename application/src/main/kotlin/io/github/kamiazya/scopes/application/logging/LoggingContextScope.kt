package io.github.kamiazya.scopes.application.logging

import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Interface for executing code within a logging context scope.
 * Implementations handle the context propagation mechanism (coroutines, ThreadLocal, etc.).
 */
interface LoggingContextScope {
    /**
     * Executes a block of code with the given logging context.
     */
    suspend fun <T> withContext(context: LoggingContext, block: suspend () -> T): T

    /**
     * Adds additional metadata to the current logging context.
     */
    suspend fun <T> withAdditionalMetadata(metadata: Map<String, Any>, block: suspend () -> T): T

    /**
     * Gets the current logging context from the coroutine context.
     */
    suspend fun getCurrentContext(): LoggingContext?
}

/**
 * Default implementation that uses coroutine context for propagation.
 * This can be used when no specific implementation is provided.
 */
object DefaultLoggingContextScope : LoggingContextScope {
    override suspend fun <T> withContext(context: LoggingContext, block: suspend () -> T): T {
        return withContext(coroutineContext + LoggingCoroutineContext(context)) {
            block()
        }
    }

    override suspend fun <T> withAdditionalMetadata(metadata: Map<String, Any>, block: suspend () -> T): T {
        val currentContext = getCurrentContext() ?: LoggingContext()
        val newContext = currentContext.withAdditionalMetadata(metadata)
        return withContext(newContext, block)
    }

    override suspend fun getCurrentContext(): LoggingContext? {
        return coroutineContext[LoggingCoroutineContext]?.loggingContext
    }
}

