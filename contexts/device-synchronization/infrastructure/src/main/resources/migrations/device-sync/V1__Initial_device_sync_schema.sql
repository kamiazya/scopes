-- Initial schema for Device Synchronization

CREATE TABLE IF NOT EXISTS devices (
    device_id TEXT PRIMARY KEY NOT NULL,
    last_sync_at INTEGER,
    last_successful_push INTEGER,
    last_successful_pull INTEGER,
    sync_status TEXT NOT NULL DEFAULT 'NEVER_SYNCED',
    pending_changes INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_devices_sync_status ON devices(sync_status);
CREATE INDEX IF NOT EXISTS idx_devices_updated_at ON devices(updated_at);

CREATE TABLE IF NOT EXISTS vector_clocks (
    device_id TEXT NOT NULL,
    component_device TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    PRIMARY KEY (device_id, component_device)
);

CREATE INDEX IF NOT EXISTS idx_vector_clocks_device_id ON vector_clocks(device_id);

