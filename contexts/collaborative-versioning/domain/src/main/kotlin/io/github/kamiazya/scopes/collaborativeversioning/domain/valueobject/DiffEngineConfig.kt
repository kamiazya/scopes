package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Configuration for the diff engine.
 */
data class DiffEngineConfig(
    val maxDocumentSize: Long = 10 * 1024 * 1024, // 10MB
    val detectArrayMoves: Boolean = true,
    val optimizeChanges: Boolean = true,
    val maxDiffDepth: Int = 100,
) {
    companion object {
        fun default() = DiffEngineConfig()

        fun performance() = DiffEngineConfig(
            detectArrayMoves = false,
            optimizeChanges = false,
        )

        fun strict() = DiffEngineConfig(
            detectArrayMoves = true,
            optimizeChanges = true,
            maxDocumentSize = Long.MAX_VALUE,
        )
    }
}
