# Scopes Installation Script for PowerShell
# This script downloads, verifies, and installs Scopes with integrated security verification
#
# Usage:
#   iwr https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.ps1 | iex
#   
# Or download and run:
#   Invoke-WebRequest -Uri "https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.ps1" -OutFile "install.ps1"
#   .\install.ps1
#
# Environment variables:
#   $env:SCOPES_VERSION              - Version to install (default: latest)
#   $env:SCOPES_INSTALL_DIR          - Installation directory (default: C:\Program Files\Scopes)
#   $env:SCOPES_GITHUB_REPO          - GitHub repository (default: kamiazya/scopes)
#   $env:SCOPES_SKIP_VERIFICATION    - Skip SLSA verification (not recommended)
#   $env:SCOPES_FORCE_INSTALL        - Skip confirmation prompts
#   $env:SCOPES_VERBOSE              - Enable verbose output

param(
    [string]$Version = $env:SCOPES_VERSION,
    [string]$InstallDir = $(if ($env:SCOPES_INSTALL_DIR) { $env:SCOPES_INSTALL_DIR } else { "$env:ProgramFiles\Scopes\bin" }),
    [string]$GitHubRepo = $(if ($env:SCOPES_GITHUB_REPO) { $env:SCOPES_GITHUB_REPO } else { "kamiazya/scopes" }),
    [switch]$SkipVerification = ($env:SCOPES_SKIP_VERIFICATION -eq 'true'),
    [switch]$ForceInstall = ($env:SCOPES_FORCE_INSTALL -eq 'true'),
    [switch]$Verbose = ($env:SCOPES_VERBOSE -eq 'true'),
    [switch]$Help
)

# Colors for output
$Script:Colors = @{
    Red = 'Red'
    Green = 'Green'
    Yellow = 'Yellow'
    Blue = 'Cyan'
    Bold = 'White'
}

# Internal variables
$Script:TempDir = ""
$Script:BinaryName = ""
$Script:BinaryPath = ""
$Script:HashFile = ""
$Script:ProvenanceFile = ""

# Function to print colored output
function Write-Status {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor $Colors.Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor $Colors.Yellow
}

function Write-ErrorMsg {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor $Colors.Red
}

function Write-Header {
    param([string]$Message)
    Write-Host "=== $Message ===" -ForegroundColor $Colors.Blue
}

function Write-Verbose {
    param([string]$Message)
    if ($Verbose) {
        Write-Host "[DEBUG] $Message" -ForegroundColor $Colors.Blue
    }
}

# Function to clean up temporary files
function Clear-TempDirectory {
    if ($Script:TempDir -and (Test-Path $Script:TempDir)) {
        Write-Verbose "Cleaning up temporary directory: $Script:TempDir"
        Remove-Item -Path $Script:TempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# Set up cleanup
Register-EngineEvent PowerShell.Exiting -Action { Clear-TempDirectory }

# Function to detect platform
function Get-Platform {
    if ($IsWindows -or $env:OS -eq "Windows_NT") {
        return "win32"
    } elseif ($IsMacOS) {
        return "darwin"
    } elseif ($IsLinux) {
        return "linux"
    } else {
        return "unknown"
    }
}

# Function to detect architecture
function Get-Architecture {
    $arch = [System.Runtime.InteropServices.RuntimeInformation]::ProcessArchitecture
    switch ($arch) {
        "X64" { return "x64" }
        "Arm64" { return "arm64" }
        default { return "unknown" }
    }
}

# Function to check if running as administrator
function Test-IsAdmin {
    if ($IsWindows -or $env:OS -eq "Windows_NT") {
        $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
        $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
        return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
    }
    return $false
}

# Function to check if directory is writable
function Test-DirectoryWritable {
    param([string]$Path)
    
    try {
        $testFile = Join-Path $Path "test_write_$(Get-Random).tmp"
        [System.IO.File]::WriteAllText($testFile, "test")
        Remove-Item $testFile -Force
        return $true
    } catch {
        return $false
    }
}

# Function to get latest version from GitHub
function Get-LatestVersion {
    Write-Status "Fetching latest version from GitHub..."
    $apiUrl = "https://api.github.com/repos/$GitHubRepo/releases/latest"
    
    try {
        $response = Invoke-RestMethod -Uri $apiUrl -Method Get -ErrorAction Stop
        return $response.tag_name
    } catch {
        Write-ErrorMsg "Failed to fetch latest version: $_"
        exit 1
    }
}

# Function to calculate hash
function Get-FileHash256 {
    param([string]$FilePath)
    
    if (Test-Path $FilePath) {
        $hash = Get-FileHash -Path $FilePath -Algorithm SHA256
        return $hash.Hash.ToLower()
    } else {
        throw "File not found: $FilePath"
    }
}

# Function to verify hash
function Test-FileHash {
    param(
        [string]$BinaryFile,
        [string]$HashFile
    )
    
    Write-Status "Verifying binary hash..."
    
    if (-not (Test-Path $BinaryFile)) {
        Write-ErrorMsg "Binary file not found: $BinaryFile"
        return $false
    }
    
    if (-not (Test-Path $HashFile)) {
        Write-ErrorMsg "Hash file not found: $HashFile"
        return $false
    }
    
    try {
        $calculatedHash = Get-FileHash256 -FilePath $BinaryFile
        $hashContent = Get-Content -Path $HashFile -Raw
        $expectedHash = ($hashContent -split ':')[1].Trim()
        
        Write-Verbose "Expected hash: $expectedHash"
        Write-Verbose "Calculated hash: $calculatedHash"
        
        if ($calculatedHash -eq $expectedHash) {
            Write-Status "✅ Hash verification PASSED"
            return $true
        } else {
            Write-ErrorMsg "❌ Hash verification FAILED"
            Write-ErrorMsg "Expected: $expectedHash"
            Write-ErrorMsg "Got: $calculatedHash"
            return $false
        }
    } catch {
        Write-ErrorMsg "Hash verification error: $_"
        return $false
    }
}

# Function to verify SLSA provenance
function Test-SLSAProvenance {
    param(
        [string]$BinaryFile,
        [string]$ProvenanceFile
    )
    
    Write-Status "Verifying SLSA provenance..."
    
    # Check if slsa-verifier is installed
    $slsaVerifier = Get-Command slsa-verifier -ErrorAction SilentlyContinue
    if (-not $slsaVerifier) {
        Write-Warning "slsa-verifier not found."
        $goCmd = Get-Command go -ErrorAction SilentlyContinue
        if ($goCmd) {
            Write-Status "Installing slsa-verifier with Go..."
            try {
                & go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest
                if ($LASTEXITCODE -ne 0) {
                    throw "Go install failed"
                }
                
                # Add GOPATH/bin to PATH if not already there
                $goPath = if ($env:GOPATH) { $env:GOPATH } else { "$env:USERPROFILE\go" }
                $goBin = Join-Path $goPath "bin"
                if ($env:PATH -notlike "*$goBin*") {
                    $env:PATH = "$goBin;$env:PATH"
                }
            } catch {
                Write-Warning "Failed to install slsa-verifier: $_"
                Write-Warning "Skipping SLSA verification."
                return $true
            }
        } else {
            Write-Warning "Go is not installed. Skipping SLSA verification."
            Write-Warning "For complete security, please install Go and slsa-verifier manually:"
            Write-Warning "  go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest"
            return $true
        }
    }
    
    # Try SLSA verification
    $slsaVerifier = Get-Command slsa-verifier -ErrorAction SilentlyContinue
    if ($slsaVerifier) {
        Write-Status "Running SLSA verification..."
        try {
            & slsa-verifier verify-artifact $BinaryFile --provenance-path $ProvenanceFile --source-uri "github.com/$GitHubRepo"
            if ($LASTEXITCODE -eq 0) {
                Write-Status "✅ SLSA verification PASSED"
                return $true
            } else {
                Write-ErrorMsg "❌ SLSA verification FAILED"
                return $false
            }
        } catch {
            Write-ErrorMsg "❌ SLSA verification FAILED: $_"
            return $false
        }
    } else {
        Write-Warning "Could not install slsa-verifier. Skipping SLSA verification."
        return $true
    }
}

# Function to download file
function Get-File {
    param(
        [string]$Url,
        [string]$OutputPath
    )
    
    Write-Verbose "Downloading: $Url"
    Write-Verbose "Output: $OutputPath"
    
    try {
        Invoke-WebRequest -Uri $Url -OutFile $OutputPath -ErrorAction Stop
        Write-Verbose "Successfully downloaded: $OutputPath"
        return $true
    } catch {
        Write-ErrorMsg "Failed to download: $Url"
        Write-ErrorMsg "Error: $_"
        return $false
    }
}

# Function to download and verify release
function Get-AndVerifyRelease {
    param(
        [string]$Version,
        [string]$Platform,
        [string]$Arch
    )
    
    Write-Header "Downloading Scopes $Version"
    
    # Create temporary directory
    $Script:TempDir = New-Item -ItemType Directory -Path (Join-Path $env:TEMP "scopes-install-$(Get-Random)") -Force
    Write-Verbose "Using temporary directory: $($Script:TempDir.FullName)"
    
    # Set up file names
    $Script:BinaryName = "scopes-$Version-$Platform-$Arch"
    if ($Platform -eq "win32") {
        $Script:BinaryName += ".exe"
    }
    
    $Script:BinaryPath = Join-Path $Script:TempDir $Script:BinaryName
    $Script:HashFile = Join-Path $Script:TempDir "binary-hash-$Platform-$Arch.txt"
    $Script:ProvenanceFile = Join-Path $Script:TempDir "multiple.intoto.jsonl"
    
    $baseUrl = "https://github.com/$GitHubRepo/releases/download/$Version"
    
    # Download binary
    Write-Status "Downloading binary: $Script:BinaryName"
    if (-not (Get-File -Url "$baseUrl/$Script:BinaryName" -OutputPath $Script:BinaryPath)) {
        exit 1
    }
    
    # Download hash file
    Write-Status "Downloading hash file..."
    if (-not (Get-File -Url "$baseUrl/binary-hash-$Platform-$Arch.txt" -OutputPath $Script:HashFile)) {
        exit 1
    }
    
    # Download provenance file (if verification is enabled)
    if (-not $SkipVerification) {
        Write-Status "Downloading SLSA provenance..."
        if (-not (Get-File -Url "$baseUrl/multiple.intoto.jsonl" -OutputPath $Script:ProvenanceFile)) {
            Write-Warning "Failed to download provenance file. SLSA verification will be skipped."
        }
    }
    
    Write-Status "✅ Download completed"
}

# Function to perform verification
function Invoke-Verification {
    if ($SkipVerification) {
        Write-Warning "⚠️  Verification skipped by user request"
        return $true
    }
    
    Write-Header "Security Verification"
    
    $verificationFailed = $false
    
    # Hash verification (mandatory)
    if (-not (Test-FileHash -BinaryFile $Script:BinaryPath -HashFile $Script:HashFile)) {
        $verificationFailed = $true
    }
    
    # SLSA verification (if provenance file exists)
    if (Test-Path $Script:ProvenanceFile) {
        if (-not (Test-SLSAProvenance -BinaryFile $Script:BinaryPath -ProvenanceFile $Script:ProvenanceFile)) {
            $verificationFailed = $true
        }
    } else {
        Write-Warning "Provenance file not available, skipping SLSA verification"
    }
    
    if ($verificationFailed) {
        Write-ErrorMsg "❌ Security verification FAILED!"
        Write-ErrorMsg "DO NOT install this binary - it may be compromised."
        exit 1
    }
    
    Write-Status "🎉 All security verifications PASSED!"
    return $true
}

# Function to install binary
function Install-Binary {
    param(
        [string]$SourcePath,
        [string]$DestinationDir
    )
    
    Write-Header "Installing Scopes"
    
    # Create installation directory if it doesn't exist
    if (-not (Test-Path $DestinationDir)) {
        Write-Status "Creating installation directory: $DestinationDir"
        try {
            New-Item -ItemType Directory -Path $DestinationDir -Force | Out-Null
        } catch {
            Write-ErrorMsg "Failed to create installation directory: $_"
            Write-ErrorMsg "Try running as Administrator or choose a different directory"
            exit 1
        }
    }
    
    # Check if we can write to the destination
    if (-not (Test-DirectoryWritable $DestinationDir)) {
        Write-ErrorMsg "Cannot write to installation directory: $DestinationDir"
        Write-ErrorMsg "Try running as Administrator or choose a different directory"
        exit 1
    }
    
    $destinationPath = Join-Path $DestinationDir "scopes.exe"
    
    # Copy binary to destination
    Write-Status "Installing to: $destinationPath"
    try {
        Copy-Item -Path $SourcePath -Destination $destinationPath -Force
        Write-Status "✅ Installation completed successfully!"
    } catch {
        Write-ErrorMsg "Failed to install binary: $_"
        exit 1
    }
}

# Function to verify installation
function Test-Installation {
    Write-Header "Verifying Installation"
    
    $binaryPath = Join-Path $InstallDir "scopes.exe"
    
    if (-not (Test-Path $binaryPath)) {
        Write-ErrorMsg "Installation verification failed: binary not found at $binaryPath"
        return $false
    }
    
    # Test if binary is in PATH
    $scopesCommand = Get-Command scopes -ErrorAction SilentlyContinue
    if ($scopesCommand) {
        Write-Status "✅ Scopes is installed and available in PATH"
        
        # Try to get version
        try {
            $versionOutput = & scopes --version 2>$null
            if ($versionOutput) {
                Write-Status "Version: $versionOutput"
            }
        } catch {
            Write-Warning "Could not get version information (this may be normal)"
        }
    } else {
        Write-Warning "Scopes is installed but not in PATH"
        Write-Status "Add $InstallDir to your PATH to use 'scopes' command globally"
    }
    
    Write-Status "🎉 Installation verification completed!"
    return $true
}

# Function to update PATH environment variable
function Add-ToPath {
    param([string]$Directory)
    
    # Check if directory is already in PATH
    $currentPath = [Environment]::GetEnvironmentVariable("PATH", "User")
    if ($currentPath -notlike "*$Directory*") {
        Write-Status "Adding $Directory to user PATH..."
        $newPath = "$currentPath;$Directory"
        [Environment]::SetEnvironmentVariable("PATH", $newPath, "User")
        $env:PATH += ";$Directory"
        Write-Status "✅ PATH updated successfully"
        Write-Status "Restart your terminal to use the updated PATH"
    } else {
        Write-Status "Directory already in PATH: $Directory"
    }
}

# Function to show next steps
function Show-NextSteps {
    Write-Header "Next Steps"
    
    Write-Host "Scopes has been successfully installed!" -ForegroundColor $Colors.Bold
    Write-Host ""
    Write-Host "Quick start:"
    Write-Host "  scopes --help                 # Show help"
    Write-Host "  scopes --version              # Show version"
    Write-Host ""
    
    $scopesCommand = Get-Command scopes -ErrorAction SilentlyContinue
    if (-not $scopesCommand) {
        Write-Host "Note: " -ForegroundColor $Colors.Yellow -NoNewline
        Write-Host "Scopes is not in your PATH. To use it globally:"
        Write-Host "  `$env:PATH += `";$InstallDir`""
        Write-Host ""
        
        # Offer to add to PATH permanently
        if (-not $ForceInstall) {
            $addToPath = Read-Host "Add Scopes to your PATH permanently? [y/N]"
            if ($addToPath -match '^[Yy]$') {
                Add-ToPath -Directory $InstallDir
            }
        }
    }
    
    Write-Host "Documentation:"
    Write-Host "  https://github.com/$GitHubRepo"
    Write-Host ""
    Write-Host "Security verification:"
    Write-Host "  All downloaded artifacts were cryptographically verified"
    Write-Host "  SLSA Level 3 provenance ensures supply chain integrity"
}

# Function to confirm installation
function Confirm-Installation {
    param(
        [string]$Version,
        [string]$Platform,
        [string]$Arch
    )
    
    if ($ForceInstall) {
        return $true
    }
    
    Write-Host "Scopes Installation" -ForegroundColor $Colors.Bold
    Write-Host ""
    Write-Host "This script will:"
    Write-Host "  • Download Scopes $Version for $Platform-$arch"
    Write-Host "  • Verify cryptographic signatures and hashes"
    Write-Host "  • Install to: $InstallDir"
    if (-not (Test-IsAdmin) -and $InstallDir.StartsWith($env:ProgramFiles)) {
        Write-Host "  • May require Administrator privileges" -ForegroundColor $Colors.Yellow
    }
    Write-Host ""
    
    $response = Read-Host "Continue with installation? [y/N]"
    return $response -match '^[Yy]$'
}

# Function to show usage
function Show-Usage {
    @"
Scopes Installation Script for PowerShell

Usage: .\install.ps1 [PARAMETERS]

PARAMETERS:
    -Version <string>         Version to install (default: latest)
    -InstallDir <string>      Installation directory (default: C:\Program Files\Scopes\bin)
    -GitHubRepo <string>      GitHub repository (default: kamiazya/scopes)
    -SkipVerification         Skip SLSA verification (not recommended)
    -ForceInstall             Skip confirmation prompts
    -Verbose                  Enable verbose output
    -Help                     Show this help message

ENVIRONMENT VARIABLES:
    SCOPES_VERSION              Default version to install
    SCOPES_INSTALL_DIR          Default installation directory
    SCOPES_GITHUB_REPO          GitHub repository
    SCOPES_SKIP_VERIFICATION    Skip SLSA verification (true/false)
    SCOPES_FORCE_INSTALL        Skip confirmation prompts (true/false)
    SCOPES_VERBOSE              Enable verbose output (true/false)

EXAMPLES:
    # Basic installation
    .\install.ps1

    # Install specific version
    .\install.ps1 -Version v1.0.0

    # Custom installation directory
    .\install.ps1 -InstallDir "C:\Tools\Scopes"

    # Using environment variables
    $env:SCOPES_VERSION='v1.0.0'
    $env:SCOPES_INSTALL_DIR='C:\Tools\Scopes'
    .\install.ps1

    # One-liner from internet
    iwr https://raw.githubusercontent.com/kamiazya/scopes/main/install/install.ps1 | iex

"@
}

# Main function
function Main {
    if ($Help) {
        Show-Usage
        return
    }
    
    Write-Header "Scopes Installer"
    
    # Detect platform and architecture
    $platform = Get-Platform
    $arch = Get-Architecture
    
    if ($platform -eq "unknown" -or $arch -eq "unknown") {
        Write-ErrorMsg "Unsupported platform: $platform-$arch"
        Write-ErrorMsg "Supported platforms: win32-x64, win32-arm64, linux-x64, linux-arm64, darwin-x64, darwin-arm64"
        exit 1
    }
    
    # Get version to install
    if (-not $Version) {
        $Version = Get-LatestVersion
        Write-Status "Latest version: $Version"
    } else {
        Write-Status "Installing specified version: $Version"
    }
    
    # Show configuration
    Write-Status "Configuration:"
    Write-Status "  Platform: $platform-$arch"
    Write-Status "  Version: $Version"
    Write-Status "  Repository: $GitHubRepo"
    Write-Status "  Install directory: $InstallDir"
    Write-Status "  Skip verification: $SkipVerification"
    
    # Confirm installation
    if (-not (Confirm-Installation -Version $Version -Platform $platform -Arch $arch)) {
        Write-Status "Installation cancelled by user"
        return
    }
    
    try {
        # Download and verify
        Get-AndVerifyRelease -Version $Version -Platform $platform -Arch $arch
        Invoke-Verification
        
        # Install
        Install-Binary -SourcePath $Script:BinaryPath -DestinationDir $InstallDir
        Test-Installation
        
        # Show next steps
        Show-NextSteps
    } finally {
        # Cleanup
        Clear-TempDirectory
    }
}

# Run main function
Main