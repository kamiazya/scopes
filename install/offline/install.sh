#!/bin/bash

# Scopes Offline Installation Script
# This script installs Scopes from a pre-downloaded offline package
#
# Usage:
#   ./install.sh [OPTIONS]
#
# Options:
#   -d, --install-dir DIR     Installation directory (default: /usr/local/bin)
#   -f, --force              Skip confirmation prompts
#   -s, --skip-verification  Skip security verification (not recommended)
#   -v, --verbose            Enable verbose output
#   -h, --help               Show help message
#
# Environment variables:
#   SCOPES_INSTALL_DIR          - Installation directory
#   SCOPES_SKIP_VERIFICATION    - Skip all verification (hash + SLSA) (not recommended)
#   SCOPES_FORCE_INSTALL        - Skip confirmation prompts
#   SCOPES_VERBOSE              - Enable verbose output

set -Eeuo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Script directory (where the offline package is extracted)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$SCRIPT_DIR"

# Configuration from environment or defaults
INSTALL_DIR="${SCOPES_INSTALL_DIR:-/usr/local/bin}"
SKIP_VERIFICATION="${SCOPES_SKIP_VERIFICATION:-false}"
FORCE_INSTALL="${SCOPES_FORCE_INSTALL:-false}"
VERBOSE="${SCOPES_VERBOSE:-false}"

# Internal variables
BINARY_NAME=""
BINARY_PATH=""
HASH_FILE=""
PROVENANCE_FILE="$PACKAGE_ROOT/verification/multiple.intoto.jsonl"
SLSA_VERIFIER=""

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
Scopes Offline Installation Script

This script installs Scopes from a pre-downloaded offline package with
integrated security verification.

USAGE:
    $0 [OPTIONS]

OPTIONS:
    -d, --install-dir DIR     Installation directory (default: /usr/local/bin)
    -f, --force              Skip confirmation prompts
    -s, --skip-verification  Skip security verification (not recommended)
    -v, --verbose            Enable verbose output
    -h, --help               Show this help message

ENVIRONMENT VARIABLES:
    SCOPES_INSTALL_DIR          Installation directory
    SCOPES_SKIP_VERIFICATION    Skip SLSA verification (not recommended)
    SCOPES_FORCE_INSTALL        Skip confirmation prompts
    SCOPES_VERBOSE              Enable verbose output

EXAMPLES:
    # Standard installation
    ./install.sh

    # Custom installation directory
    ./install.sh --install-dir /opt/scopes/bin

    # Automated installation (no prompts)
    ./install.sh --force

    # Installation with verbose output
    ./install.sh --verbose

For more information, see README.md or docs/INSTALL.md
EOF
}

# Function to parse command line arguments
parse_args() {
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
            -s|--skip-verification)
                SKIP_VERIFICATION="true"
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
}

# Function to detect platform
detect_platform() {
    case "$(uname -s)" in
        Darwin*)    echo "darwin" ;;
        Linux*)     echo "linux" ;;
        CYGWIN*|MINGW*|MSYS*) echo "win32" ;;
        *)          echo "unknown" ;;
    esac
}

# Function to detect architecture
detect_arch() {
    case "$(uname -m)" in
        x86_64|amd64) echo "x64" ;;
        arm64|aarch64) echo "arm64" ;;
        *) echo "unknown" ;;
    esac
}

# Function to check if running as root
is_root() {
    [[ $EUID -eq 0 ]]
}

# Function to determine if sudo is needed
needs_sudo() {
    if is_root; then
        return 1  # Don't need sudo if already root
    fi

    if [[ ! -w "$INSTALL_DIR" ]]; then
        return 0  # Need sudo if directory is not writable
    fi

    return 1  # Don't need sudo
}

# Function to validate package structure
validate_package() {
    print_status "Validating offline package structure..."

    local required_dirs=("binaries" "verification" "sbom" "docs" "tools")
    for dir in "${required_dirs[@]}"; do
        if [[ ! -d "$PACKAGE_ROOT/$dir" ]]; then
            print_error "Missing required directory: $dir"
            print_error "This does not appear to be a valid Scopes offline package"
            exit 1
        fi
        print_verbose "Found directory: $dir"
    done

    # Check for README
    if [[ ! -f "$PACKAGE_ROOT/README.md" ]]; then
        print_error "Missing README.md - invalid package structure"
        exit 1
    fi

    print_status "✅ Package structure validation passed"
}

# Function to find version from binaries
detect_version() {
    local binary_file
    binary_file=$(find "$PACKAGE_ROOT/binaries" -name "scopes-v*" -type f | head -1)

    if [[ -n "$binary_file" ]]; then
        local filename
        filename=$(basename "$binary_file")
        # Extract version from filename like "scopes-v1.0.0-linux-x64" or "scopes-v0.0.0-test20250906140650-linux-x64"
        echo "$filename" | sed -n 's/scopes-\(v[0-9][^-]*\(-[^-]*\)*\)-[^-]*-[^-]*/\1/p'
    else
        print_error "Cannot detect version from binaries"
        exit 1
    fi
}

# Function to calculate hash
calculate_hash() {
    local file="$1"
    local platform
    platform=$(detect_platform)

    if [[ "$platform" == "darwin" ]]; then
        shasum -a 256 "$file" | awk '{print $1}'
    else
        sha256sum "$file" | awk '{print $1}'
    fi
}

# Function to verify hash
verify_hash() {
    local binary_file="$1"
    local hash_file="$2"

    print_status "Verifying binary hash..."

    if [[ ! -f "$binary_file" ]]; then
        print_error "Binary file not found: $binary_file"
        return 1
    fi

    if [[ ! -f "$hash_file" ]]; then
        print_error "Hash file not found: $hash_file"
        return 1
    fi

    local calculated_hash
    calculated_hash=$(calculate_hash "$binary_file")

    local hash_file_content
    hash_file_content=$(cat "$hash_file" 2>/dev/null || echo "")
    print_verbose "Hash file content: '$hash_file_content'"

    local expected_hash=""
    if [[ "$hash_file_content" == *":"* ]]; then
        # Format: "filename:hash" - extract hash part
        expected_hash=$(echo "$hash_file_content" | cut -d':' -f2 | tr -d ' \r\n')
    else
        # Format: just hash or other format - take the first word that looks like a hash
        expected_hash=$(echo "$hash_file_content" | grep -oE '[a-f0-9]{64}' | head -n1 | tr -d ' \r\n')
    fi

    print_verbose "Expected hash: '$expected_hash'"
    print_verbose "Calculated hash: '$calculated_hash'"

    if [[ -z "$expected_hash" ]]; then
        print_error "❌ Hash verification FAILED"
        print_error "Could not extract hash from file: $hash_file"
        return 1
    fi

    if [[ "$calculated_hash" == "$expected_hash" ]]; then
        print_status "✅ Hash verification PASSED"
        return 0
    else
        print_error "❌ Hash verification FAILED"
        print_error "Expected: $expected_hash"
        print_error "Got: $calculated_hash"
        return 1
    fi
}

# Function to find SLSA verifier
find_slsa_verifier() {
    local platform="$1"
    local arch="$2"

    local verifier_name="slsa-verifier-$platform-$arch"
    if [[ "$platform" == "win32" ]]; then
        verifier_name="$verifier_name.exe"
    fi

    local verifier_path="$PACKAGE_ROOT/tools/$verifier_name"

    if [[ -f "$verifier_path" && -x "$verifier_path" ]]; then
        echo "$verifier_path"
    else
        echo ""
    fi
}

# Function to verify SLSA provenance
verify_slsa() {
    local binary_file="$1"
    local provenance_file="$2"
    local platform="$3"
    local arch="$4"

    print_status "Verifying SLSA provenance..."

    # Find offline SLSA verifier
    SLSA_VERIFIER=$(find_slsa_verifier "$platform" "$arch")

    if [[ -n "$SLSA_VERIFIER" ]]; then
        print_status "Using offline SLSA verifier: $SLSA_VERIFIER"

        if "$SLSA_VERIFIER" verify-artifact "$binary_file" \
            --provenance-path "$provenance_file" \
            --source-uri "github.com/kamiazya/scopes"; then
            print_status "✅ SLSA verification PASSED"
            return 0
        else
            print_error "❌ SLSA verification FAILED"
            return 1
        fi
    else
        # Try system slsa-verifier if available
        if command -v slsa-verifier >/dev/null 2>&1; then
            print_status "Using system SLSA verifier"
            if slsa-verifier verify-artifact "$binary_file" \
                --provenance-path "$provenance_file" \
                --source-uri "github.com/kamiazya/scopes"; then
                print_status "✅ SLSA verification PASSED"
                return 0
            else
                print_error "❌ SLSA verification FAILED"
                return 1
            fi
        else
            print_warning "SLSA verifier not available. Skipping SLSA verification."
            print_warning "For complete security, ensure slsa-verifier is available."
            return 0
        fi
    fi
}

# Function to setup binary paths
setup_binary_paths() {
    local version="$1"
    local platform="$2"
    local arch="$3"

    BINARY_NAME="scopes-$version-$platform-$arch"
    if [[ "$platform" == "win32" ]]; then
        BINARY_NAME="$BINARY_NAME.exe"
    fi

    BINARY_PATH="$PACKAGE_ROOT/binaries/$BINARY_NAME"
    HASH_FILE="$PACKAGE_ROOT/verification/binary-hash-$platform-$arch.txt"

    print_verbose "Binary path: $BINARY_PATH"
    print_verbose "Hash file: $HASH_FILE"

    # Check if binary exists
    if [[ ! -f "$BINARY_PATH" ]]; then
        print_error "Binary not found: $BINARY_PATH"
        print_error "Available binaries:"
        ls -la "$PACKAGE_ROOT/binaries/" || true
        exit 1
    fi

    # Check if hash file exists
    if [[ ! -f "$HASH_FILE" ]]; then
        print_error "Hash file not found: $HASH_FILE"
        exit 1
    fi
}

# Function to perform verification
perform_verification() {
    local platform="$1"
    local arch="$2"

    if [[ "$SKIP_VERIFICATION" == "true" ]]; then
        print_warning "⚠️  All verification (hash + SLSA) skipped by user request"
        return 0
    fi

    print_header "Security Verification"

    local verification_failed=false

    # Hash verification (mandatory)
    if ! verify_hash "$BINARY_PATH" "$HASH_FILE"; then
        verification_failed=true
    fi

    # SLSA verification (if provenance file exists)
    if [[ -f "$PROVENANCE_FILE" ]]; then
        if ! verify_slsa "$BINARY_PATH" "$PROVENANCE_FILE" "$platform" "$arch"; then
            verification_failed=true
        fi
    else
        print_warning "Provenance file not available, skipping SLSA verification"
    fi

    if [[ "$verification_failed" == "true" ]]; then
        print_error "❌ Security verification FAILED!"
        print_error "DO NOT install this binary - it may be compromised."
        exit 1
    fi

    print_status "🎉 All security verifications PASSED!"
}

# Function to install binary
install_binary() {
    local source="$1"
    local dest_dir="$2"
    local binary_name="scopes"

    print_header "Installing Scopes"

    # Create installation directory if it doesn't exist
    if [[ ! -d "$dest_dir" ]]; then
        print_status "Creating installation directory: $dest_dir"
        if needs_sudo; then
            sudo mkdir -p "$dest_dir"
        else
            mkdir -p "$dest_dir"
        fi
    fi

    local dest_path="$dest_dir/$binary_name"

    # Copy binary to destination
    print_status "Installing to: $dest_path"
    if needs_sudo; then
        print_status "Using sudo for installation..."
        sudo cp "$source" "$dest_path"
        sudo chmod +x "$dest_path"
    else
        cp "$source" "$dest_path"
        chmod +x "$dest_path"
    fi

    print_status "✅ Installation completed successfully!"
}

# Function to verify installation
verify_installation() {
    print_header "Verifying Installation"

    local binary_path="$INSTALL_DIR/scopes"

    if [[ ! -f "$binary_path" ]]; then
        print_error "Installation verification failed: binary not found at $binary_path"
        return 1
    fi

    if [[ ! -x "$binary_path" ]]; then
        print_error "Installation verification failed: binary is not executable"
        return 1
    fi

    # Test if binary is in PATH
    if command -v scopes >/dev/null 2>&1; then
        print_status "✅ Scopes is installed and available in PATH"

        # Try to get version
        local version_output
        if version_output=$(scopes --version 2>/dev/null); then
            print_status "Version: $version_output"
        else
            print_warning "Could not get version information (this may be normal)"
        fi
    else
        print_warning "Scopes is installed but not in PATH"
        print_status "Add $INSTALL_DIR to your PATH to use 'scopes' command globally"
        print_status "You can run: export PATH=\"$INSTALL_DIR:\$PATH\""
    fi

    print_status "🎉 Installation verification completed!"
}

# Function to show next steps
show_next_steps() {
    local version="$1"

    print_header "Next Steps"

    echo -e "${BOLD}Scopes $version has been successfully installed offline!${NC}"
    echo ""
    echo "Quick start:"
    echo "  scopes --help                 # Show help"
    echo "  scopes --version              # Show version"
    echo ""

    if ! command -v scopes >/dev/null 2>&1; then
        echo -e "${YELLOW}Note:${NC} Scopes is not in your PATH. To use it globally:"
        echo "  export PATH=\"$INSTALL_DIR:\$PATH\""
        echo "  echo 'export PATH=\"$INSTALL_DIR:\$PATH\"' >> ~/.bashrc  # Make permanent"
        echo ""
    fi

    echo "Documentation (included in this package):"
    echo "  docs/INSTALL.md               # Detailed installation guide"
    echo "  docs/SECURITY.md              # Security verification guide"
    echo "  docs/ENTERPRISE.md            # Enterprise deployment guide"
    echo ""
    echo "SBOM files are available in the sbom/ directory for compliance requirements."
    echo ""
    echo "Security verification:"
    echo "  All artifacts were cryptographically verified offline"
    echo "  SLSA Level 3 provenance ensures supply chain integrity"
}

# Function to confirm installation
confirm_installation() {
    local version="$1"
    local platform="$2"
    local arch="$3"

    if [[ "$FORCE_INSTALL" == "true" ]]; then
        return 0
    fi

    echo -e "${BOLD}Scopes Offline Installation${NC}"
    echo ""
    echo "This script will:"
    echo "  • Install Scopes $version for $platform-$arch from offline package"
    echo "  • Verify cryptographic signatures and hashes (offline)"
    echo "  • Install to: $INSTALL_DIR"
    if needs_sudo; then
        echo "  • Require sudo privileges for installation"
    fi
    echo ""

    read -p "Continue with installation? [y/N] " -n 1 -r
    echo

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Installation cancelled by user"
        exit 0
    fi
}

# Main function
main() {
    # Parse command line arguments
    parse_args "$@"

    print_header "Scopes Offline Installer"

    # Validate package structure
    validate_package

    # Detect platform and architecture
    local platform
    local arch
    platform=$(detect_platform)
    arch=$(detect_arch)

    if [[ "$platform" == "unknown" || "$arch" == "unknown" ]]; then
        print_error "Unsupported platform: $platform-$arch"
        print_error "Supported platforms: linux-x64, linux-arm64, darwin-x64, darwin-arm64"
        exit 1
    fi

    # Detect version from package
    local version
    version=$(detect_version)
    print_status "Detected version: $version"

    # Setup binary paths
    setup_binary_paths "$version" "$platform" "$arch"

    # Show configuration
    print_status "Configuration:"
    print_status "  Package root: $PACKAGE_ROOT"
    print_status "  Platform: $platform-$arch"
    print_status "  Version: $version"
    print_status "  Install directory: $INSTALL_DIR"
    print_status "  Skip verification: $SKIP_VERIFICATION"

    # Check if sudo is needed and available
    if needs_sudo; then
        if ! command -v sudo >/dev/null 2>&1; then
            print_error "Installation requires write access to $INSTALL_DIR"
            print_error "Please run as root or use --install-dir to specify a writable location"
            exit 1
        fi
    fi

    # Confirm installation
    confirm_installation "$version" "$platform" "$arch"

    # Perform verification
    perform_verification "$platform" "$arch"

    # Install
    install_binary "$BINARY_PATH" "$INSTALL_DIR"
    verify_installation

    # Show next steps
    show_next_steps "$version"
}

# Run main function
main "$@"
