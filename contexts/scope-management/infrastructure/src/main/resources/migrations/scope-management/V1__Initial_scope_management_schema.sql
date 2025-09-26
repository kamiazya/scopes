-- Initial schema for Scope Management bounded context
-- This migration creates all the tables and indexes for the scope management system

-- Scopes table
CREATE TABLE IF NOT EXISTS scopes (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    parent_id TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (parent_id) REFERENCES scopes(id) ON DELETE CASCADE
);

-- Scopes indexes
CREATE INDEX IF NOT EXISTS idx_scopes_parent_id ON scopes(parent_id);
CREATE INDEX IF NOT EXISTS idx_scopes_created_at ON scopes(created_at);
CREATE INDEX IF NOT EXISTS idx_scopes_updated_at ON scopes(updated_at);
CREATE INDEX IF NOT EXISTS idx_scopes_title_parent ON scopes(title, parent_id);
CREATE INDEX IF NOT EXISTS idx_scopes_parent_created ON scopes(parent_id, created_at, id);
CREATE INDEX IF NOT EXISTS idx_scopes_root_created ON scopes(created_at, id) WHERE parent_id IS NULL;

-- Scope aliases table
CREATE TABLE IF NOT EXISTS scope_aliases (
    id TEXT PRIMARY KEY NOT NULL,
    scope_id TEXT NOT NULL,
    alias_name TEXT NOT NULL UNIQUE,
    alias_type TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (scope_id) REFERENCES scopes(id) ON DELETE CASCADE
);

-- Scope aliases indexes
CREATE INDEX IF NOT EXISTS idx_scope_aliases_scope_id ON scope_aliases(scope_id);
-- idx_scope_aliases_alias_name is redundant - alias_name already has UNIQUE constraint
CREATE INDEX IF NOT EXISTS idx_scope_aliases_alias_type ON scope_aliases(alias_type);

-- Scope aspects table (align with SQLDelight schema)
CREATE TABLE IF NOT EXISTS scope_aspects (
    id INTEGER PRIMARY KEY,
    scope_id TEXT NOT NULL,
    aspect_key TEXT NOT NULL,
    aspect_value TEXT NOT NULL,
    FOREIGN KEY (scope_id) REFERENCES scopes(id) ON DELETE CASCADE,
    UNIQUE(scope_id, aspect_key, aspect_value)
);

-- Scope aspects indexes
CREATE INDEX IF NOT EXISTS idx_scope_aspects_scope_id ON scope_aspects(scope_id);
CREATE INDEX IF NOT EXISTS idx_scope_aspects_aspect_key ON scope_aspects(aspect_key);
CREATE INDEX IF NOT EXISTS idx_scope_aspects_key_value ON scope_aspects(aspect_key, aspect_value);

-- Context views table
CREATE TABLE IF NOT EXISTS context_views (
    id TEXT PRIMARY KEY NOT NULL,
    key TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    filter TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Context views indexes
-- idx_context_views_key is redundant - key already has UNIQUE constraint
-- idx_context_views_name is redundant - name already has UNIQUE constraint
CREATE INDEX IF NOT EXISTS idx_context_views_created_at ON context_views(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_context_views_updated_at ON context_views(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_context_views_key_filter ON context_views(key, filter) WHERE filter IS NOT NULL;

-- Aspect definitions table (align with SQLDelight schema)
CREATE TABLE IF NOT EXISTS aspect_definitions (
    key TEXT PRIMARY KEY NOT NULL,
    aspect_type TEXT NOT NULL,
    description TEXT,
    allow_multiple_values INTEGER NOT NULL DEFAULT 0
);

-- Aspect definitions indexes
CREATE INDEX IF NOT EXISTS idx_aspect_definitions_aspect_type ON aspect_definitions(aspect_type);

-- Active context table (single row table for current context)
CREATE TABLE IF NOT EXISTS active_context (
    id TEXT PRIMARY KEY NOT NULL DEFAULT 'default',
    context_view_id TEXT,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (context_view_id) REFERENCES context_views(id) ON DELETE SET NULL
);

-- Ensure only one active context exists
CREATE UNIQUE INDEX IF NOT EXISTS idx_active_context_single ON active_context(id);

-- Performance index for foreign key lookup
CREATE INDEX IF NOT EXISTS idx_active_context_view_id ON active_context(context_view_id) WHERE context_view_id IS NOT NULL;
