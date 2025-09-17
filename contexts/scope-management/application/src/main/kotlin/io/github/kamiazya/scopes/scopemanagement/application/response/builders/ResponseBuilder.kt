package io.github.kamiazya.scopes.scopemanagement.application.response.builders

interface ResponseBuilder<T> {
    fun buildMcpResponse(data: T): Map<String, Any>
    fun buildCliResponse(data: T): String
}
