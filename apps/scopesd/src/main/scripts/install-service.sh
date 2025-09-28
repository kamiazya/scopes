#!/bin/bash
# Scopes Daemon Service Installation Script

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_NAME="scopesd"
USER_INSTALL=false

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

# Install systemd service (Linux)
install_systemd() {
    local service_file
    local target_dir

    if [[ "$USER_INSTALL" == "true" ]]; then
        service_file="${SCRIPT_DIR}/../resources/systemd/scopesd.user.service"
        target_dir="$HOME/.config/systemd/user"

        log_info "Installing user systemd service..."
        mkdir -p "$target_dir"
        cp "$service_file" "$target_dir/scopesd.service"

        systemctl --user daemon-reload
        systemctl --user enable scopesd.service
        systemctl --user start scopesd.service

        log_info "User service installed and started"
        log_info "Check status with: systemctl --user status scopesd"
    else
        if ! check_sudo; then
            log_error "System-wide installation requires sudo"
            exit 1
        fi

        service_file="${SCRIPT_DIR}/../resources/systemd/scopesd.service"
        target_dir="/etc/systemd/system"

        log_info "Installing system-wide systemd service..."
        cp "$service_file" "$target_dir/"

        systemctl daemon-reload
        systemctl enable scopesd.service
        systemctl start scopesd.service

        log_info "System service installed and started"
        log_info "Check status with: systemctl status scopesd"
    fi
}

# Install launchd service (macOS)
install_launchd() {
    local plist_file
    local target_dir
    local service_label="com.github.kamiazya.scopesd"

    if [[ "$USER_INSTALL" == "true" ]]; then
        plist_file="${SCRIPT_DIR}/../resources/launchd/com.github.kamiazya.scopesd.user.plist"
        target_dir="$HOME/Library/LaunchAgents"

        log_info "Installing user launchd service..."
        mkdir -p "$target_dir"

        # Replace USER placeholder with actual username
        sed "s/USER/$USER/g" "$plist_file" > "$target_dir/$service_label.plist"

        # Load the service
        launchctl load -w "$target_dir/$service_label.plist"

        log_info "User service installed and started"
        log_info "Check status with: launchctl list | grep $service_label"
    else
        if ! check_sudo; then
            log_error "System-wide installation requires sudo"
            exit 1
        fi

        plist_file="${SCRIPT_DIR}/../resources/launchd/com.github.kamiazya.scopesd.plist"
        target_dir="/Library/LaunchDaemons"

        log_info "Installing system-wide launchd service..."

        # Replace USER placeholder with actual username
        sed "s/USER/$SUDO_USER/g" "$plist_file" > "$target_dir/$service_label.plist"

        # Set proper permissions
        chown root:wheel "$target_dir/$service_label.plist"
        chmod 644 "$target_dir/$service_label.plist"

        # Load the service
        launchctl load -w "$target_dir/$service_label.plist"

        log_info "System service installed and started"
        log_info "Check status with: sudo launchctl list | grep $service_label"
    fi
}

# Main installation logic
main() {
    local os_type

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --user)
                USER_INSTALL=true
                shift
                ;;
            --help|-h)
                echo "Usage: $0 [--user]"
                echo ""
                echo "Options:"
                echo "  --user    Install as user service (no sudo required)"
                echo "  --help    Show this help message"
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
    log_info "Installation type: $([ "$USER_INSTALL" == "true" ] && echo "User" || echo "System-wide")"

    case $os_type in
        linux)
            install_systemd
            ;;
        macos)
            install_launchd
            ;;
        *)
            log_error "Unsupported operating system: $OSTYPE"
            exit 1
            ;;
    esac

    log_info "Installation complete!"
}

main "$@"
