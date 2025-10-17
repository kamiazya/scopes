#!/bin/bash

# Database Maintenance Script for Scopes
# This script performs routine maintenance on SQLite databases used by Scopes

set -euo pipefail

# Configuration
DB_DIR="${SCOPES_DB_DIR:-${HOME}/.scopes/data}"
LOG_FILE="${SCOPES_LOG_DIR:-${HOME}/.scopes/logs}/db-maintenance-$(date '+%Y%m%d-%H%M%S').log"

# Ensure directories exist
mkdir -p "$(dirname "$LOG_FILE")"

# Logging function
log() {
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

# Error handling
trap 'log "ERROR: Script failed at line $LINENO"' ERR

# Function to optimize a database
optimize_database() {
  local db_path="$1"
  local db_name
  db_name=$(basename "$db_path")

  log "Starting optimization for $db_name"

  if [[ ! -f "$db_path" ]]; then
    log "WARNING: Database $db_path not found, skipping"
    return 0
  fi

  # Create backup before optimization using sqlite3 .backup for WAL safety
  local backup_path
  backup_path="${db_path}.backup.$(date '+%Y%m%d-%H%M%S')"
  log "Creating backup: $backup_path"
  sqlite3 "$db_path" ".backup '$backup_path'"

  # Run SQLite optimization commands
  if sqlite3 "$db_path" <<EOF 2>&1 | tee -a "$LOG_FILE"; then
-- Analyze database for query optimizer
PRAGMA analysis_limit=1000;
ANALYZE;

-- Check database integrity
PRAGMA integrity_check;

-- Optimize database
PRAGMA optimize;

-- Vacuum to reclaim space
VACUUM;

-- Update page count statistics
PRAGMA page_count;
PRAGMA freelist_count;

-- Show database stats
SELECT 'Database size: ' || page_count * page_size / 1024 / 1024 || ' MB' FROM pragma_page_count(), pragma_page_size();
SELECT 'Free pages: ' || freelist_count FROM pragma_freelist_count();
EOF
    log "Successfully optimized $db_name"

    # Remove old backups (keep last 7 days)
    find "$(dirname "$db_path")" -name "$(basename "$db_path").backup.*" -mtime +7 -delete 2>/dev/null || true
    log "Cleaned up old backups for $db_name"
  else
    log "ERROR: Failed to optimize $db_name"
    return 1
  fi
}

# Function to analyze database performance
analyze_performance() {
  local db_path="$1"
  local db_name
  db_name=$(basename "$db_path")

  log "Analyzing performance for $db_name"

  sqlite3 "$db_path" <<EOF 2>&1 | tee -a "$LOG_FILE"
-- Check for missing indexes
SELECT 'Tables without any index:';
SELECT name FROM sqlite_master
WHERE type='table'
AND name NOT IN (
    SELECT DISTINCT tbl_name FROM sqlite_master WHERE type='index'
)
AND name NOT LIKE 'sqlite_%';

-- Show table statistics
SELECT 'Table statistics:';
SELECT
    m.name AS table_name,
    COUNT(DISTINCT i.name) AS index_count,
    (SELECT COUNT(*) FROM pragma_table_info(m.name)) AS column_count
FROM sqlite_master m
LEFT JOIN sqlite_master i ON m.name = i.tbl_name AND i.type = 'index'
WHERE m.type = 'table' AND m.name NOT LIKE 'sqlite_%'
GROUP BY m.name
ORDER BY m.name;

-- Show index usage (requires SQLite 3.32.0+)
SELECT 'Most used indexes:';
SELECT name, tbl_name FROM sqlite_master
WHERE type = 'index'
ORDER BY name;
EOF
}

# Main execution
main() {
  log "=== Starting database maintenance ==="
  log "Database directory: $DB_DIR"

  if [[ ! -d "$DB_DIR" ]]; then
    log "ERROR: Database directory $DB_DIR not found"
    exit 1
  fi

  # Find all SQLite databases
  local databases
  mapfile -t databases < <(find "$DB_DIR" -name "*.db" -type f 2>/dev/null)

  if [[ ${#databases[@]} -eq 0 ]]; then
    log "No databases found in $DB_DIR"
    exit 0
  fi

  log "Found ${#databases[@]} database(s)"

  # Process each database
  for db in "${databases[@]}"; do
    log "----------------------------------------"
    optimize_database "$db"
    analyze_performance "$db"
  done

  log "----------------------------------------"
  log "=== Database maintenance completed ==="

  # Summary
  log ""
  log "Summary:"
  log "- Processed ${#databases[@]} database(s)"
  log "- Log file: $LOG_FILE"
}

# Run main function
main "$@"
