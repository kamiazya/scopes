# E2E Test Suite for Scopes Native Binary (Windows)
# This script performs comprehensive testing of the native binary on Windows

param(
    [Parameter(Mandatory=$false)]
    [string]$BinaryPath = $env:SCOPES_BINARY_PATH
)

# Exit on error
$ErrorActionPreference = "Stop"

# Color functions
function Write-Success { Write-Host $args -ForegroundColor Green }
function Write-Error { Write-Host $args -ForegroundColor Red }
function Write-Warning { Write-Host $args -ForegroundColor Yellow }

# Validate binary path
if ([string]::IsNullOrEmpty($BinaryPath)) {
    Write-Error "Error: Binary path not provided"
    Write-Host "Usage: .\run-native-tests.ps1 -BinaryPath <path-to-binary>"
    exit 1
}

if (-not (Test-Path $BinaryPath)) {
    Write-Error "Error: Binary not found at: $BinaryPath"
    exit 1
}

Write-Host "========================================="
Write-Host "Scopes Native Binary E2E Test Suite"
Write-Host "Binary: $BinaryPath"
Write-Host "========================================="

$TotalTests = 0
$PassedTests = 0
$FailedTests = 0

# Function to run a test
function Test-Command {
    param(
        [string]$TestName,
        [string[]]$Arguments
    )
    
    $script:TotalTests++
    
    Write-Host -NoNewline "Testing: $TestName ... "
    
    try {
        $output = & $BinaryPath $Arguments 2>&1
        $exitCode = $LASTEXITCODE
        
        if ($exitCode -eq 0) {
            Write-Success "✓ PASSED"
            $script:PassedTests++
            return $true
        } else {
            Write-Error "✗ FAILED"
            $script:FailedTests++
            Write-Warning "  Command: $BinaryPath $($Arguments -join ' ')"
            return $false
        }
    } catch {
        Write-Error "✗ FAILED (exception)"
        $script:FailedTests++
        Write-Warning "  Error: $_"
        return $false
    }
}

# Function to run test expecting specific output
function Test-CommandWithOutput {
    param(
        [string]$TestName,
        [string]$ExpectedPattern,
        [string[]]$Arguments
    )
    
    $script:TotalTests++
    
    Write-Host -NoNewline "Testing: $TestName ... "
    
    try {
        $output = & $BinaryPath $Arguments 2>&1 | Out-String
        $exitCode = $LASTEXITCODE
        
        if ($exitCode -eq 0 -and $output -match $ExpectedPattern) {
            Write-Success "✓ PASSED"
            $script:PassedTests++
            return $true
        } else {
            Write-Error "✗ FAILED (output mismatch)"
            $script:FailedTests++
            Write-Warning "  Expected pattern: $ExpectedPattern"
            Write-Warning "  Actual output: $($output.Substring(0, [Math]::Min(100, $output.Length)))"
            return $false
        }
    } catch {
        Write-Error "✗ FAILED (exception)"
        $script:FailedTests++
        Write-Warning "  Error: $_"
        return $false
    }
}

# Function to test commands that are expected to fail
function Test-CommandExpectFail {
    param(
        [string]$TestName,
        [string[]]$Arguments
    )
    
    $script:TotalTests++
    
    Write-Host -NoNewline "Testing: $TestName ... "
    
    try {
        $output = & $BinaryPath $Arguments 2>&1
        $exitCode = $LASTEXITCODE
        
        if ($exitCode -ne 0) {
            Write-Success "✓ PASSED (failed as expected)"
            $script:PassedTests++
            return $true
        } else {
            Write-Error "✗ FAILED (should have failed)"
            $script:FailedTests++
            return $false
        }
    } catch {
        # Exception is expected for invalid commands
        Write-Success "✓ PASSED (failed as expected)"
        $script:PassedTests++
        return $true
    }
}

Write-Host ""
Write-Host "=== Phase 1: Basic Execution Tests ==="
Write-Host ""

# Test 1: Binary executes without arguments
Test-Command "Execute without arguments" @()

# Test 2: Help flag
Test-Command "Help flag (--help)" @("--help")
Test-Command "Help flag short (-h)" @("-h")

Write-Host ""
Write-Host "=== Phase 2: Command Structure Tests ==="
Write-Host ""

# Test main commands
Test-Command "Scope command help" @("scope", "--help")
Test-Command "Context command help" @("context", "--help")
Test-Command "Workspace command help" @("workspace", "--help")
Test-Command "Focus command help" @("focus", "--help")
Test-Command "Aspect command help" @("aspect", "--help")

Write-Host ""
Write-Host "=== Phase 3: Subcommand Tests ==="
Write-Host ""

# Test scope subcommands
Test-Command "Scope list help" @("scope", "list", "--help")
Test-Command "Scope create help" @("scope", "create", "--help")
Test-Command "Scope update help" @("scope", "update", "--help")
Test-Command "Scope delete help" @("scope", "delete", "--help")
Test-Command "Scope show help" @("scope", "show", "--help")

# Test context subcommands
Test-Command "Context switch help" @("context", "switch", "--help")
Test-Command "Context current help" @("context", "current", "--help")

# Test workspace subcommands
Test-Command "Workspace add help" @("workspace", "add", "--help")
Test-Command "Workspace remove help" @("workspace", "remove", "--help")
Test-Command "Workspace list help" @("workspace", "list", "--help")
Test-Command "Workspace clear help" @("workspace", "clear", "--help")

Write-Host ""
Write-Host "=== Phase 4: Output Format Tests ==="
Write-Host ""

# Test that help contains usage information
Test-CommandWithOutput "Help contains usage" "Usage:" @("--help")

Write-Host ""
Write-Host "=== Phase 5: Error Handling Tests ==="
Write-Host ""

# Test invalid commands (should fail gracefully)
Test-CommandExpectFail "Invalid command handling" @("invalid-command")

# Test invalid flags
Test-CommandExpectFail "Invalid flag handling" @("--invalid-flag")

Write-Host ""
Write-Host "========================================="
Write-Host "Test Results Summary"
Write-Host "========================================="
Write-Host "Total Tests:  $TotalTests"
Write-Success "Passed:       $PassedTests"
Write-Error "Failed:       $FailedTests"

if ($FailedTests -eq 0) {
    Write-Host ""
    Write-Success "All tests passed successfully! ✓"
    exit 0
} else {
    Write-Host ""
    Write-Error "Some tests failed. Please review the output above."
    exit 1
}