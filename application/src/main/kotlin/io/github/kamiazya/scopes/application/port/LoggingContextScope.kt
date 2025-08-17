package io.github.kamiazya.scopes.application.port

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
}

/**
 * Global scope instance that should be configured by the infrastructure layer.
 */
var loggingContextScope: LoggingContextScope? = null