package io.github.kamiazya.scopes.platform.observability.metrics

import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe in-memory implementation of Counter.
 * Uses AtomicLong for thread-safe operations.
 */
class InMemoryCounter(private val name: String, private val description: String? = null, private val tags: Map<String, String> = emptyMap()) : Counter {

    private val atomicCount = AtomicLong(0)

    override fun increment() {
        atomicCount.incrementAndGet()
    }

    override fun increment(amount: Double) {
        require(amount >= 0) { "Counter increment amount must be non-negative, got $amount" }
        // Convert double to long for atomic operations
        val longAmount = amount.toLong()
        atomicCount.addAndGet(longAmount)
    }

    override fun count(): Double = atomicCount.get().toDouble()

    override fun reset() {
        atomicCount.set(0)
    }

    override fun toString(): String {
        val tagString = if (tags.isNotEmpty()) {
            "{${tags.entries.joinToString(", ") { "${it.key}=\"${it.value}\"" }}}"
        } else {
            ""
        }
        return "$name$tagString: ${count()}"
    }
}
