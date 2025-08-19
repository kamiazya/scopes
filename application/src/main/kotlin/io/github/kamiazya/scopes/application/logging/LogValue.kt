package io.github.kamiazya.scopes.application.logging

/**
 * Represents a serializable value that can be included in log context.
 * This ensures type safety and serializability for all log context values.
 */
sealed class LogValue {
    data class StringValue(val value: String) : LogValue()
    data class NumberValue(val value: Number) : LogValue()
    data class BooleanValue(val value: Boolean) : LogValue()
    data class ListValue(val value: List<LogValue>) : LogValue()
    data class MapValue(val value: Map<String, LogValue>) : LogValue()
    object NullValue : LogValue()
}

/**
 * Converts any value to a LogValue if possible.
 * Returns null if the value cannot be converted to a serializable form.
 */
fun Any?.toLogValue(): LogValue? {
    return when (this) {
        null -> LogValue.NullValue
        is String -> LogValue.StringValue(this)
        is Number -> LogValue.NumberValue(this)
        is Boolean -> LogValue.BooleanValue(this)
        is List<*> -> {
            val convertedList = this.mapNotNull { it.toLogValue() }
            if (convertedList.size == this.size) {
                LogValue.ListValue(convertedList)
            } else {
                null // Some elements couldn't be converted
            }
        }
        is Map<*, *> -> {
            val convertedMap = mutableMapOf<String, LogValue>()
            for ((key, value) in this) {
                if (key is String) {
                    val logValue = value.toLogValue()
                    if (logValue != null) {
                        convertedMap[key] = logValue
                    } else {
                        return null // Value couldn't be converted
                    }
                } else {
                    return null // Non-string key
                }
            }
            LogValue.MapValue(convertedMap)
        }
        else -> LogValue.StringValue(this.toString()) // Fallback to string representation
    }
}

/**
 * Extension function to convert a Map<String, Any> to Map<String, LogValue>.
 */
fun Map<String, Any>.toLogValueMap(): Map<String, LogValue> {
    return this.mapNotNull { (key, value) ->
        value.toLogValue()?.let { key to it }
    }.toMap()
}

/**
 * Extension function to convert LogValue back to Any for compatibility.
 */
fun LogValue.toAny(): Any? {
    return when (this) {
        is LogValue.StringValue -> value
        is LogValue.NumberValue -> value
        is LogValue.BooleanValue -> value
        is LogValue.ListValue -> value.map { it.toAny() }
        is LogValue.MapValue -> value.mapValues { it.value.toAny() }
        LogValue.NullValue -> null
    }
}

/**
 * Extension function to convert Map<String, LogValue> back to Map<String, Any>.
 */
fun Map<String, LogValue>.toAnyMap(): Map<String, Any> {
    return this.mapNotNull { (key, value) ->
        val anyValue = value.toAny()
        if (anyValue != null) key to anyValue else null
    }.toMap()
}

