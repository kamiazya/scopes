package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Value object representing size change between two states.
 */
data class SizeChange(val fromSize: Long, val toSize: Long) {
    val difference: Long = toSize - fromSize
    val percentageChange: Double = if (fromSize > 0) {
        (difference.toDouble() / fromSize) * 100
    } else {
        if (toSize > 0) 100.0 else 0.0
    }

    fun isIncrease(): Boolean = difference > 0
    fun isDecrease(): Boolean = difference < 0
    fun isUnchanged(): Boolean = difference == 0L
}
