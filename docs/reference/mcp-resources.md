# MCP Resources Reference

This document describes the resources exposed by the Scopes MCP server.

## Overview

The MCP server provides dynamic resources that allow AI assistants to explore the Scopes hierarchy and understand the current project structure. Resources support ETags for efficient caching and provide read-only access to scope information.

## Available Resources

### 1. CLI Quick Reference (`cli-quick-ref`)

A static resource providing quick reference for CLI commands.

- **URI**: `scopes://cli-quick-ref`
- **MIME Type**: `text/markdown`
- **Content**: Static markdown documentation of CLI commands

### 2. Scope Details (`scope/*`)

Dynamic resources providing detailed information about specific scopes.

- **URI Pattern**: `scopes://scope/{alias}`
- **MIME Type**: `application/json`
- **Content**: JSON representation of scope details
- **ETag Support**: Yes (based on scope modification timestamp)

#### Example Response:
```json
{
  "id": "01234567-89ab-cdef-0123-456789abcdef",
  "alias": "project-alpha",
  "title": "Project Alpha",
  "status": "IN_PROGRESS",
  "children": ["feature-x", "feature-y"]
}
```

### 3. Scope Tree View (`tree/*`)

Dynamic resources providing hierarchical tree views of scopes.

- **URI Pattern**: `scopes://tree/{alias}?depth={n}`
- **MIME Type**: `text/plain`
- **Content**: Tree-structured text representation
- **ETag Support**: Yes (based on tree content hash)

#### Query Parameters

##### `depth` (optional)
Controls the depth of tree traversal.

- **Default**: `1`
- **Valid Range**: `1-5` (automatically coerced to this range)
- **Description**: Specifies how many levels deep to traverse the scope hierarchy

#### Performance Considerations

The tree view implements several safeguards to prevent performance issues:

##### Maximum Nodes Limit
- **Limit**: 1000 nodes per tree
- **Behavior**: Tree traversal stops when the limit is reached
- **Indication**: Appends "(truncated)" to the last node when limit is hit

##### Depth Limiting
- **Maximum Depth**: 5 levels
- **Default Depth**: 1 level (immediate children only)
- **Rationale**: Prevents excessive memory usage and response times for deep hierarchies

#### Example Tree Output:
```
project-alpha (Project Alpha) [IN_PROGRESS]
├── feature-x (Feature X) [IN_PROGRESS]
│   ├── task-1 (Implement API) [TODO]
│   └── task-2 (Write tests) [TODO]
└── feature-y (Feature Y) [PLANNING]
    └── task-3 (Design UI) [TODO]
```

## Best Practices

### 1. Use Appropriate Depth
- Start with `depth=1` to get immediate children
- Increase depth only when needed to explore specific branches
- Be aware that higher depths may result in truncated output

### 2. Leverage ETags
- Resources support ETags for efficient caching
- The server will return 304 Not Modified for unchanged resources
- This reduces bandwidth and improves performance

### 3. Handle Truncation
- Always check for "(truncated)" in tree views
- If truncation occurs, consider:
  - Reducing the depth parameter
  - Focusing on specific subtrees
  - Using multiple targeted requests instead of one large request

## Future Enhancements

The current implementation uses recursive tree building which may cause performance issues with very large hierarchies. Future versions may implement:

1. **Iterative Tree Building**: Replace recursion with iteration to handle deeper trees
2. **Streaming Responses**: Stream large trees instead of building in memory
3. **Pagination**: Add skip/limit parameters for navigating large sibling sets
4. **Custom Node Limits**: Allow clients to specify their own node limits

## Error Handling

Resources may return errors in the following cases:

- **404 Not Found**: When the requested scope alias doesn't exist
- **400 Bad Request**: When URI format is invalid
- **500 Internal Server Error**: When an unexpected error occurs

All errors are returned as JSON with an error message:
```json
{
  "error": "Scope not found: unknown-alias"
}
```