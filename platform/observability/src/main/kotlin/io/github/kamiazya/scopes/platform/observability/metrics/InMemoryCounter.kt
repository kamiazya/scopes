package io.github.kamiazya.scopes.platform.observability.metrics

/**
 * Thread-safe in-memory implementation of Counter.
 * Uses synchronized blocks for thread-safe operations.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class InMemoryCounter(private val name: String, private val description: String? = null, private val tags: Map<String, String> = emptyMap()) : Counter {

    private var count: Long = 0
    private val lock = Object()

    override fun increment() {
        synchronized(lock) {
            count += 1
        }
    }

    override fun increment(amount: Double) {
        require(amount >= 0) { "Counter increment amount must be non-negative, got $amount" }
        // Convert double to long for atomic operations
        val longAmount = amount.toLong()
        synchronized(lock) {
            count += longAmount
        }
    }

    override fun count(): Double = synchronized(lock) {
        count.toDouble()
    }

    override fun reset() {
        synchronized(lock) {
            count = 0
        }
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
