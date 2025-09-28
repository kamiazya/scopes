#!/bin/bash
# Scopes Daemon Service Management Script

set -euo pipefail

SERVICE_NAME="scopesd"
USER_SERVICE=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_status() {
    echo -e "${BLUE}[STATUS]${NC} $1"
}

# Detect OS
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "linux"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macos"
    else
        echo "unsupported"
    fi
}

# Check if service is installed
is_service_installed() {
    local os_type=$1

    if [[ "$os_type" == "linux" ]]; then
        if [[ "$USER_SERVICE" == "true" ]]; then
            [[ -f "$HOME/.config/systemd/user/scopesd.service" ]]
        else
            [[ -f "/etc/systemd/system/scopesd.service" ]]
        fi
    elif [[ "$os_type" == "macos" ]]; then
        if [[ "$USER_SERVICE" == "true" ]]; then
            [[ -f "$HOME/Library/LaunchAgents/com.github.kamiazya.scopesd.plist" ]]
        else
            [[ -f "/Library/LaunchDaemons/com.github.kamiazya.scopesd.plist" ]]
        fi
    fi
}

# Get service status
get_service_status() {
    local os_type=$1

    if [[ "$os_type" == "linux" ]]; then
        if [[ "$USER_SERVICE" == "true" ]]; then
            systemctl --user is-active scopesd.service 2>/dev/null || echo "inactive"
        else
            systemctl is-active scopesd.service 2>/dev/null || echo "inactive"
        fi
    elif [[ "$os_type" == "macos" ]]; then
        if launchctl list | grep -q "com.github.kamiazya.scopesd"; then
            echo "active"
        else
            echo "inactive"
        fi
    fi
}

# Start service
start_service() {
    local os_type=$1

    log_info "Starting scopesd service..."

    if [[ "$os_type" == "linux" ]]; then
        if [[ "$USER_SERVICE" == "true" ]]; then
            systemctl --user start scopesd.service
        else
            sudo systemctl start scopesd.service
        fi
    elif [[ "$os_type" == "macos" ]]; then
        if [[ "$USER_SERVICE" == "true" ]]; then
            launchctl load -w "$HOME/Library/LaunchAgents/com.github.kamiazya.scopesd.plist" 2>/dev/null || \
                launchctl start com.github.kamiazya.scopesd
        else
            sudo launchctl load -w "/Library/LaunchDaemons/com.github.kamiazya.scopesd.plist" 2>/dev/null || \
                sudo launchctl start com.github.kamiazya.scopesd
        fi
    fi
}

# Stop service
stop_service() {
    local os_type=$1

    log_info "Stopping scopesd service..."

    if [[ "$os_type" == "linux" ]]; then
        if [[ "$USER_SERVICE" == "true" ]]; then
            systemctl --user stop scopesd.service
        else
            sudo systemctl stop scopesd.service
        fi
    elif [[ "$os_type" == "macos" ]]; then
        if [[ "$USER_SERVICE" == "true" ]]; then
            launchctl stop com.github.kamiazya.scopesd
        else
            sudo launchctl stop com.github.kamiazya.scopesd
        fi
    fi
}

# Restart service
restart_service() {
    local os_type=$1

    log_info "Restarting scopesd service..."

    if [[ "$os_type" == "linux" ]]; then
        if [[ "$USER_SERVICE" == "true" ]]; then
            systemctl --user restart scopesd.service
        else
            sudo systemctl restart scopesd.service
        fi
    elif [[ "$os_type" == "macos" ]]; then
        stop_service "$os_type"
        sleep 2
        start_service "$os_type"
    fi
}

# Show service status
show_status() {
    local os_type=$1

    log_status "Scopes Daemon Service Status"
    echo ""

    # Check if installed
    if ! is_service_installed "$os_type"; then
        log_warn "Service is not installed"
        echo "Run 'install-service.sh$([ "$USER_SERVICE" == "true" ] && echo " --user")' to install"
        return 1
    fi

    # Get status
    local status=$(get_service_status "$os_type")

    if [[ "$status" == "active" ]]; then
        log_info "Service is running"
    else
        log_warn "Service is not running"
    fi

    # Show detailed status
    echo ""
    if [[ "$os_type" == "linux" ]]; then
        if [[ "$USER_SERVICE" == "true" ]]; then
            systemctl --user status scopesd.service --no-pager
        else
            systemctl status scopesd.service --no-pager
        fi
    elif [[ "$os_type" == "macos" ]]; then
        launchctl list | grep com.github.kamiazya.scopesd || echo "Service not found in launchctl"
    fi

    # Check endpoint file
    echo ""
    log_status "Endpoint Information"
    local endpoint_file
    if [[ "$os_type" == "linux" ]]; then
        endpoint_file="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/scopes/scopesd.endpoint"
    else
        endpoint_file="$HOME/Library/Application Support/scopes/run/scopesd.endpoint"
    fi

    if [[ -f "$endpoint_file" ]]; then
        log_info "Endpoint file found: $endpoint_file"
        echo "Contents:"
        cat "$endpoint_file" | sed 's/^/  /'
    else
        log_warn "Endpoint file not found"
    fi
}

# Show logs
show_logs() {
    local os_type=$1
    local lines=${2:-50}

    log_status "Recent log entries (last $lines lines)"
    echo ""

    if [[ "$os_type" == "linux" ]]; then
        if [[ "$USER_SERVICE" == "true" ]]; then
            journalctl --user -u scopesd.service -n "$lines" --no-pager
        else
            sudo journalctl -u scopesd.service -n "$lines" --no-pager
        fi
    elif [[ "$os_type" == "macos" ]]; then
        local log_file="$HOME/Library/Logs/scopes/scopesd.log"
        if [[ -f "$log_file" ]]; then
            tail -n "$lines" "$log_file"
        else
            log_warn "Log file not found: $log_file"
        fi
    fi
}

# Main logic
main() {
    local command=""
    local os_type

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --user)
                USER_SERVICE=true
                shift
                ;;
            start|stop|restart|status|logs)
                command=$1
                shift
                ;;
            --lines)
                LINES=$2
                shift 2
                ;;
            --help|-h)
                echo "Usage: $0 [--user] <command> [options]"
                echo ""
                echo "Commands:"
                echo "  start     Start the scopesd service"
                echo "  stop      Stop the scopesd service"
                echo "  restart   Restart the scopesd service"
                echo "  status    Show service status"
                echo "  logs      Show service logs"
                echo ""
                echo "Options:"
                echo "  --user    Operate on user service"
                echo "  --lines N Show last N lines of logs (default: 50)"
                echo "  --help    Show this help message"
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done

    if [[ -z "$command" ]]; then
        log_error "No command specified"
        echo "Use --help for usage information"
        exit 1
    fi

    os_type=$(detect_os)

    if [[ "$os_type" == "unsupported" ]]; then
        log_error "Unsupported operating system: $OSTYPE"
        exit 1
    fi

    case $command in
        start)
            start_service "$os_type"
            ;;
        stop)
            stop_service "$os_type"
            ;;
        restart)
            restart_service "$os_type"
            ;;
        status)
            show_status "$os_type"
            ;;
        logs)
            show_logs "$os_type" "${LINES:-50}"
            ;;
    esac
}

main "$@"
