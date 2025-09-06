#!/bin/bash
# E2E Test Suite for Scopes Native Binary
# This script performs comprehensive testing of the native binary

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get binary path from argument or environment
BINARY_PATH="${1:-$SCOPES_BINARY_PATH}"

if [ -z "$BINARY_PATH" ]; then
    echo -e "${RED}Error: Binary path not provided${NC}"
    echo "Usage: $0 <path-to-binary>"
    exit 1
fi

if [ ! -f "$BINARY_PATH" ]; then
    echo -e "${RED}Error: Binary not found at: $BINARY_PATH${NC}"
    exit 1
fi

# Make binary executable if needed
chmod +x "$BINARY_PATH" 2>/dev/null || true

echo "========================================="
echo "Scopes Native Binary E2E Test Suite"
echo "Binary: $BINARY_PATH"
echo "========================================="

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run a test
run_test() {
    local test_name="$1"
    shift
    local command_args="$@"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -n "Testing: $test_name ... "

    # Create temp file for output
    local output_file=$(mktemp)
    local error_file=$(mktemp)

    # Run the command
    if "$BINARY_PATH" $command_args > "$output_file" 2> "$error_file"; then
        echo -e "${GREEN}✓ PASSED${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))

        # Clean up temp files
        rm -f "$output_file" "$error_file"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))

        # Show error details
        echo -e "${YELLOW}  Command: $BINARY_PATH $command_args${NC}"
        if [ -s "$error_file" ]; then
            echo -e "${YELLOW}  Error output:${NC}"
            cat "$error_file" | sed 's/^/    /'
        fi

        # Clean up temp files
        rm -f "$output_file" "$error_file"
        return 1
    fi
}

# Function to run test expecting specific output
run_test_with_output() {
    local test_name="$1"
    local expected_pattern="$2"
    shift 2
    local command_args="$@"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -n "Testing: $test_name ... "

    # Create temp file for output
    local output_file=$(mktemp)

    # Run the command
    if "$BINARY_PATH" $command_args > "$output_file" 2>&1; then
        # Check if output contains expected pattern
        if grep -q "$expected_pattern" "$output_file"; then
            echo -e "${GREEN}✓ PASSED${NC}"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            rm -f "$output_file"
            return 0
        else
            echo -e "${RED}✗ FAILED (output mismatch)${NC}"
            FAILED_TESTS=$((FAILED_TESTS + 1))
            echo -e "${YELLOW}  Expected pattern: $expected_pattern${NC}"
            echo -e "${YELLOW}  Actual output:${NC}"
            cat "$output_file" | head -5 | sed 's/^/    /'
            rm -f "$output_file"
            return 1
        fi
    else
        echo -e "${RED}✗ FAILED (command failed)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        rm -f "$output_file"
        return 1
    fi
}

echo ""
echo "=== Phase 1: Basic Execution Tests ==="
echo ""

# Test 1: Binary executes without arguments
run_test "Execute without arguments" || true

# Test 2: Help flag
run_test "Help flag (--help)" --help
run_test "Help flag short (-h)" -h

# Test 3: Version flag
run_test "Version flag (--version)" --version
run_test "Version flag short (-v)" -v

echo ""
echo "=== Phase 2: Command Structure Tests ==="
echo ""

# Test main commands
run_test "Scope command help" scope --help
run_test "Context command help" context --help
run_test "Workspace command help" workspace --help
run_test "Focus command help" focus --help
run_test "Aspect command help" aspect --help

echo ""
echo "=== Phase 3: Subcommand Tests ==="
echo ""

# Test scope subcommands
run_test "Scope list help" scope list --help
run_test "Scope create help" scope create --help
run_test "Scope update help" scope update --help
run_test "Scope delete help" scope delete --help
run_test "Scope show help" scope show --help

# Test context subcommands
run_test "Context switch help" context switch --help
run_test "Context current help" context current --help

# Test workspace subcommands
run_test "Workspace add help" workspace add --help
run_test "Workspace remove help" workspace remove --help
run_test "Workspace list help" workspace list --help
run_test "Workspace clear help" workspace clear --help

echo ""
echo "=== Phase 4: Output Format Tests ==="
echo ""

# Test that version output contains expected format
run_test_with_output "Version output format" "scopes" --version

# Test that help contains usage information
run_test_with_output "Help contains usage" "Usage:" --help

echo ""
echo "=== Phase 5: Error Handling Tests ==="
echo ""

# Test invalid commands (should fail gracefully)
echo -n "Testing: Invalid command handling ... "
if "$BINARY_PATH" invalid-command 2>/dev/null; then
    echo -e "${RED}✗ FAILED (should have failed)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
else
    echo -e "${GREEN}✓ PASSED (failed as expected)${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

# Test invalid flags
echo -n "Testing: Invalid flag handling ... "
if "$BINARY_PATH" --invalid-flag 2>/dev/null; then
    echo -e "${RED}✗ FAILED (should have failed)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
else
    echo -e "${GREEN}✓ PASSED (failed as expected)${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""
echo "========================================="
echo "Test Results Summary"
echo "========================================="
echo -e "Total Tests:  $TOTAL_TESTS"
echo -e "Passed:       ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed:       ${RED}$FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo ""
    echo -e "${GREEN}All tests passed successfully! ✓${NC}"
    exit 0
else
    echo ""
    echo -e "${RED}Some tests failed. Please review the output above.${NC}"
    exit 1
fi
