#!/bin/bash

# Scopes Unified Installation Script
# This script downloads and installs Scopes using the unified offline package
#
# Usage:
#   curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install-unified.sh | sh
#
# Environment variables:
#   SCOPES_VERSION              - Version to install (default: latest)
#   SCOPES_INSTALL_DIR          - Installation directory (default: /usr/local/bin)
#   SCOPES_GITHUB_REPO          - GitHub repository (default: kamiazya/scopes)
#   SCOPES_SKIP_VERIFICATION    - Skip SLSA verification (not recommended)
#   SCOPES_FORCE_INSTALL        - Skip confirmation prompts
#   SCOPES_VERBOSE              - Enable verbose output
#   SCOPES_KEEP_PACKAGE         - Keep downloaded package after installation

set -Eeuo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Configuration from environment or defaults
VERSION="${SCOPES_VERSION:-}"
INSTALL_DIR="${SCOPES_INSTALL_DIR:-/usr/local/bin}"
GITHUB_REPO="${SCOPES_GITHUB_REPO:-kamiazya/scopes}"
SKIP_VERIFICATION="${SCOPES_SKIP_VERIFICATION:-false}"
FORCE_INSTALL="${SCOPES_FORCE_INSTALL:-false}"
VERBOSE="${SCOPES_VERBOSE:-false}"
KEEP_PACKAGE="${SCOPES_KEEP_PACKAGE:-false}"

# Internal variables
TEMP_DIR=""
PACKAGE_FILE=""
PACKAGE_DIR=""

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

# Function to clean up temporary files
cleanup() {
    if [[ "$KEEP_PACKAGE" == "true" ]]; then
        print_status "Keeping package files in: $TEMP_DIR"
        print_status "You can manually install later using: $PACKAGE_DIR/install.sh"
    elif [[ -n "$TEMP_DIR" && -d "$TEMP_DIR" ]]; then
        print_verbose "Cleaning up temporary directory: $TEMP_DIR"
        rm -rf "$TEMP_DIR"
    fi
}

# Set up cleanup trap
trap cleanup EXIT INT TERM

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

# Function to get latest version from GitHub
get_latest_version() {
    print_status "Fetching latest version from GitHub..."
    local api_url="https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    local latest_version

    if command -v curl >/dev/null 2>&1; then
        latest_version=$(curl -sSL "$api_url" | grep '"tag_name"' | cut -d'"' -f4)
    elif command -v wget >/dev/null 2>&1; then
        latest_version=$(wget -qO- "$api_url" | grep '"tag_name"' | cut -d'"' -f4)
    else
        print_error "Neither curl nor wget is available"
        exit 1
    fi

    if [[ -z "$latest_version" ]]; then
        print_error "Failed to fetch latest version"
        exit 1
    fi

    echo "$latest_version"
}

# Function to download file
download_file() {
    local url="$1"
    local output="$2"

    print_verbose "Downloading: $url"
    print_verbose "Output: $output"

    if command -v curl >/dev/null 2>&1; then
        if ! curl -sSL -o "$output" "$url"; then
            print_error "Failed to download: $url"
            return 1
        fi
    elif command -v wget >/dev/null 2>&1; then
        if ! wget -q -O "$output" "$url"; then
            print_error "Failed to download: $url"
            return 1
        fi
    else
        print_error "Neither curl nor wget is available"
        return 1
    fi

    print_verbose "Successfully downloaded: $output"
}

# Function to verify package checksum
verify_package_checksum() {
    local package_file="$1"
    local checksum_file="$2"

    print_status "Verifying package checksum..."

    if [[ ! -f "$checksum_file" ]]; then
        print_warning "Checksum file not found, skipping package checksum verification"
        return 0
    fi

    local expected_checksum
    expected_checksum=$(cut -d' ' -f1 "$checksum_file")

    local calculated_checksum
    if [[ "$(detect_platform)" == "darwin" ]]; then
        calculated_checksum=$(shasum -a 256 "$package_file" | cut -d' ' -f1)
    else
        calculated_checksum=$(sha256sum "$package_file" | cut -d' ' -f1)
    fi

    if [[ "$calculated_checksum" == "$expected_checksum" ]]; then
        print_status "✅ Package checksum verification PASSED"
        return 0
    else
        print_error "❌ Package checksum verification FAILED"
        print_error "Expected: $expected_checksum"
        print_error "Got: $calculated_checksum"
        return 1
    fi
}

# Function to download and extract unified package
download_unified_package() {
    local version="$1"

    print_header "Downloading Scopes Unified Package"

    # Create temporary directory
    TEMP_DIR=$(mktemp -d)
    print_verbose "Using temporary directory: $TEMP_DIR"

    # Determine package name
    local package_name="scopes-${version}-offline.tar.gz"
    local checksum_name="${package_name}.sha256"

    PACKAGE_FILE="$TEMP_DIR/$package_name"
    local checksum_file="$TEMP_DIR/$checksum_name"

    local base_url="https://github.com/$GITHUB_REPO/releases/download/$version"

    # Download unified package
    print_status "Downloading unified package: $package_name"
    if ! download_file "$base_url/$package_name" "$PACKAGE_FILE"; then
        # Fallback: Try .zip for older releases or Windows preference
        package_name="scopes-${version}-offline.zip"
        PACKAGE_FILE="$TEMP_DIR/$package_name"
        print_status "Trying alternative format: $package_name"
        if ! download_file "$base_url/$package_name" "$PACKAGE_FILE"; then
            print_error "Failed to download unified package"
            exit 1
        fi
    fi

    # Download checksum
    print_status "Downloading checksum file..."
    if download_file "$base_url/$checksum_name" "$checksum_file"; then
        verify_package_checksum "$PACKAGE_FILE" "$checksum_file"
    else
        print_warning "Checksum file not available, skipping package verification"
    fi

    # Extract package
    print_status "Extracting unified package..."
    cd "$TEMP_DIR"

    if [[ "$package_name" == *.tar.gz ]]; then
        tar -xzf "$PACKAGE_FILE"
    elif [[ "$package_name" == *.zip ]]; then
        unzip -q "$PACKAGE_FILE"
    else
        print_error "Unknown package format: $package_name"
        exit 1
    fi

    # Find extracted directory
    PACKAGE_DIR=$(find . -maxdepth 1 -type d -name "scopes-*-offline" | head -1)
    if [[ -z "$PACKAGE_DIR" ]]; then
        print_error "Failed to find extracted package directory"
        exit 1
    fi

    PACKAGE_DIR="$TEMP_DIR/$(basename "$PACKAGE_DIR")"
    print_verbose "Package extracted to: $PACKAGE_DIR"

    print_status "✅ Package download and extraction completed"
}

# Function to run offline installer
run_offline_installer() {
    print_header "Running Offline Installer"

    cd "$PACKAGE_DIR"

    # Check if offline installer exists
    if [[ ! -f "install.sh" ]]; then
        print_error "Offline installer not found in package"
        exit 1
    fi

    # Make installer executable
    chmod +x install.sh

    # Prepare installer arguments
    local installer_args=""

    if [[ -n "$INSTALL_DIR" ]]; then
        installer_args="$installer_args --install-dir $INSTALL_DIR"
    fi

    if [[ "$SKIP_VERIFICATION" == "true" ]]; then
        installer_args="$installer_args --skip-verification"
    fi

    if [[ "$FORCE_INSTALL" == "true" ]]; then
        installer_args="$installer_args --force"
    fi

    if [[ "$VERBOSE" == "true" ]]; then
        installer_args="$installer_args --verbose"
    fi

    print_verbose "Running: ./install.sh $installer_args"

    # Run the offline installer
    if [[ -n "$installer_args" ]]; then
        ./install.sh $installer_args
    else
        ./install.sh
    fi
}

# Function to check prerequisites
check_prerequisites() {
    local missing_tools=()

    if ! command -v curl >/dev/null 2>&1 && ! command -v wget >/dev/null 2>&1; then
        missing_tools+=("curl or wget")
    fi

    if ! command -v tar >/dev/null 2>&1; then
        missing_tools+=("tar")
    fi

    if ! command -v gzip >/dev/null 2>&1; then
        missing_tools+=("gzip")
    fi

    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        print_error "Missing required tools: ${missing_tools[*]}"
        print_error "Please install these tools and try again"
        exit 1
    fi
}

# Main function
main() {
    print_header "Scopes Unified Installer"

    # Check prerequisites
    check_prerequisites

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

    # Get version to install
    if [[ -z "$VERSION" ]]; then
        VERSION=$(get_latest_version)
        print_status "Latest version: $VERSION"
    else
        print_status "Installing specified version: $VERSION"
    fi

    # Show configuration
    print_status "Configuration:"
    print_status "  Platform: $platform-$arch"
    print_status "  Version: $VERSION"
    print_status "  Repository: $GITHUB_REPO"
    print_status "  Install directory: $INSTALL_DIR"
    print_status "  Skip verification: $SKIP_VERIFICATION"
    print_status "  Keep package: $KEEP_PACKAGE"

    # Download and extract unified package
    download_unified_package "$VERSION"

    # Run offline installer from extracted package
    run_offline_installer

    print_header "Installation Complete"

    if [[ "$KEEP_PACKAGE" == "true" ]]; then
        print_status "Package files kept in: $PACKAGE_DIR"
        print_status "You can reinstall anytime using: $PACKAGE_DIR/install.sh"
    fi

    print_status "🎉 Scopes has been successfully installed!"
}

# Run main function
main "$@"
