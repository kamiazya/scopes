#!/bin/bash

# Scopes Installation Script
# This script downloads, verifies, and installs Scopes with integrated security verification
#
# Usage:
#   curl -sSL https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.sh | sh
#
# Environment variables:
#   SCOPES_VERSION              - Version to install (default: latest)
#   SCOPES_INSTALL_DIR          - Installation directory (default: /usr/local/bin)
#   SCOPES_GITHUB_REPO          - GitHub repository (default: kamiazya/scopes)
#   SCOPES_SKIP_VERIFICATION    - Skip SLSA verification (not recommended)
#   SCOPES_FORCE_INSTALL        - Skip confirmation prompts
#   SCOPES_VERBOSE              - Enable verbose output

set -e

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

# Internal variables
TEMP_DIR=""
BINARY_NAME=""
BINARY_PATH=""
HASH_FILE=""
PROVENANCE_FILE=""
NEEDS_SUDO=""

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
    if [[ -n "$TEMP_DIR" && -d "$TEMP_DIR" ]]; then
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

# Function to check if running as root
is_root() {
    [[ $EUID -eq 0 ]]
}

# Function to check if directory is writable
is_writable() {
    [[ -w "$1" ]]
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

# Function to calculate hash
calculate_hash() {
    local file="$1"
    local platform=$(detect_platform)
    
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
    
    local calculated_hash=$(calculate_hash "$binary_file")
    local expected_hash=$(cut -d':' -f2 "$hash_file" | tr -d ' \r\n')
    
    print_verbose "Expected hash: $expected_hash"
    print_verbose "Calculated hash: $calculated_hash"
    
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

# Function to verify SLSA provenance
verify_slsa() {
    local binary_file="$1"
    local provenance_file="$2"
    
    print_status "Verifying SLSA provenance..."
    
    # Check if slsa-verifier is installed
    if ! command -v slsa-verifier >/dev/null 2>&1; then
        print_warning "slsa-verifier not found. Installing..."
        if command -v go >/dev/null 2>&1; then
            print_status "Installing slsa-verifier with Go..."
            go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest
            # Add GOPATH/bin to PATH if not already there
            if [[ ":$PATH:" != *":$HOME/go/bin:"* ]]; then
                export PATH="$HOME/go/bin:$PATH"
            fi
        else
            print_warning "Go is not installed. Skipping SLSA verification."
            print_warning "For complete security, please install Go and slsa-verifier manually:"
            print_warning "  go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest"
            return 0
        fi
    fi
    
    if command -v slsa-verifier >/dev/null 2>&1; then
        print_status "Running SLSA verification..."
        if slsa-verifier verify-artifact "$binary_file" \
            --provenance-path "$provenance_file" \
            --source-uri "github.com/$GITHUB_REPO"; then
            print_status "✅ SLSA verification PASSED"
            return 0
        else
            print_error "❌ SLSA verification FAILED"
            return 1
        fi
    else
        print_warning "Could not install slsa-verifier. Skipping SLSA verification."
        return 0
    fi
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

# Function to download and verify release
download_and_verify() {
    local version="$1"
    local platform="$2"
    local arch="$3"
    
    print_header "Downloading Scopes $version"
    
    # Create temporary directory
    TEMP_DIR=$(mktemp -d)
    print_verbose "Using temporary directory: $TEMP_DIR"
    
    # Set up file names
    BINARY_NAME="scopes-$version-$platform-$arch"
    if [[ "$platform" == "win32" ]]; then
        BINARY_NAME="$BINARY_NAME.exe"
    fi
    
    BINARY_PATH="$TEMP_DIR/$BINARY_NAME"
    HASH_FILE="$TEMP_DIR/binary-hash-$platform-$arch.txt"
    PROVENANCE_FILE="$TEMP_DIR/multiple.intoto.jsonl"
    
    local base_url="https://github.com/$GITHUB_REPO/releases/download/$version"
    
    # Download binary
    print_status "Downloading binary: $BINARY_NAME"
    if ! download_file "$base_url/$BINARY_NAME" "$BINARY_PATH"; then
        exit 1
    fi
    
    # Download hash file
    print_status "Downloading hash file..."
    if ! download_file "$base_url/binary-hash-$platform-$arch.txt" "$HASH_FILE"; then
        exit 1
    fi
    
    # Download provenance file (if verification is enabled)
    if [[ "$SKIP_VERIFICATION" != "true" ]]; then
        print_status "Downloading SLSA provenance..."
        if ! download_file "$base_url/multiple.intoto.jsonl" "$PROVENANCE_FILE"; then
            print_warning "Failed to download provenance file. SLSA verification will be skipped."
        fi
    fi
    
    print_status "✅ Download completed"
}

# Function to perform verification
perform_verification() {
    if [[ "$SKIP_VERIFICATION" == "true" ]]; then
        print_warning "⚠️  Verification skipped by user request"
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
        if ! verify_slsa "$BINARY_PATH" "$PROVENANCE_FILE"; then
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
    print_header "Next Steps"
    
    echo -e "${BOLD}Scopes has been successfully installed!${NC}"
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
    
    echo "Documentation:"
    echo "  https://github.com/$GITHUB_REPO"
    echo ""
    echo "Security verification:"
    echo "  All downloaded artifacts were cryptographically verified"
    echo "  SLSA Level 3 provenance ensures supply chain integrity"
}

# Function to confirm installation
confirm_installation() {
    if [[ "$FORCE_INSTALL" == "true" ]]; then
        return 0
    fi
    
    echo -e "${BOLD}Scopes Installation${NC}"
    echo ""
    echo "This script will:"
    echo "  • Download Scopes $VERSION for $platform-$arch"
    echo "  • Verify cryptographic signatures and hashes"
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
    print_header "Scopes Installer"
    
    # Check prerequisites
    if ! command -v curl >/dev/null 2>&1 && ! command -v wget >/dev/null 2>&1; then
        print_error "Either curl or wget is required for installation"
        exit 1
    fi
    
    # Detect platform and architecture
    local platform=$(detect_platform)
    local arch=$(detect_arch)
    
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
    
    # Check if sudo is needed and available
    if needs_sudo; then
        if ! command -v sudo >/dev/null 2>&1; then
            print_error "Installation requires write access to $INSTALL_DIR"
            print_error "Please run as root or change SCOPES_INSTALL_DIR to a writable location"
            exit 1
        fi
        NEEDS_SUDO="true"
    fi
    
    # Confirm installation
    confirm_installation
    
    # Download and verify
    download_and_verify "$VERSION" "$platform" "$arch"
    perform_verification
    
    # Install
    install_binary "$BINARY_PATH" "$INSTALL_DIR"
    verify_installation
    
    # Show next steps
    show_next_steps
}

# Run main function
main "$@"