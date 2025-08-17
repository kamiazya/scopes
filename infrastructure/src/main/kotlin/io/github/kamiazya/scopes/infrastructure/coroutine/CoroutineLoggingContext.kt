package io.github.kamiazya.scopes.infrastructure.coroutine

import io.github.kamiazya.scopes.application.port.LoggingContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * CoroutineContext element that wraps LoggingContext for propagation through coroutines.
 * This is the infrastructure layer's coroutine-specific adapter.
 */
data class CoroutineLoggingContext(
    val loggingContext: LoggingContext
) : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<CoroutineLoggingContext>

    override val key: CoroutineContext.Key<*> = Key
}

/**
 * Gets the current LoggingContext from the coroutine context.
 */
suspend fun currentLoggingContext(): LoggingContext? {
    return coroutineContext[CoroutineLoggingContext]?.loggingContext
}

/**
 * Executes a block with the given LoggingContext in the coroutine context.
 */
suspend fun <T> withLoggingContext(
    context: LoggingContext,
    block: suspend () -> T
): T {
    return withContext(CoroutineLoggingContext(context)) {
        block()
    }
}

/**
 * Adds additional metadata to the current logging context.
 */
suspend fun <T> withAdditionalLoggingContext(
    metadata: Map<String, Any>,
    block: suspend () -> T
): T {
    val current = currentLoggingContext() ?: LoggingContext()
    return withLoggingContext(current.withMetadata(metadata), block)
}

/**
 * Coroutine-based implementation of LoggingContextScope.
 */
class CoroutineLoggingContextScope : io.github.kamiazya.scopes.application.port.LoggingContextScope {
    override suspend fun <T> withContext(context: LoggingContext, block: suspend () -> T): T {
        return withLoggingContext(context, block)
    }

    override suspend fun <T> withAdditionalMetadata(metadata: Map<String, Any>, block: suspend () -> T): T {
        return withAdditionalLoggingContext(metadata, block)
    }
}
