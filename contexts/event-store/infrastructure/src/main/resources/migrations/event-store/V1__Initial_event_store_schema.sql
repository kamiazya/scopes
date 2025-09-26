-- Initial schema for Event Store

CREATE TABLE IF NOT EXISTS events (
    sequence_number INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id TEXT NOT NULL UNIQUE,
    aggregate_id TEXT NOT NULL,
    aggregate_version INTEGER NOT NULL,
    event_type TEXT NOT NULL,
    event_data TEXT NOT NULL,
    occurred_at INTEGER NOT NULL,
    stored_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_events_aggregate_id_version ON events(aggregate_id, aggregate_version);
CREATE INDEX IF NOT EXISTS idx_events_stored_at ON events(stored_at);
CREATE INDEX IF NOT EXISTS idx_events_occurred_at ON events(occurred_at);
CREATE INDEX IF NOT EXISTS idx_events_event_type_sequence_number ON events(event_type, sequence_number);
CREATE INDEX IF NOT EXISTS idx_events_event_type_occurred_at ON events(event_type, occurred_at);

