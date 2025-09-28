#!/bin/bash
# Scopes Daemon Service Uninstallation Script

set -euo pipefail

SERVICE_NAME="scopesd"
USER_UNINSTALL=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# Check if running with sudo
check_sudo() {
    if [[ $EUID -eq 0 ]]; then
        return 0
    else
        return 1
    fi
}

# Uninstall systemd service (Linux)
uninstall_systemd() {
    if [[ "$USER_UNINSTALL" == "true" ]]; then
        local service_file="$HOME/.config/systemd/user/scopesd.service"

        log_info "Uninstalling user systemd service..."

        # Stop and disable service
        systemctl --user stop scopesd.service 2>/dev/null || true
        systemctl --user disable scopesd.service 2>/dev/null || true

        # Remove service file
        rm -f "$service_file"

        # Reload daemon
        systemctl --user daemon-reload

        log_info "User service uninstalled"
    else
        if ! check_sudo; then
            log_error "System-wide uninstallation requires sudo"
            exit 1
        fi

        local service_file="/etc/systemd/system/scopesd.service"

        log_info "Uninstalling system-wide systemd service..."

        # Stop and disable service
        systemctl stop scopesd.service 2>/dev/null || true
        systemctl disable scopesd.service 2>/dev/null || true

        # Remove service file
        rm -f "$service_file"

        # Reload daemon
        systemctl daemon-reload

        log_info "System service uninstalled"
    fi
}

# Uninstall launchd service (macOS)
uninstall_launchd() {
    local service_label="com.github.kamiazya.scopesd"

    if [[ "$USER_UNINSTALL" == "true" ]]; then
        local plist_file="$HOME/Library/LaunchAgents/$service_label.plist"

        log_info "Uninstalling user launchd service..."

        # Unload the service
        launchctl unload -w "$plist_file" 2>/dev/null || true

        # Remove the plist file
        rm -f "$plist_file"

        log_info "User service uninstalled"
    else
        if ! check_sudo; then
            log_error "System-wide uninstallation requires sudo"
            exit 1
        fi

        local plist_file="/Library/LaunchDaemons/$service_label.plist"

        log_info "Uninstalling system-wide launchd service..."

        # Unload the service
        launchctl unload -w "$plist_file" 2>/dev/null || true

        # Remove the plist file
        rm -f "$plist_file"

        log_info "System service uninstalled"
    fi
}

# Clean up runtime files
cleanup_runtime_files() {
    log_info "Cleaning up runtime files..."

    # Clean endpoint files
    if [[ "$(detect_os)" == "linux" ]]; then
        rm -f "${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/scopes/scopesd.endpoint"
    elif [[ "$(detect_os)" == "macos" ]]; then
        rm -f "$HOME/Library/Application Support/scopes/run/scopesd.endpoint"
    fi

    # Clean up log files if requested
    if [[ "${CLEAN_LOGS:-false}" == "true" ]]; then
        log_info "Cleaning up log files..."
        if [[ "$(detect_os)" == "linux" ]]; then
            rm -rf "$HOME/.local/state/scopes/logs"
        elif [[ "$(detect_os)" == "macos" ]]; then
            rm -rf "$HOME/Library/Logs/scopes"
        fi
    fi
}

# Main uninstallation logic
main() {
    local os_type

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --user)
                USER_UNINSTALL=true
                shift
                ;;
            --clean-logs)
                CLEAN_LOGS=true
                shift
                ;;
            --help|-h)
                echo "Usage: $0 [--user] [--clean-logs]"
                echo ""
                echo "Options:"
                echo "  --user        Uninstall user service"
                echo "  --clean-logs  Remove log files"
                echo "  --help        Show this help message"
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done

    os_type=$(detect_os)

    log_info "Detected OS: $os_type"
    log_info "Uninstallation type: $([ "$USER_UNINSTALL" == "true" ] && echo "User" || echo "System-wide")"

    case $os_type in
        linux)
            uninstall_systemd
            ;;
        macos)
            uninstall_launchd
            ;;
        *)
            log_error "Unsupported operating system: $OSTYPE"
            exit 1
            ;;
    esac

    cleanup_runtime_files

    log_info "Uninstallation complete!"
}

main "$@"
