# MCP Implementation Guide

## Overview

Scopes includes a fully functional MCP (Model Context Protocol) server that enables AI assistants to interact with the scope management system. The implementation is complete and operational, providing tools, resources, and prompts for AI-driven task management.

## Available MCP Tools (11 Implemented)

### Scope Management Tools

#### 1. `scopes.create`
Creates a new scope with optional parent and custom alias.
```json
{
  "title": "Implement authentication",
  "description": "Add user authentication system",
  "parentAlias": "backend-project",
  "customAlias": "auth-feature",
  "idempotencyKey": "unique-key-123"
}
```

#### 2. `scopes.get`
Retrieves detailed information about a specific scope.
```json
{
  "alias": "auth-feature"
}
```

#### 3. `scopes.update`
Updates an existing scope's title or description.
```json
{
  "alias": "auth-feature",
  "title": "Enhanced authentication system",
  "description": "Multi-factor authentication support"
}
```

#### 4. `scopes.delete`
Deletes a scope and all its children.
```json
{
  "alias": "old-feature"
}
```

#### 5. `scopes.children`
Lists all direct children of a scope.
```json
{
  "parentAlias": "project-root",
  "includeDescendants": false
}
```

#### 6. `scopes.roots`
Lists all root-level scopes (no parent).
```json
{}
```

### Alias Management Tools

#### 7. `aliases.add`
Adds a new alias to an existing scope.
```json
{
  "scopeAlias": "quiet-river-x7k",
  "newAlias": "sprint-42"
}
```

#### 8. `aliases.remove`
Removes an alias from a scope.
```json
{
  "alias": "old-alias"
}
```

#### 9. `aliases.setCanonical`
Sets a new canonical alias for a scope.
```json
{
  "currentAlias": "quiet-river-x7k",
  "newCanonicalAlias": "main-project"
}
```

#### 10. `aliases.list`
Lists all aliases for a specific scope.
```json
{
  "scopeAlias": "main-project"
}
```

#### 11. `aliases.resolve`
Resolves an alias or prefix to the full scope information.
```json
{
  "aliasOrPrefix": "auth"
}
```

## MCP Resources

### 1. CLI Quick Reference (`cli-quick-ref`)
Static markdown documentation of all CLI commands.
- **URI**: `scopes://cli-quick-ref`
- **Type**: `text/markdown`
- **Content**: Complete CLI command reference

### 2. Scope Details (`scope/{alias}`)
Dynamic JSON representation of scope information.
- **URI**: `scopes://scope/{alias}`
- **Type**: `application/json`
- **ETag Support**: Yes

Example:
```json
{
  "id": "01HXY...",
  "canonicalAlias": "auth-feature",
  "aliases": ["auth-feature", "sprint-42"],
  "title": "Authentication System",
  "status": "IN_PROGRESS",
  "parentId": "01HXZ...",
  "children": ["login-ui", "password-validation"]
}
```

### 3. Tree Views (`tree/{alias}`)
Hierarchical tree representation in text or markdown.
- **URI**: `scopes://tree/{alias}?depth={n}&format={text|markdown}`
- **Depth**: 1-5 levels (default: 1)
- **Max Nodes**: 1000 (truncates if exceeded)

Example output:
```
project-alpha (Project Alpha) [IN_PROGRESS]
├── feature-x (Feature X) [IN_PROGRESS]
│   ├── task-1 (Implement API) [TODO]
│   └── task-2 (Write tests) [TODO]
└── feature-y (Feature Y) [PLANNING]
```

## MCP Prompts

### 1. Outline Prompt (`scopes.outline`)
Generates structured outlines for scope hierarchies.
```json
{
  "scopeAlias": "project-root",
  "depth": 3,
  "includeStatus": true
}
```

### 2. Plan Prompt (`scopes.plan`)
Creates execution plans for scope completion.
```json
{
  "scopeAlias": "feature-x",
  "includeEstimates": true,
  "includeDependencies": true
}
```

### 3. Summarize Prompt (`scopes.summarize`)
Generates summaries of scope progress and status.
```json
{
  "scopeAlias": "sprint-42",
  "includeMetrics": true,
  "includeBlockers": true
}
```

## Tool Annotations

Each tool includes metadata for AI assistants:

```kotlin
ToolAnnotations(
    title = "Create Scope",
    readOnlyHint = false,        // Modifies data
    destructiveHint = false,      // Not destructive
    idempotentHint = true        // Supports idempotency
)
```

## Integration with AI Assistants

### Claude Desktop Configuration

Add to your Claude Desktop config:
```json
{
  "mcpServers": {
    "scopes": {
      "command": "scopes",
      "args": ["mcp"],
      "env": {
        "SCOPES_MCP_MODE": "stdio"
      }
    }
  }
}
```

### Supported Transports
- **stdio**: Standard input/output (default)
- **HTTP**: REST API endpoint (future)
- **WebSocket**: Real-time connection (future)

## Error Handling

The MCP server provides structured error responses:

```json
{
  "error": {
    "code": -32602,
    "message": "Invalid params",
    "data": {
      "field": "alias",
      "reason": "Scope not found: unknown-alias"
    }
  }
}
```

Error codes follow JSON-RPC 2.0 specification:
- `-32700`: Parse error
- `-32600`: Invalid request
- `-32601`: Method not found
- `-32602`: Invalid params
- `-32603`: Internal error

## Idempotency Support

Tools that modify data support idempotency keys:

```kotlin
class IdempotencyService {
    // Prevents duplicate operations
    suspend fun executeOnce(
        key: String,
        operation: suspend () -> Result
    ): Result
}
```

Usage:
```json
{
  "title": "New task",
  "idempotencyKey": "create-task-2025-01-18-001"
}
```

## Performance Characteristics

### Response Times
- Tool execution: < 100ms (typical)
- Resource fetching: < 50ms (with caching)
- Tree generation: < 200ms (up to 1000 nodes)

### Caching
- ETags for resource caching
- In-memory cache for frequently accessed scopes
- Query result caching (5-minute TTL)

### Concurrency
- Thread-safe operation handlers
- Coroutine-based async execution
- Connection pooling for database access

## Security Considerations

### Authentication
Currently, the MCP server runs locally and doesn't require authentication. Future versions will support:
- API key authentication
- OAuth 2.0 flow
- JWT tokens

### Authorization
All operations currently have full access. Future enhancements:
- Role-based access control (RBAC)
- Scope-level permissions
- Operation whitelisting

### Data Protection
- No sensitive data in logs
- Parameterized queries prevent SQL injection
- Input validation on all tool parameters

## Testing the MCP Server

### Manual Testing
```bash
# Start MCP server
scopes mcp

# In another terminal, send test requests
echo '{"jsonrpc":"2.0","method":"scopes.create","params":{"title":"Test"},"id":1}' | scopes mcp
```

### Integration Tests
Located in `interfaces/mcp/src/test/kotlin/`:
- `McpServerIntegrationTest.kt` - End-to-end tests
- Tool-specific test files for each handler

### Load Testing
```kotlin
@Test
fun `handle concurrent requests`() = runTest {
    val requests = (1..1000).map { i ->
        async {
            mcpServer.handle(createRequest(i))
        }
    }
    requests.awaitAll()
    // Verify all succeeded
}
```

## Extending the MCP Server

### Adding New Tools

1. Create tool handler:
```kotlin
class MyToolHandler : ToolHandler {
    override val name = "scopes.myTool"
    override val description = "My custom tool"

    override suspend fun handle(
        arguments: JsonElement,
        context: ToolContext
    ): CallToolResult {
        // Implementation
    }
}
```

2. Register in `ToolRegistrar`:
```kotlin
registrar.register(MyToolHandler())
```

3. Add tests:
```kotlin
class MyToolHandlerTest {
    @Test
    fun `test my tool`() {
        // Test implementation
    }
}
```

### Adding New Resources

1. Create resource handler:
```kotlin
class MyResourceHandler : ResourceHandler {
    override fun handles(uri: String) =
        uri.startsWith("scopes://my-resource")

    override suspend fun handle(
        uri: String,
        context: ResourceContext
    ): Resource {
        // Implementation
    }
}
```

2. Register in `ResourceRegistrar`:
```kotlin
registrar.register(MyResourceHandler())
```

## Monitoring and Debugging

### Enable Debug Logging
```bash
export SCOPES_LOG_LEVEL=DEBUG
scopes mcp
```

### MCP Protocol Tracing
```bash
export SCOPES_MCP_TRACE=true
scopes mcp
```

This logs all incoming requests and outgoing responses.

### Performance Metrics
```bash
export SCOPES_MCP_METRICS=true
scopes mcp
```

Outputs timing information for each request.

## Future Enhancements

### Near-term
- Additional tools for aspect and context management
- Batch operations support
- Streaming responses for large datasets

### Long-term
- WebSocket transport for real-time updates
- GraphQL-style query capabilities
- AI model fine-tuning data export

## Related Documentation

- [MCP Resources Reference](./mcp-resources.md) - Resource details
- [CLI Quick Reference](./cli-quick-reference.md) - CLI commands
- [API Reference](./api/) - Programmatic API
- [Architecture Overview](../explanation/clean-architecture.md) - System architecture