package io.github.kamiazya.scopes.interfaces.mcp.support

import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Lightweight DSL helpers for building JSON Schemas for MCP Tool I/O.
 * Keeps schema definitions concise and consistent across handlers.
 */
object SchemaDsl {

    fun toolInput(required: List<String> = emptyList(), properties: JsonObjectBuilder.() -> Unit): Tool.Input =
        Tool.Input(properties = schemaObject(required, properties))

    fun toolOutput(required: List<String> = emptyList(), properties: JsonObjectBuilder.() -> Unit): Tool.Output =
        Tool.Output(properties = schemaObject(required, properties))

    fun schemaObject(required: List<String> = emptyList(), properties: JsonObjectBuilder.() -> Unit): JsonObject = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        // Always include required array to keep generated shape uniform
        required(*required.toTypedArray())
        putJsonObject("properties") { properties() }
    }
}

/**
 * Property builder helpers.
 */
fun JsonObjectBuilder.stringProperty(name: String, minLength: Int? = null, pattern: String? = null, description: String? = null) {
    putJsonObject(name) {
        put("type", "string")
        minLength?.let { put("minLength", it) }
        pattern?.let { put("pattern", it) }
        description?.let { put("description", it) }
    }
}

fun JsonObjectBuilder.booleanProperty(name: String, description: String? = null) {
    putJsonObject(name) {
        put("type", "boolean")
        description?.let { put("description", it) }
    }
}

fun JsonObjectBuilder.numberProperty(name: String, description: String? = null) {
    putJsonObject(name) {
        put("type", "number")
        description?.let { put("description", it) }
    }
}

fun JsonObjectBuilder.integerProperty(name: String, description: String? = null) {
    putJsonObject(name) {
        put("type", "integer")
        description?.let { put("description", it) }
    }
}

fun JsonObjectBuilder.enumProperty(name: String, values: List<String>, description: String? = null) {
    putJsonObject(name) {
        put("type", "string")
        put("enum", buildJsonArray { values.forEach { add(it) } })
        description?.let { put("description", it) }
    }
}

fun JsonObjectBuilder.arrayOfStringsProperty(name: String, description: String? = null) {
    putJsonObject(name) {
        put("type", "array")
        putJsonObject("items") { put("type", "string") }
        description?.let { put("description", it) }
    }
}

/**
 * Common field shortcuts.
 */
fun JsonObjectBuilder.aliasProperty(name: String = "alias", description: String = "Scope alias") =
    stringProperty(name, minLength = 1, description = description)

fun JsonObjectBuilder.scopeAliasProperty(name: String = "scopeAlias", description: String = "Existing scope alias") =
    stringProperty(name, minLength = 1, description = description)

fun JsonObjectBuilder.newAliasProperty(name: String = "newAlias", description: String = "New alias to add") =
    stringProperty(name, minLength = 1, description = description)

fun JsonObjectBuilder.idempotencyKeyProperty(name: String = "idempotencyKey", description: String = "Idempotency key to prevent duplicate operations") =
    stringProperty(name, pattern = IdempotencyService.IDEMPOTENCY_KEY_PATTERN_STRING, description = description)

/**
 * Define an object-typed property with nested properties and required list.
 */
fun JsonObjectBuilder.objectProperty(name: String, required: List<String> = emptyList(), properties: JsonObjectBuilder.() -> Unit) {
    putJsonObject(name) {
        put("type", "object")
        put("additionalProperties", false)
        required(*required.toTypedArray())
        putJsonObject("properties") { properties() }
    }
}

/**
 * Define an array-typed property whose items are objects.
 */
fun JsonObjectBuilder.arrayOfObjectsProperty(name: String, itemRequired: List<String> = emptyList(), itemProperties: JsonObjectBuilder.() -> Unit) {
    putJsonObject(name) {
        put("type", "array")
        putJsonObject("items") {
            put("type", "object")
            put("additionalProperties", false)
            required(*itemRequired.toTypedArray())
            putJsonObject("properties") { itemProperties() }
        }
    }
}

/** Set the required array on the current object builder. */
fun JsonObjectBuilder.required(vararg names: String) {
    put("required", buildJsonArray { names.forEach { add(it) } })
}

/** Define an array-typed property with a custom items definition. */
fun JsonObjectBuilder.arrayProperty(name: String, items: JsonObjectBuilder.() -> Unit) {
    putJsonObject(name) {
        put("type", "array")
        putJsonObject("items") { items() }
    }
}

/** Within an array's items, define an object with required + properties. */
fun JsonObjectBuilder.itemsObject(required: List<String> = emptyList(), properties: JsonObjectBuilder.() -> Unit) {
    put("type", "object")
    put("additionalProperties", false)
    put("required", buildJsonArray { required.forEach { add(it) } })
    putJsonObject("properties") { properties() }
}
