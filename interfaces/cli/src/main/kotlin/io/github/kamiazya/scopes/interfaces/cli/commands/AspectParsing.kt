package io.github.kamiazya.scopes.interfaces.cli.commands

/**
 * Parse a single aspect entry string into key/value pair.
 * Supports both `key:value` and `key=value` formats.
 */
fun parseAspectEntry(entry: String): Pair<String, String>? {
    if (entry.isBlank()) return null

    val idxColon = entry.indexOf(':')
    val idxEq = entry.indexOf('=')

    val idx = when {
        idxColon >= 0 && idxEq >= 0 -> minOf(idxColon, idxEq)
        idxColon >= 0 -> idxColon
        idxEq >= 0 -> idxEq
        else -> -1
    }
    if (idx < 0) return null

    val key = entry.substring(0, idx).trim()
    val value = entry.substring(idx + 1).trim()
    if (key.isEmpty() || value.isEmpty()) return null
    return key to value
}

/**
 * Parse multiple aspect filter strings into a grouped map keyed by aspect key.
 * Each input can be formatted as `key:value` or `key=value`.
 * Invalid entries are ignored.
 */
fun parseAspectFilters(entries: List<String>): Map<String, List<String>> =
    entries.mapNotNull { parseAspectEntry(it) }
        .groupBy({ it.first }, { it.second })

