#!/bin/bash

# Cross-platform verification script for Scopes releases
# Supports: Linux, macOS, Windows (Git Bash/WSL)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values (can be overridden by environment variables)
VERSION="${SCOPES_VERSION:-}"
BINARY_PATH="${SCOPES_BINARY_PATH:-}"
PROVENANCE_PATH="${SCOPES_PROVENANCE_PATH:-}"
HASH_FILE="${SCOPES_HASH_FILE:-}"
VERIFY_SLSA="${SCOPES_VERIFY_SLSA:-true}"
VERIFY_HASH="${SCOPES_VERIFY_HASH:-true}"
VERIFY_SBOM="${SCOPES_VERIFY_SBOM:-false}"
AUTO_DOWNLOAD="${SCOPES_AUTO_DOWNLOAD:-false}"
GITHUB_REPO="${SCOPES_GITHUB_REPO:-kamiazya/scopes}"
PLATFORM_OVERRIDE="${SCOPES_PLATFORM:-}"
ARCH_OVERRIDE="${SCOPES_ARCH:-}"

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
    echo -e "${BLUE}=== $1 ===${NC}"
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

# Function to calculate hash cross-platform
calculate_hash() {
    local file="$1"
    local platform=$(detect_platform)
    
    if [[ "$platform" == "win32" ]]; then
        # Windows: Use certutil if available, otherwise try sha256sum
        if command -v certutil >/dev/null 2>&1; then
            certutil -hashfile "$file" SHA256 | grep -v "hash" | grep -v "CertUtil" | tr -d ' \r\n' | tr '[:upper:]' '[:lower:]'
        elif command -v sha256sum >/dev/null 2>&1; then
            sha256sum "$file" | awk '{print $1}'
        else
            print_error "No hash calculation tool available"
            exit 1
        fi
    elif [[ "$platform" == "darwin" ]]; then
        # macOS: Use shasum
        shasum -a 256 "$file" | awk '{print $1}'
    else
        # Linux: Use sha256sum
        sha256sum "$file" | awk '{print $1}'
    fi
}

# Function to verify hash
verify_hash() {
    local binary_file="$1"
    local hash_file="$2"
    
    print_header "Hash Verification"
    
    if [[ ! -f "$binary_file" ]]; then
        print_error "Binary file not found: $binary_file"
        return 1
    fi
    
    if [[ ! -f "$hash_file" ]]; then
        print_error "Hash file not found: $hash_file"
        return 1
    fi
    
    print_status "Calculating hash for: $binary_file"
    local calculated_hash
    calculated_hash=$(calculate_hash "$binary_file")
    
    print_status "Reading expected hash from: $hash_file"
    local expected_hash
    expected_hash=$(cut -d':' -f2 "$hash_file" | tr -d ' \r\n')
    
    print_status "Expected hash: $expected_hash"
    print_status "Calculated hash: $calculated_hash"
    
    if [[ "$calculated_hash" == "$expected_hash" ]]; then
        print_status "✅ Hash verification PASSED"
        return 0
    else
        print_error "❌ Hash verification FAILED"
        print_error "Hashes do not match!"
        return 1
    fi
}

# Function to verify SLSA provenance
verify_slsa() {
    local binary_file="$1"
    local provenance_file="$2"
    
    print_header "SLSA Provenance Verification"
    
    if [[ ! -f "$binary_file" ]]; then
        print_error "Binary file not found: $binary_file"
        return 1
    fi
    
    if [[ ! -f "$provenance_file" ]]; then
        print_error "Provenance file not found: $provenance_file"
        return 1
    fi
    
    # Check if slsa-verifier is installed
    if ! command -v slsa-verifier >/dev/null 2>&1; then
        print_warning "slsa-verifier not found. Installing..."
        if command -v go >/dev/null 2>&1; then
            go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest
        else
            print_error "Go is required to install slsa-verifier"
            print_error "Please install Go and run: go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest"
            return 1
        fi
    fi
    
    print_status "Verifying SLSA provenance..."
    if slsa-verifier verify-artifact "$binary_file" \
        --provenance-path "$provenance_file" \
        --source-uri "github.com/$GITHUB_REPO"; then
        print_status "✅ SLSA verification PASSED"
        return 0
    else
        print_error "❌ SLSA verification FAILED"
        return 1
    fi
}

# Function to download release files
download_release() {
    local version="$1"
    local platform="$2"
    local arch="$3"
    
    print_header "Auto-downloading release files"
    
    local base_url="https://github.com/$GITHUB_REPO/releases/download/$version"
    local binary_name="scopes-$version-$platform-$arch"
    local hash_name="binary-hash-$platform-$arch.txt"
    
    if [[ "$platform" == "win32" ]]; then
        binary_name="$binary_name.exe"
    fi
    
    print_status "Downloading binary: $binary_name"
    curl -L -o "$binary_name" "$base_url/$binary_name" || {
        print_error "Failed to download binary"
        exit 1
    }
    
    print_status "Downloading hash file: $hash_name"
    curl -L -o "$hash_name" "$base_url/$hash_name" || {
        print_error "Failed to download hash file"
        exit 1
    }
    
    print_status "Downloading provenance file: multiple.intoto.jsonl"
    curl -L -o "multiple.intoto.jsonl" "$base_url/multiple.intoto.jsonl" || {
        print_error "Failed to download provenance file"
        exit 1
    }
    
    # Set global variables
    BINARY_PATH="$binary_name"
    HASH_FILE="$hash_name"
    PROVENANCE_PATH="multiple.intoto.jsonl"
    
    print_status "✅ Download completed"
}

# Function to verify SBOM
verify_sbom() {
    local version="$1"
    local platform="$2"
    local arch="$3"
    
    print_header "SBOM Verification"
    
    local sbom_json="sbom-$platform-$arch.json"
    local sbom_xml="sbom-$platform-$arch.xml"
    
    # Try to download SBOM files if not present
    if [[ ! -f "$sbom_json" ]] && [[ "$AUTO_DOWNLOAD" == "true" ]]; then
        print_status "Downloading SBOM files..."
        local base_url="https://github.com/$GITHUB_REPO/releases/download/$version"
        curl -L -o "$sbom_json" "$base_url/$sbom_json" || print_warning "Failed to download $sbom_json"
        curl -L -o "$sbom_xml" "$base_url/$sbom_xml" || print_warning "Failed to download $sbom_xml"
    fi
    
    if [[ -f "$sbom_json" ]]; then
        print_status "Verifying SBOM JSON format..."
        if command -v cyclonedx >/dev/null 2>&1; then
            if cyclonedx validate "$sbom_json"; then
                print_status "✅ SBOM JSON validation PASSED"
            else
                print_error "❌ SBOM JSON validation FAILED"
            fi
        else
            print_warning "CycloneDX CLI not found, skipping SBOM validation"
        fi
        
        # Verify SBOM hash if hash file contains it
        if [[ -f "$HASH_FILE" ]] && grep -q "$sbom_json" "$HASH_FILE"; then
            print_status "Verifying SBOM hash..."
            local sbom_hash_entry=$(grep "$sbom_json" "$HASH_FILE")
            local expected_sbom_hash=$(echo "$sbom_hash_entry" | cut -d':' -f2 | tr -d ' \r\n')
            local calculated_sbom_hash=$(calculate_hash "$sbom_json")
            
            if [[ "$calculated_sbom_hash" == "$expected_sbom_hash" ]]; then
                print_status "✅ SBOM hash verification PASSED"
            else
                print_error "❌ SBOM hash verification FAILED"
            fi
        fi
    else
        print_warning "SBOM JSON file not found: $sbom_json"
    fi
}

# Function to show usage
show_usage() {
    cat << EOF
Cross-Platform Scopes Release Verification Script

Usage: $0 [OPTIONS]

OPTIONS:
    -v, --version VERSION       Release version to verify (e.g., v1.0.0)
    -b, --binary PATH          Path to binary file
    -p, --provenance PATH      Path to provenance file
    -h, --hash-file PATH       Path to hash file
    -d, --download             Auto-download release files
    -s, --skip-slsa           Skip SLSA verification
    -k, --skip-hash           Skip hash verification
    --verify-sbom             Also verify SBOM files
    --platform PLATFORM       Override platform detection (linux/darwin/win32)
    --arch ARCH               Override architecture detection (x64/arm64)
    --repo REPO               GitHub repository (default: kamiazya/scopes)
    --help                    Show this help message

ENVIRONMENT VARIABLES:
    SCOPES_VERSION             Default version to verify
    SCOPES_BINARY_PATH         Default binary path
    SCOPES_PROVENANCE_PATH     Default provenance file path
    SCOPES_HASH_FILE           Default hash file path
    SCOPES_VERIFY_SLSA         Enable/disable SLSA verification (true/false)
    SCOPES_VERIFY_HASH         Enable/disable hash verification (true/false)
    SCOPES_VERIFY_SBOM         Enable/disable SBOM verification (true/false)
    SCOPES_AUTO_DOWNLOAD       Enable auto-download (true/false)
    SCOPES_GITHUB_REPO         GitHub repository (owner/repo)
    SCOPES_PLATFORM            Override platform detection
    SCOPES_ARCH                Override architecture detection

EXAMPLES:
    # Using environment variables
    export SCOPES_VERSION=v1.0.0
    export SCOPES_AUTO_DOWNLOAD=true
    $0

    # Auto-download and verify with command line
    $0 --download --version v1.0.0

    # Verify local files
    $0 --binary scopes-v1.0.0-linux-x64 --provenance multiple.intoto.jsonl --hash-file binary-hash-linux-x64.txt

    # Using environment for common settings
    export SCOPES_GITHUB_REPO=your-org/scopes-fork
    export SCOPES_VERIFY_SBOM=true
    $0 --download --version v1.0.0

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -b|--binary)
            BINARY_PATH="$2"
            shift 2
            ;;
        -p|--provenance)
            PROVENANCE_PATH="$2"
            shift 2
            ;;
        -h|--hash-file)
            HASH_FILE="$2"
            shift 2
            ;;
        -d|--download)
            AUTO_DOWNLOAD="true"
            shift
            ;;
        -s|--skip-slsa)
            VERIFY_SLSA="false"
            shift
            ;;
        -k|--skip-hash)
            VERIFY_HASH="false"
            shift
            ;;
        --verify-sbom)
            VERIFY_SBOM="true"
            shift
            ;;
        --platform)
            PLATFORM_OVERRIDE="$2"
            shift 2
            ;;
        --arch)
            ARCH_OVERRIDE="$2"
            shift 2
            ;;
        --repo)
            GITHUB_REPO="$2"
            shift 2
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Main verification logic
main() {
    print_header "Scopes Release Verification"
    
    # Detect platform and architecture
    local platform=${PLATFORM_OVERRIDE:-$(detect_platform)}
    local arch=${ARCH_OVERRIDE:-$(detect_arch)}
    
    # Show current configuration
    print_status "Configuration:"
    print_status "  Version: ${VERSION:-'(not set)'}"
    print_status "  Platform: $platform-$arch"
    print_status "  Repository: $GITHUB_REPO"
    print_status "  Auto-download: $AUTO_DOWNLOAD"
    print_status "  Verify SLSA: $VERIFY_SLSA"
    print_status "  Verify Hash: $VERIFY_HASH"
    print_status "  Verify SBOM: $VERIFY_SBOM"
    echo
    
    if [[ "$platform" == "unknown" ]] || [[ "$arch" == "unknown" ]]; then
        print_error "Unsupported platform or architecture"
        print_error "Please specify --platform and --arch manually"
        exit 1
    fi
    
    # Auto-download if requested
    if [[ "$AUTO_DOWNLOAD" == "true" ]]; then
        if [[ -z "$VERSION" ]]; then
            print_error "Version is required for auto-download"
            exit 1
        fi
        download_release "$VERSION" "$platform" "$arch"
    fi
    
    # Validate required files
    if [[ "$VERIFY_HASH" == "true" ]] && [[ -z "$BINARY_PATH" || -z "$HASH_FILE" ]]; then
        print_error "Binary path and hash file are required for hash verification"
        exit 1
    fi
    
    if [[ "$VERIFY_SLSA" == "true" ]] && [[ -z "$BINARY_PATH" || -z "$PROVENANCE_PATH" ]]; then
        print_error "Binary path and provenance file are required for SLSA verification"
        exit 1
    fi
    
    local overall_result=0
    
    # Perform hash verification
    if [[ "$VERIFY_HASH" == "true" ]]; then
        if ! verify_hash "$BINARY_PATH" "$HASH_FILE"; then
            overall_result=1
        fi
        echo
    fi
    
    # Perform SLSA verification
    if [[ "$VERIFY_SLSA" == "true" ]]; then
        if ! verify_slsa "$BINARY_PATH" "$PROVENANCE_PATH"; then
            overall_result=1
        fi
        echo
    fi
    
    # Perform SBOM verification if requested
    if [[ "$VERIFY_SBOM" == "true" ]]; then
        if [[ -n "$VERSION" ]]; then
            verify_sbom "$VERSION" "$platform" "$arch"
        else
            print_warning "Version required for SBOM verification, skipping"
        fi
        echo
    fi
    
    # Final result
    print_header "Verification Results"
    if [[ $overall_result -eq 0 ]]; then
        print_status "🎉 All verifications PASSED!"
        print_status "The binary is authentic and can be trusted."
    else
        print_error "💥 One or more verifications FAILED!"
        print_error "DO NOT use this binary - it may be compromised."
        exit 1
    fi
}

# Run main function
main "$@"