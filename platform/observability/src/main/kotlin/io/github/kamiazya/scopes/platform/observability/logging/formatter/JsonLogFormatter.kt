package io.github.kamiazya.scopes.platform.observability.logging.formatter

import io.github.kamiazya.scopes.platform.observability.logging.LogEntry
import io.github.kamiazya.scopes.platform.observability.logging.LogFormatter
import io.github.kamiazya.scopes.platform.observability.logging.LogValue
import kotlinx.serialization.json.*

/**
 * Formats log entries as JSON for structured logging.
 * Compatible with common log aggregation systems and JSON Lines format.
 */
class JsonLogFormatter : LogFormatter {

    private val json = Json {
        // Compact output without pretty printing for JSON Lines compatibility
        prettyPrint = false
    }

    override fun format(entry: LogEntry): String {
        val jsonObject = buildJsonObject {
            put("timestamp", entry.timestamp.toString())
            put("level", entry.level.name)
            put("logger", entry.loggerName)
            put("message", entry.message)

            // Add all context as separate fields
            val allContext = entry.getAllContext()
            if (allContext.isNotEmpty()) {
                putJsonObject("context") {
                    allContext.forEach { (key, logValue) ->
                        put(key, logValueToJsonElement(logValue))
                    }
                }
            }

            // Add error information if present
            entry.throwable?.let { throwable ->
                putJsonObject("error") {
                    put("type", throwable::class.simpleName ?: "Unknown")
                    put("message", throwable.message ?: "")
                    putJsonArray("stackTrace") {
                        throwable.stackTrace.forEach { element ->
                            add(element.toString())
                        }
                    }
                }
            }
        }

        // Use Json instance with compact formatting
        return json.encodeToString(JsonObject.serializer(), jsonObject)
    }

    private fun logValueToJsonElement(logValue: LogValue): JsonElement = when (logValue) {
        is LogValue.StringValue -> JsonPrimitive(logValue.value)
        is LogValue.NumberValue -> JsonPrimitive(logValue.value)
        is LogValue.BooleanValue -> JsonPrimitive(logValue.value)
        is LogValue.ListValue -> buildJsonArray {
            logValue.value.forEach { item ->
                add(logValueToJsonElement(item))
            }
        }
        is LogValue.MapValue -> buildJsonObject {
            logValue.value.forEach { (k, v) ->
                put(k, logValueToJsonElement(v))
            }
        }
        LogValue.NullValue -> JsonNull
    }
}
