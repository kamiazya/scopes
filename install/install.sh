#!/bin/bash

# Scopes Installation Script for JAR Distribution
# This script installs Scopes JAR with wrapper scripts
#
# Usage:
#   ./install.sh [OPTIONS]
#
# Options:
#   -d, --install-dir DIR     Installation directory (default: /usr/local)
#   -f, --force              Skip confirmation prompts
#   -v, --verbose            Enable verbose output
#   -h, --help               Show help message

set -Eeuo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Configuration from environment or defaults
INSTALL_DIR="${SCOPES_INSTALL_DIR:-/usr/local}"
FORCE_INSTALL="${SCOPES_FORCE_INSTALL:-false}"
VERBOSE="${SCOPES_VERBOSE:-false}"

# Internal variables
JAR_FILE=""
WRAPPER_SCRIPT=""

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}${BOLD}=== $1 ===${NC}"
}

print_verbose() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}[DEBUG]${NC} $1"
    fi
}

# Function to show help
show_help() {
    cat << EOF
Scopes Installation Script for JAR Distribution

This script installs Scopes JAR file with wrapper scripts.

USAGE:
    $0 [OPTIONS]

OPTIONS:
    -d, --install-dir DIR     Installation directory (default: /usr/local)
    -f, --force              Skip confirmation prompts
    -v, --verbose            Enable verbose output
    -h, --help               Show this help message

ENVIRONMENT VARIABLES:
    SCOPES_INSTALL_DIR       Installation directory
    SCOPES_FORCE_INSTALL     Skip confirmation prompts
    SCOPES_VERBOSE           Enable verbose output

EXAMPLES:
    # Standard installation (requires sudo)
    sudo ./install.sh

    # Custom installation directory
    ./install.sh --install-dir ~/.local

    # Force installation without prompts
    sudo ./install.sh --force

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--install-dir)
            INSTALL_DIR="$2"
            shift 2
            ;;
        -f|--force)
            FORCE_INSTALL="true"
            shift
            ;;
        -v|--verbose)
            VERBOSE="true"
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Check Java installation
check_java() {
    print_header "Checking Java Installation"

    if ! command -v java &> /dev/null; then
        print_error "Java is not installed"
        echo ""
        echo "Scopes requires Java 21 or later to run."
        echo ""
        echo "Installation instructions:"
        echo "  Linux (Debian/Ubuntu): sudo apt install openjdk-21-jre"
        echo "  Linux (Fedora/RHEL):   sudo dnf install java-21-openjdk"
        echo "  macOS:                 brew install openjdk@21"
        echo "  Or download from:      https://adoptium.net/"
        exit 1
    fi

    # Extract major version number (handles both old and new version formats, EA builds, and vendor variations)
    JAVA_VERSION=$(java -version 2>&1 | sed -E -n 's/.*version "([0-9]+).*/\1/p' | head -n 1)

    if [ -z "$JAVA_VERSION" ]; then
        print_error "Could not determine Java version"
        exit 1
    fi

    if [ "$JAVA_VERSION" -lt 21 ]; then
        print_error "Java $JAVA_VERSION is installed, but Scopes requires Java 21 or later"
        echo ""
        echo "Please upgrade your Java installation:"
        echo "  Current version: Java $JAVA_VERSION"
        echo "  Required version: Java 21+"
        exit 1
    fi

    print_status "Java $JAVA_VERSION detected"
}

# Detect binary
detect_files() {
    print_header "Detecting Installation Files"

    # Find JAR file
    if [ -f "$SCRIPT_DIR/scopes.jar" ]; then
        JAR_FILE="$SCRIPT_DIR/scopes.jar"
    elif [ -f "$SCRIPT_DIR/../scopes.jar" ]; then
        JAR_FILE="$SCRIPT_DIR/../scopes.jar"
    else
        print_error "scopes.jar not found"
        exit 1
    fi
    print_verbose "JAR file: $JAR_FILE"

    # Find wrapper script
    if [ -f "$SCRIPT_DIR/bin/scopes" ]; then
        WRAPPER_SCRIPT="$SCRIPT_DIR/bin/scopes"
    elif [ -f "$SCRIPT_DIR/../bin/scopes" ]; then
        WRAPPER_SCRIPT="$SCRIPT_DIR/../bin/scopes"
    else
        print_error "Wrapper script not found"
        exit 1
    fi
    print_verbose "Wrapper script: $WRAPPER_SCRIPT"

    print_status "Installation files detected"
}

# Verify installation
verify_installation() {
    print_header "Verifying Installation"

    # Check if hash file exists
    HASH_FILE="$SCRIPT_DIR/verification/scopes.jar.sha256"
    if [ ! -f "$HASH_FILE" ]; then
        print_warning "Hash file not found, skipping verification"
        return
    fi

    print_status "Verifying SHA256 hash..."

    # Verify hash
    if command -v sha256sum &> /dev/null; then
        echo "$(cat "$HASH_FILE") $JAR_FILE" | sha256sum -c - || {
            print_error "Hash verification failed"
            exit 1
        }
    elif command -v shasum &> /dev/null; then
        echo "$(cat "$HASH_FILE") $JAR_FILE" | shasum -a 256 -c - || {
            print_error "Hash verification failed"
            exit 1
        }
    else
        print_warning "sha256sum/shasum not found, skipping hash verification"
        return
    fi

    print_status "Hash verification passed"

    # SLSA verification (optional if slsa-verifier is available)
    SLSA_FILE="$SCRIPT_DIR/verification/multiple.intoto.jsonl"
    if [ -f "$SLSA_FILE" ] && command -v slsa-verifier &> /dev/null; then
        print_status "Verifying SLSA provenance..."
        if slsa-verifier verify-artifact "$JAR_FILE" \
            --provenance-path "$SLSA_FILE" \
            --source-uri github.com/kamiazya/scopes &> /dev/null; then
            print_status "SLSA provenance verification passed"
        else
            print_warning "SLSA provenance verification failed (non-critical)"
        fi
    elif [ -f "$SLSA_FILE" ]; then
        print_verbose "SLSA provenance file found, but slsa-verifier is not installed"
        print_verbose "To enable SLSA verification: go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest"
    fi
}

# Install files
install_files() {
    print_header "Installing Scopes"

    # Create installation directories
    mkdir -p "$INSTALL_DIR/bin"
    mkdir -p "$INSTALL_DIR/lib"

    # Copy JAR file
    print_status "Installing JAR file to $INSTALL_DIR/lib/"
    cp "$JAR_FILE" "$INSTALL_DIR/lib/scopes.jar"

    # Copy wrapper script
    print_status "Installing wrapper script to $INSTALL_DIR/bin/"
    cp "$WRAPPER_SCRIPT" "$INSTALL_DIR/bin/scopes"
    chmod +x "$INSTALL_DIR/bin/scopes"

    print_status "Installation completed successfully"
}

# Test installation
test_installation() {
    print_header "Testing Installation"

    if ! "$INSTALL_DIR/bin/scopes" --help &> /dev/null; then
        print_error "Installation test failed"
        print_error "Scopes command is not working properly"
        exit 1
    fi

    print_status "Installation test passed"

    # Show help to verify installation
    echo ""
    echo "Scopes has been installed successfully!"
    echo "Run 'scopes --help' to get started"
}

# Main installation flow
main() {
    print_header "Scopes JAR Installation"
    echo ""

    # Check Java
    check_java
    echo ""

    # Detect files
    detect_files
    echo ""

    # Verify (optional)
    verify_installation
    echo ""

    # Confirm installation
    if [[ "$FORCE_INSTALL" != "true" ]]; then
        echo -e "${BOLD}Installation Summary:${NC}"
        echo "  JAR file:      $JAR_FILE"
        echo "  Wrapper:       $WRAPPER_SCRIPT"
        echo "  Install to:    $INSTALL_DIR"
        echo ""
        read -p "Continue with installation? [y/N] " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_warning "Installation cancelled"
            exit 0
        fi
    fi

    # Install
    install_files
    echo ""

    # Test
    test_installation
    echo ""

    print_status "Scopes has been installed successfully!"
    echo ""

    # Check PATH and offer to update shell profile
    if [[ ":$PATH:" != *":$INSTALL_DIR/bin:"* ]]; then
        print_warning "$INSTALL_DIR/bin is not in your PATH"
        echo ""

        # Detect shell and profile file
        SHELL_NAME=$(basename "$SHELL")
        case "$SHELL_NAME" in
            bash)
                PROFILE_FILE="$HOME/.bashrc"
                [ -f "$HOME/.bash_profile" ] && PROFILE_FILE="$HOME/.bash_profile"
                ;;
            zsh)
                PROFILE_FILE="$HOME/.zshrc"
                ;;
            fish)
                PROFILE_FILE="$HOME/.config/fish/config.fish"
                ;;
            *)
                PROFILE_FILE="$HOME/.profile"
                ;;
        esac

        echo "Detected shell: $SHELL_NAME"
        echo "Profile file: $PROFILE_FILE"
        echo ""

        if [[ "$FORCE_INSTALL" != "true" ]]; then
            read -p "Would you like to add $INSTALL_DIR/bin to PATH in $PROFILE_FILE? [y/N] " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                echo "" >> "$PROFILE_FILE"
                echo "# Added by Scopes installer" >> "$PROFILE_FILE"
                echo "export PATH=\"$INSTALL_DIR/bin:\$PATH\"" >> "$PROFILE_FILE"
                print_status "PATH updated in $PROFILE_FILE"
                echo ""
                echo "Please run 'source $PROFILE_FILE' or restart your shell to apply changes"
            else
                echo ""
                echo "You can manually add this line to your shell profile ($PROFILE_FILE):"
                echo "  export PATH=\"$INSTALL_DIR/bin:\$PATH\""
            fi
        else
            echo "Add this line to your shell profile ($PROFILE_FILE):"
            echo "  export PATH=\"$INSTALL_DIR/bin:\$PATH\""
        fi
    else
        print_status "$INSTALL_DIR/bin is already in your PATH"
    fi

    echo ""
    echo "Next steps:"
    echo "  Run 'scopes --help' to get started"
    echo ""
}

main "$@"
