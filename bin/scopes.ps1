#!/usr/bin/env pwsh
# Scopes CLI wrapper script for Windows PowerShell
# This script provides a convenient way to run the Scopes JAR file

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
)

$ErrorActionPreference = "Stop"

# Determine the directory where this script is located
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Try multiple JAR file locations
$JarLocations = @(
    Join-Path $ScriptDir "scopes.jar"
    Join-Path $ScriptDir "..\lib\scopes.jar"
    Join-Path $ScriptDir "..\share\scopes\scopes.jar"
)

$JarFile = $null
foreach ($location in $JarLocations) {
    if (Test-Path $location) {
        $JarFile = $location
        break
    }
}

if (-not $JarFile) {
    Write-Error "Error: scopes.jar not found in any of the expected locations:"
    foreach ($location in $JarLocations) {
        Write-Error "  - $location"
    }
    Write-Error ""
    Write-Error "Please ensure Scopes is installed correctly."
    exit 1
}

# Check if Java is available
try {
    $null = Get-Command java -ErrorAction Stop
} catch {
    Write-Error "Error: Java is not installed or not in PATH"
    Write-Error ""
    Write-Error "Scopes requires Java 21 or later to run."
    Write-Error ""
    Write-Error "Installation instructions:"
    Write-Error "  Download from: https://adoptium.net/"
    Write-Error "  Or use Chocolatey: choco install openjdk21"
    Write-Error "  Or use Scoop: scoop install openjdk21"
    exit 1
}

# Check Java version (robust extraction for EA builds and vendor variations)
try {
    $javaVersionOutput = & java -version 2>&1 | Select-Object -First 1
    # Extract major version using regex that handles EA builds and vendor variations
    if ($javaVersionOutput -match 'version "(\d+)') {
        $majorVersion = [int]$Matches[1]

        # Handle old format like "1.8.0_xxx" where major version is second number
        if ($majorVersion -eq 1 -and $javaVersionOutput -match 'version "1\.(\d+)') {
            $majorVersion = [int]$Matches[1]
        }

        if ($majorVersion -lt 21) {
            Write-Error "Error: Java $majorVersion is installed, but Scopes requires Java 21 or later"
            Write-Error ""
            Write-Error "Please upgrade your Java installation:"
            Write-Error "  Current version: Java $majorVersion"
            Write-Error "  Required version: Java 21+"
            Write-Error ""
            Write-Error "Installation instructions:"
            Write-Error "  Download from: https://adoptium.net/"
            Write-Error "  Or use Chocolatey: choco install openjdk21"
            Write-Error "  Or use Scoop: scoop install openjdk21"
            exit 1
        }
    } else {
        Write-Warning "Warning: Could not determine Java version, proceeding anyway..."
    }
} catch {
    Write-Warning "Warning: Could not check Java version, proceeding anyway..."
}

# Load Java options from environment or config file
# Priority: SCOPES_JAVA_OPTS > JAVA_OPTS > config file
$JavaOptions = ""

# Check for config file in standard locations
$ConfigLocations = @(
    (Join-Path $env:APPDATA "scopes\scopes.conf"),
    (Join-Path $env:USERPROFILE ".scopes\scopes.conf")
)

foreach ($configFile in $ConfigLocations) {
    if (Test-Path $configFile) {
        # Read JAVA_OPTS from config file
        Get-Content $configFile | ForEach-Object {
            if ($_ -match '^JAVA_OPTS\s*=\s*(.+)$') {
                $value = $Matches[1].Trim()
                # Remove quotes if present
                $value = $value -replace '^["'']|["'']$', ''
                $JavaOptions = $value
                break
            }
        }
        break
    }
}

# Override with environment variables
if ($env:JAVA_OPTS) {
    $JavaOptions = $env:JAVA_OPTS
}

if ($env:SCOPES_JAVA_OPTS) {
    $JavaOptions = $env:SCOPES_JAVA_OPTS
}

# Execute the JAR file with all arguments passed through
if ($JavaOptions) {
    # Split JAVA_OPTIONS into array for proper argument passing
    $javaArgs = $JavaOptions -split '\s+'
    & java @javaArgs -jar $JarFile @Arguments
} else {
    & java -jar $JarFile @Arguments
}
exit $LASTEXITCODE
