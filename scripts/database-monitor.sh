#!/bin/bash

# Database Performance Monitoring Script for Scopes
# This script monitors database performance metrics and query execution

set -euo pipefail

# Configuration
DB_PATH="${1:-${HOME}/.scopes/data/scopes.db}"
INTERVAL="${2:-5}" # Monitoring interval in seconds
DURATION="${3:-60}" # Total monitoring duration in seconds

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_color() {
    local color=$1
    shift
    echo -e "${color}$*${NC}"
}

# Function to get database metrics
get_metrics() {
    sqlite3 "$DB_PATH" <<EOF
.mode list
.separator "|"
SELECT
    'cache_hit_rate',
    ROUND(CAST(cache_hit AS REAL) / (cache_hit + cache_miss) * 100, 2) || '%'
FROM (
    SELECT
        (SELECT value FROM pragma_stats WHERE name = 'cache_hit') AS cache_hit,
        (SELECT value FROM pragma_stats WHERE name = 'cache_miss') AS cache_miss
)
WHERE cache_hit + cache_miss > 0;

SELECT 'page_cache_size', page_size * cache_size / 1024 / 1024 || ' MB'
FROM pragma_page_size(), pragma_cache_size();

SELECT 'wal_checkpoint', wal_autocheckpoint || ' pages'
FROM pragma_wal_autocheckpoint();

SELECT 'database_size', page_count * page_size / 1024 / 1024 || ' MB'
FROM pragma_page_count(), pragma_page_size();

SELECT 'free_pages', freelist_count
FROM pragma_freelist_count();

SELECT 'busy_timeout', timeout || ' ms'
FROM pragma_busy_timeout();
EOF
}

# Function to monitor active queries (simulated for SQLite)
monitor_queries() {
    # SQLite doesn't have built-in query monitoring like other databases
    # This is a placeholder for application-level monitoring
    echo "Active queries monitoring not available for SQLite"
    echo "Consider implementing application-level query logging"
}

# Function to check for long-running operations
check_locks() {
    # Check if database is locked by trying to begin an immediate transaction
    if ! sqlite3 "$DB_PATH" "BEGIN IMMEDIATE; ROLLBACK;" 2>&1 | grep -q "locked"; then
        print_color $GREEN "No active locks detected"
    else
        print_color $YELLOW "WARNING: Database has active locks"
    fi
}

# Main monitoring loop
main() {
    if [[ ! -f "$DB_PATH" ]]; then
        print_color $RED "ERROR: Database $DB_PATH not found"
        exit 1
    fi

    # Check if sqlite3 command is available
    if ! command -v sqlite3 &> /dev/null; then
        print_color $RED "ERROR: sqlite3 command not found. Please install SQLite3."
        exit 1
    fi

    print_color $GREEN "=== Database Performance Monitor ==="
    echo "Database: $DB_PATH"
    echo "Interval: ${INTERVAL}s"
    echo "Duration: ${DURATION}s"
    echo ""

    local end_time=$(($(date +%s) + DURATION))
    local iteration=0

    while [[ $(date +%s) -lt $end_time ]]; do
        iteration=$((iteration + 1))

        clear
        print_color $GREEN "=== Iteration $iteration - $(date +'%Y-%m-%d %H:%M:%S') ==="
        echo ""

        # Get and display metrics
        print_color $YELLOW "Database Metrics:"
        while IFS='|' read -r metric value; do
            printf "  %-20s: %s\n" "$metric" "$value"
        done < <(get_metrics)
        echo ""

        # Check for locks
        print_color $YELLOW "Lock Status:"
        check_locks
        echo ""

        # Query monitoring placeholder
        print_color $YELLOW "Query Monitoring:"
        monitor_queries
        echo ""

        # Performance recommendations
        print_color $YELLOW "Performance Tips:"
        echo "  - Run './database-maintenance.sh' regularly for optimization"
        echo "  - Monitor application logs for slow queries"
        echo "  - Consider increasing cache_size for better performance"
        echo ""

        if [[ $(date +%s) -lt $end_time ]]; then
            echo "Next update in ${INTERVAL} seconds... (Ctrl+C to stop)"
            sleep "$INTERVAL"
        fi
    done

    print_color $GREEN "Monitoring completed after ${DURATION} seconds"
}

# Handle Ctrl+C gracefully
trap 'echo ""; print_color $YELLOW "Monitoring stopped by user"; exit 0' INT

# Run main function
main
