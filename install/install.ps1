#!/usr/bin/env pwsh
# Scopes Installation Script for JAR Distribution (Windows PowerShell)
# This script installs Scopes JAR with wrapper scripts from local files
#
# Usage:
#   .\install.ps1 [OPTIONS]
#
# Options:
#   -InstallDir DIR     Installation directory (default: C:\Program Files\scopes)
#   -Force              Skip confirmation prompts
#   -Verbose            Enable verbose output
#   -Help               Show help message

param(
    [string]$InstallDir = $env:SCOPES_INSTALL_DIR,
    [switch]$Force = [bool]$env:SCOPES_FORCE_INSTALL,
    [switch]$VerboseOutput = [bool]$env:SCOPES_VERBOSE,
    [switch]$Help
)

$ErrorActionPreference = "Stop"

# Colors for output
$ColorInfo = "Green"
$ColorWarn = "Yellow"
$ColorError = "Red"
$ColorHeader = "Cyan"
$ColorDebug = "Blue"

# Script directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Configuration from environment or defaults
if (-not $InstallDir) {
    $InstallDir = "C:\Program Files\scopes"
}

# Internal variables
$JarFile = ""
$WrapperScriptBat = ""
$WrapperScriptPs1 = ""

# Function to print colored output
function Write-Status {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor $ColorInfo
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor $ColorWarn
}

function Write-Err {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor $ColorError
}

function Write-Header {
    param([string]$Message)
    Write-Host "=== $Message ===" -ForegroundColor $ColorHeader
}

function Write-Debug {
    param([string]$Message)
    if ($VerboseOutput) {
        Write-Host "[DEBUG] $Message" -ForegroundColor $ColorDebug
    }
}

# Function to show help
function Show-Help {
    @"
Scopes Installation Script for JAR Distribution

This script installs Scopes JAR file with wrapper scripts.

USAGE:
    .\install.ps1 [OPTIONS]

OPTIONS:
    -InstallDir DIR     Installation directory (default: C:\Program Files\scopes)
    -Force              Skip confirmation prompts
    -VerboseOutput      Enable verbose output
    -Help               Show this help message

ENVIRONMENT VARIABLES:
    SCOPES_INSTALL_DIR       Installation directory
    SCOPES_FORCE_INSTALL     Skip confirmation prompts
    SCOPES_VERBOSE           Enable verbose output

EXAMPLES:
    # Standard installation (requires admin)
    .\install.ps1

    # Custom installation directory
    .\install.ps1 -InstallDir "$env:LOCALAPPDATA\scopes"

    # Force installation without prompts
    .\install.ps1 -Force

"@
    exit 0
}

if ($Help) {
    Show-Help
}

# Check Java installation
function Test-Java {
    Write-Header "Checking Java Installation"

    try {
        $null = Get-Command java -ErrorAction Stop
    } catch {
        Write-Err "Java is not installed"
        Write-Host ""
        Write-Host "Scopes requires Java 21 or later to run."
        Write-Host ""
        Write-Host "Installation instructions:"
        Write-Host "  Download from: https://adoptium.net/"
        Write-Host "  Or use Chocolatey: choco install openjdk21"
        Write-Host "  Or use Scoop: scoop install openjdk21"
        exit 1
    }

    try {
        $javaVersionOutput = & java -version 2>&1 | Select-Object -First 1
        if ($javaVersionOutput -match 'version "(.+?)"') {
            $javaVersionString = $Matches[1]

            # Extract major version (handle both old format like 1.8 and new format like 21.0.1)
            $versionParts = $javaVersionString -split '\.'
            if ($versionParts[0] -eq "1") {
                $majorVersion = [int]$versionParts[1]
            } else {
                $majorVersion = [int]$versionParts[0]
            }

            if ($majorVersion -lt 21) {
                Write-Err "Java $majorVersion is installed, but Scopes requires Java 21 or later"
                Write-Host ""
                Write-Host "Please upgrade your Java installation:"
                Write-Host "  Current version: Java $majorVersion"
                Write-Host "  Required version: Java 21+"
                exit 1
            }

            Write-Status "Java $majorVersion detected"
        } else {
            Write-Warn "Could not parse Java version, proceeding anyway..."
        }
    } catch {
        Write-Warn "Could not check Java version, proceeding anyway..."
    }
}

# Detect binary
function Find-Files {
    Write-Header "Detecting Installation Files"

    # Find JAR file
    $jarLocations = @(
        Join-Path $ScriptDir "scopes.jar"
        Join-Path $ScriptDir "..\scopes.jar"
    )

    foreach ($location in $jarLocations) {
        if (Test-Path $location) {
            $script:JarFile = $location
            break
        }
    }

    if (-not $JarFile) {
        Write-Err "scopes.jar not found"
        exit 1
    }
    Write-Debug "JAR file: $JarFile"

    # Find wrapper scripts
    $wrapperLocations = @(
        Join-Path $ScriptDir "bin\scopes.bat"
        Join-Path $ScriptDir "..\bin\scopes.bat"
    )

    foreach ($location in $wrapperLocations) {
        if (Test-Path $location) {
            $script:WrapperScriptBat = $location
            break
        }
    }

    if (-not $WrapperScriptBat) {
        Write-Err "Wrapper script scopes.bat not found"
        exit 1
    }
    Write-Debug "Wrapper script (batch): $WrapperScriptBat"

    # Find PowerShell wrapper
    $psWrapperLocations = @(
        Join-Path $ScriptDir "bin\scopes.ps1"
        Join-Path $ScriptDir "..\bin\scopes.ps1"
    )

    foreach ($location in $psWrapperLocations) {
        if (Test-Path $location) {
            $script:WrapperScriptPs1 = $location
            break
        }
    }

    if (-not $WrapperScriptPs1) {
        Write-Err "Wrapper script scopes.ps1 not found"
        exit 1
    }
    Write-Debug "Wrapper script (PowerShell): $WrapperScriptPs1"

    Write-Status "Installation files detected"
}

# Verify installation
function Test-InstallationFiles {
    Write-Header "Verifying Installation"

    # Check if hash file exists
    $hashFile = Join-Path $ScriptDir "verification\scopes.jar.sha256"
    if (-not (Test-Path $hashFile)) {
        Write-Warn "Hash file not found, skipping verification"
        return
    }

    Write-Status "Verifying SHA256 hash..."

    try {
        # Parse hash file (format: "<hash>  <filename>")
        # Extract only the first field (the hash)
        $hashLine = (Get-Content $hashFile).Trim()
        $expectedHash = ($hashLine -split '\s+')[0]

        $actualHash = (Get-FileHash -Path $JarFile -Algorithm SHA256).Hash

        # Case-insensitive comparison
        if ($actualHash -ine $expectedHash) {
            Write-Err "Hash verification failed"
            Write-Host "Expected: $expectedHash"
            Write-Host "Actual: $actualHash"
            exit 1
        }

        Write-Status "Hash verification passed"
    } catch {
        Write-Warn "Hash verification failed: $_"
        return
    }

    # SLSA verification (optional if slsa-verifier is available)
    $slsaFile = Join-Path $ScriptDir "verification\multiple.intoto.jsonl"
    if (Test-Path $slsaFile) {
        try {
            $null = Get-Command slsa-verifier -ErrorAction Stop
            Write-Status "Verifying SLSA provenance..."
            $result = & slsa-verifier verify-artifact $JarFile `
                --provenance-path $slsaFile `
                --source-uri github.com/kamiazya/scopes 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Status "SLSA provenance verification passed"
            } else {
                Write-Warn "SLSA provenance verification failed (non-critical)"
            }
        } catch {
            Write-Debug "SLSA provenance file found, but slsa-verifier is not installed"
            Write-Debug "To enable SLSA verification: go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest"
        }
    }
}

# Install files
function Install-Files {
    Write-Header "Installing Scopes"

    # Create installation directories
    $binDir = Join-Path $InstallDir "bin"
    $libDir = Join-Path $InstallDir "lib"

    New-Item -ItemType Directory -Force -Path $binDir | Out-Null
    New-Item -ItemType Directory -Force -Path $libDir | Out-Null

    # Copy JAR file
    Write-Status "Installing JAR file to $libDir"
    Copy-Item -Path $JarFile -Destination (Join-Path $libDir "scopes.jar") -Force

    # Copy wrapper scripts
    Write-Status "Installing wrapper scripts to $binDir"
    Copy-Item -Path $WrapperScriptBat -Destination (Join-Path $binDir "scopes.bat") -Force
    Copy-Item -Path $WrapperScriptPs1 -Destination (Join-Path $binDir "scopes.ps1") -Force

    Write-Status "Installation completed successfully"
}

# Test installation
function Test-InstalledVersion {
    Write-Header "Testing Installation"

    $scopesExe = Join-Path $InstallDir "bin\scopes.bat"

    try {
        $null = & $scopesExe --help 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Err "Installation test failed"
            Write-Err "Scopes command is not working properly"
            exit 1
        }

        Write-Status "Installation test passed"
        Write-Host ""
        Write-Host "Scopes has been installed successfully!"
        Write-Host "Run 'scopes --help' to get started"
    } catch {
        Write-Err "Installation test failed: $_"
        exit 1
    }
}

# Main installation flow
function Main {
    Write-Header "Scopes JAR Installation"
    Write-Host ""

    # Check Java
    Test-Java
    Write-Host ""

    # Detect files
    Find-Files
    Write-Host ""

    # Verify (optional)
    Test-InstallationFiles
    Write-Host ""

    # Confirm installation
    if (-not $Force) {
        Write-Host "Installation Summary:" -ForegroundColor $ColorHeader
        Write-Host "  JAR file:      $JarFile"
        Write-Host "  Wrapper (bat): $WrapperScriptBat"
        Write-Host "  Wrapper (ps1): $WrapperScriptPs1"
        Write-Host "  Install to:    $InstallDir"
        Write-Host ""

        $response = Read-Host "Continue with installation? [y/N]"
        if ($response -notmatch '^[Yy]$') {
            Write-Warn "Installation cancelled"
            exit 0
        }
    }

    # Install
    Install-Files
    Write-Host ""

    # Test
    Test-InstalledVersion
    Write-Host ""

    Write-Status "Scopes has been installed successfully!"
    Write-Host ""

    # Check PATH and offer to update
    $pathEntries = $env:PATH -split ';'
    $binPath = Join-Path $InstallDir "bin"

    if ($pathEntries -notcontains $binPath) {
        Write-Warn "$binPath is not in your PATH"
        Write-Host ""

        if (-not $Force) {
            $response = Read-Host "Would you like to add $binPath to your User PATH? [y/N]"
            if ($response -match '^[Yy]$') {
                try {
                    # Get current user PATH
                    $currentPath = [Environment]::GetEnvironmentVariable('Path', 'User')
                    if ($currentPath -notlike "*$binPath*") {
                        # Add to user PATH
                        $newPath = if ($currentPath) { "$currentPath;$binPath" } else { $binPath }
                        [Environment]::SetEnvironmentVariable('Path', $newPath, 'User')
                        # Update current session
                        $env:PATH += ";$binPath"
                        Write-Status "PATH updated successfully"
                        Write-Host ""
                        Write-Host "Please restart your terminal to apply changes globally"
                    } else {
                        Write-Status "$binPath is already in User PATH"
                    }
                } catch {
                    Write-Warn "Failed to update PATH: $_"
                    Write-Host ""
                    Write-Host "You can manually add it:"
                    Write-Host "  User PATH:   `$env:PATH += `";$binPath`"; [Environment]::SetEnvironmentVariable('Path', `$env:PATH, 'User')"
                }
            } else {
                Write-Host ""
                Write-Host "You can manually add it to your PATH:"
                Write-Host "  User PATH:   `$env:PATH += `";$binPath`"; [Environment]::SetEnvironmentVariable('Path', `$env:PATH, 'User')"
                Write-Host "  System PATH: Requires admin - use System Properties > Environment Variables"
            }
        } else {
            Write-Host "Add it to your PATH:"
            Write-Host "  User PATH:   `$env:PATH += `";$binPath`"; [Environment]::SetEnvironmentVariable('Path', `$env:PATH, 'User')"
            Write-Host "  System PATH: Requires admin - use System Properties > Environment Variables"
        }
    } else {
        Write-Status "$binPath is already in your PATH"
    }

    Write-Host ""
    Write-Host "Next steps:"
    Write-Host "  Run 'scopes --help' to get started"
    Write-Host ""
}

Main
