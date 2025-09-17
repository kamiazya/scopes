# Cross-platform verification script for Scopes releases (PowerShell version)
# Supports Windows, macOS, and Linux with PowerShell Core

param(
    [Parameter(Position=0, HelpMessage="Release version to verify (e.g., v1.0.0)")]
    [string]$Version = $env:SCOPES_VERSION,
    
    [Parameter(HelpMessage="Path to binary file")]
    [string]$BinaryPath = $env:SCOPES_BINARY_PATH,
    
    [Parameter(HelpMessage="Path to provenance file")]
    [string]$ProvenancePath = $env:SCOPES_PROVENANCE_PATH,
    
    [Parameter(HelpMessage="Path to hash file")]
    [string]$HashFile = $env:SCOPES_HASH_FILE,
    
    [Parameter(HelpMessage="Auto-download release files")]
    [switch]$AutoDownload = ($env:SCOPES_AUTO_DOWNLOAD -eq 'true'),
    
    [Parameter(HelpMessage="Skip SLSA verification")]
    [switch]$SkipSLSA = ($env:SCOPES_VERIFY_SLSA -eq 'false'),
    
    [Parameter(HelpMessage="Skip hash verification")]
    [switch]$SkipHash = ($env:SCOPES_VERIFY_HASH -eq 'false'),
    
    [Parameter(HelpMessage="Also verify SBOM files")]
    [switch]$VerifySBOM = ($env:SCOPES_VERIFY_SBOM -eq 'true'),
    
    [Parameter(HelpMessage="Override platform detection (linux/darwin/win32)")]
    [string]$PlatformOverride = $env:SCOPES_PLATFORM,
    
    [Parameter(HelpMessage="Override architecture detection (x64/arm64)")]
    [string]$ArchOverride = $env:SCOPES_ARCH,
    
    [Parameter(HelpMessage="GitHub repository (default: kamiazya/scopes)")]
    [string]$GitHubRepo = (if ($env:SCOPES_GITHUB_REPO) { $env:SCOPES_GITHUB_REPO } else { "kamiazya/scopes" }),
    
    [Parameter(HelpMessage="Show help message")]
    [switch]$Help
)

# Colors for output
$Script:Colors = @{
    Red = 'Red'
    Green = 'Green'
    Yellow = 'Yellow'
    Blue = 'Cyan'
}

# Function to print colored output
function Write-Status {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor $Colors.Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor $Colors.Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor $Colors.Red
}

function Write-Header {
    param([string]$Message)
    Write-Host "=== $Message ===" -ForegroundColor $Colors.Blue
}

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

# Function to calculate hash cross-platform
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
    
    Write-Header "Hash Verification"
    
    if (-not (Test-Path $BinaryFile)) {
        Write-Error "Binary file not found: $BinaryFile"
        return $false
    }
    
    if (-not (Test-Path $HashFile)) {
        Write-Error "Hash file not found: $HashFile"
        return $false
    }
    
    Write-Status "Calculating hash for: $BinaryFile"
    try {
        $calculatedHash = Get-FileHash256 -FilePath $BinaryFile
    } catch {
        Write-Error "Failed to calculate hash: $_"
        return $false
    }
    
    Write-Status "Reading expected hash from: $HashFile"
    $hashContent = Get-Content -Path $HashFile -Raw
    $expectedHash = ($hashContent -split ':')[1].Trim()
    
    Write-Status "Expected hash: $expectedHash"
    Write-Status "Calculated hash: $calculatedHash"
    
    if ($calculatedHash -eq $expectedHash) {
        Write-Status "✅ Hash verification PASSED"
        return $true
    } else {
        Write-Error "❌ Hash verification FAILED"
        Write-Error "Hashes do not match!"
        return $false
    }
}

# Function to verify SLSA provenance
function Test-SLSAProvenance {
    param(
        [string]$BinaryFile,
        [string]$ProvenanceFile
    )
    
    Write-Header "SLSA Provenance Verification"
    
    if (-not (Test-Path $BinaryFile)) {
        Write-Error "Binary file not found: $BinaryFile"
        return $false
    }
    
    if (-not (Test-Path $ProvenanceFile)) {
        Write-Error "Provenance file not found: $ProvenanceFile"
        return $false
    }
    
    # Check if slsa-verifier is installed
    $slsaVerifier = Get-Command slsa-verifier -ErrorAction SilentlyContinue
    if (-not $slsaVerifier) {
        Write-Warning "slsa-verifier not found."
        $goCmd = Get-Command go -ErrorAction SilentlyContinue
        if ($goCmd) {
            Write-Warning "Installing slsa-verifier..."
            & go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest
            if ($LASTEXITCODE -ne 0) {
                Write-Error "Failed to install slsa-verifier"
                return $false
            }
        } else {
            Write-Error "Go is required to install slsa-verifier"
            Write-Error "Please install Go and run: go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest"
            return $false
        }
    }
    
    Write-Status "Verifying SLSA provenance..."
    $result = & slsa-verifier verify-artifact $BinaryFile --provenance-path $ProvenanceFile --source-uri "github.com/$GitHubRepo" 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Status "✅ SLSA verification PASSED"
        return $true
    } else {
        Write-Error "❌ SLSA verification FAILED"
        Write-Error $result
        return $false
    }
}

# Function to download release files
function Get-ReleaseFiles {
    param(
        [string]$Version,
        [string]$Platform,
        [string]$Arch
    )
    
    Write-Header "Auto-downloading release files"
    
    $baseUrl = "https://github.com/$GitHubRepo/releases/download/$Version"
    $binaryName = "scopes-$Version-$Platform-$Arch"
    $hashName = "binary-hash-$Platform-$Arch.txt"
    $provenanceName = "multiple.intoto.jsonl"
    
    if ($Platform -eq "win32") {
        $binaryName += ".exe"
    }
    
    try {
        Write-Status "Downloading binary: $binaryName"
        Invoke-WebRequest -Uri "$baseUrl/$binaryName" -OutFile $binaryName -ErrorAction Stop
        
        Write-Status "Downloading hash file: $hashName"
        Invoke-WebRequest -Uri "$baseUrl/$hashName" -OutFile $hashName -ErrorAction Stop
        
        Write-Status "Downloading provenance file: $provenanceName"
        Invoke-WebRequest -Uri "$baseUrl/$provenanceName" -OutFile $provenanceName -ErrorAction Stop
        
        # Set script variables
        $Script:BinaryPath = $binaryName
        $Script:HashFile = $hashName
        $Script:ProvenancePath = $provenanceName
        
        Write-Status "✅ Download completed"
        return $true
    } catch {
        Write-Error "Failed to download files: $_"
        return $false
    }
}

# Function to verify SBOM
function Test-SBOM {
    param(
        [string]$Version,
        [string]$Platform,
        [string]$Arch
    )
    
    Write-Header "SBOM Verification"
    
    $sbomBuildJson = "sbom-build-$Platform-$Arch.json"
    $sbomBuildXml = "sbom-build-$Platform-$Arch.xml"
    $sbomImageJson = "sbom-image-$Platform-$Arch.cyclonedx.json"
    
    # Try to download SBOM files if not present and auto-download is enabled
    if ($AutoDownload -and $Version) {
        Write-Status "Downloading SBOM files..."
        $baseUrl = "https://github.com/$GitHubRepo/releases/download/$Version"
        
        # Download build-time SBOMs
        if (-not (Test-Path $sbomBuildJson)) {
            try {
                Invoke-WebRequest -Uri "$baseUrl/$sbomBuildJson" -OutFile $sbomBuildJson -ErrorAction Stop
            } catch {
                Write-Warning "Failed to download $sbomBuildJson : $_"
            }
        }
        if (-not (Test-Path $sbomBuildXml)) {
            try {
                Invoke-WebRequest -Uri "$baseUrl/$sbomBuildXml" -OutFile $sbomBuildXml -ErrorAction SilentlyContinue
            } catch {
                Write-Warning "Failed to download $sbomBuildXml : $_"
            }
        }
        
        # Download binary SBOM (from Syft)
        if (-not (Test-Path $sbomImageJson)) {
            try {
                Invoke-WebRequest -Uri "$baseUrl/$sbomImageJson" -OutFile $sbomImageJson -ErrorAction Stop
            } catch {
                Write-Warning "Failed to download $sbomImageJson : $_"
            }
        }
    }
    
    $sbomVerified = $false
    
    # Verify build-time SBOM (CycloneDX from Gradle)
    if (Test-Path $sbomBuildJson) {
        Write-Status "Verifying build-time SBOM (from Gradle build)..."
        $cycloneDx = Get-Command cyclonedx -ErrorAction SilentlyContinue
        if ($cycloneDx) {
            $result = & cyclonedx validate $sbomBuildJson 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Status "✅ Build-time SBOM JSON validation PASSED"
            } else {
                Write-Error "❌ Build-time SBOM JSON validation FAILED"
                Write-Error $result
            }
        } else {
            Write-Warning "CycloneDX CLI not found, skipping SBOM validation"
        }
        
        # Verify build-time SBOM hash if hash file contains it
        if ((Test-Path $HashFile) -and (Select-String -Path $HashFile -Pattern $sbomBuildJson -Quiet)) {
            Write-Status "Verifying build-time SBOM hash..."
            $sbomHashEntry = Select-String -Path $HashFile -Pattern $sbomBuildJson
            $expectedSbomHash = ($sbomHashEntry.Line -split ':')[1].Trim()
            $calculatedSbomHash = Get-FileHash256 -FilePath $sbomBuildJson
            
            if ($calculatedSbomHash -eq $expectedSbomHash) {
                Write-Status "✅ Build-time SBOM hash verification PASSED"
            } else {
                Write-Error "❌ Build-time SBOM hash verification FAILED"
            }
        }
        $sbomVerified = $true
    } else {
        Write-Warning "Build-time SBOM JSON file not found: $sbomBuildJson"
    }
    
    # Verify binary SBOM (from Syft)
    if (Test-Path $sbomImageJson) {
        Write-Status "Verifying binary SBOM (from Syft binary analysis)..."
        $cycloneDx = Get-Command cyclonedx -ErrorAction SilentlyContinue
        if ($cycloneDx) {
            $result = & cyclonedx validate $sbomImageJson 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Status "✅ Binary SBOM JSON validation PASSED"
            } else {
                Write-Error "❌ Binary SBOM JSON validation FAILED"
                Write-Error $result
            }
        } else {
            Write-Warning "CycloneDX CLI not found, skipping SBOM validation"
        }
        
        # Verify binary SBOM hash if hash file contains it
        if ((Test-Path $HashFile) -and (Select-String -Path $HashFile -Pattern $sbomImageJson -Quiet)) {
            Write-Status "Verifying binary SBOM hash..."
            $sbomHashEntry = Select-String -Path $HashFile -Pattern $sbomImageJson
            $expectedSbomHash = ($sbomHashEntry.Line -split ':')[1].Trim()
            $calculatedSbomHash = Get-FileHash256 -FilePath $sbomImageJson
            
            if ($calculatedSbomHash -eq $expectedSbomHash) {
                Write-Status "✅ Binary SBOM hash verification PASSED"
            } else {
                Write-Error "❌ Binary SBOM hash verification FAILED"
            }
        }
        $sbomVerified = $true
        
        Write-Status "📋 SBOM Types Available:"
        Write-Status "  Build-time SBOM: Gradle dependencies and declared components"
        Write-Status "  Binary SBOM: Binary analysis of final native executable"
    } else {
        Write-Warning "Binary SBOM not found: $sbomImageJson"
    }
    
    
    if (-not $sbomVerified) {
        Write-Warning "No SBOM files found for verification"
    }
}

# Function to show usage
function Show-Usage {
    @"
Cross-Platform Scopes Release Verification Script (PowerShell)

Usage: .\Verify-Release.ps1 [PARAMETERS]

PARAMETERS:
    -Version <string>         Release version to verify (e.g., v1.0.0)
    -BinaryPath <string>      Path to binary file
    -ProvenancePath <string>  Path to provenance file
    -HashFile <string>        Path to hash file
    -AutoDownload             Auto-download release files
    -SkipSLSA                 Skip SLSA verification
    -SkipHash                 Skip hash verification
    -VerifySBOM               Also verify SBOM files
    -PlatformOverride <string> Override platform detection (linux/darwin/win32)
    -ArchOverride <string>    Override architecture detection (x64/arm64)
    -GitHubRepo <string>      GitHub repository (default: kamiazya/scopes)
    -Help                     Show this help message

ENVIRONMENT VARIABLES:
    SCOPES_VERSION             Default version to verify
    SCOPES_BINARY_PATH         Default binary path
    SCOPES_PROVENANCE_PATH     Default provenance file path
    SCOPES_HASH_FILE           Default hash file path
    SCOPES_AUTO_DOWNLOAD       Enable auto-download (true/false)
    SCOPES_VERIFY_SLSA         Enable/disable SLSA verification (default: true, false to skip)
    SCOPES_VERIFY_HASH         Enable/disable hash verification (default: true, false to skip)
    SCOPES_VERIFY_SBOM         Enable/disable SBOM verification (true/false)
    SCOPES_GITHUB_REPO         GitHub repository (owner/repo)
    SCOPES_PLATFORM            Override platform detection
    SCOPES_ARCH                Override architecture detection

EXAMPLES:
    # Using environment variables
    $env:SCOPES_VERSION='v1.0.0'
    $env:SCOPES_AUTO_DOWNLOAD='true'
    .\Verify-Release.ps1

    # Command line parameters
    .\Verify-Release.ps1 -AutoDownload -Version v1.0.0
    .\Verify-Release.ps1 -BinaryPath scopes-v1.0.0-win32-x64.exe -HashFile binary-hash-win32-x64.txt -SkipSLSA
    
    # Mixed environment and parameters
    $env:SCOPES_GITHUB_REPO='your-org/scopes-fork'
    $env:SCOPES_VERIFY_SBOM='true'
    .\Verify-Release.ps1 -AutoDownload -Version v1.0.0

"@
}

# Main function
function Main {
    if ($Help) {
        Show-Usage
        return
    }
    
    Write-Header "Scopes Release Verification"
    
    # Detect platform and architecture
    $platform = if ($PlatformOverride) { $PlatformOverride } else { Get-Platform }
    $arch = if ($ArchOverride) { $ArchOverride } else { Get-Architecture }
    
    # Show current configuration
    Write-Status "Configuration:"
    Write-Status "  Version: $(if ($Version) { $Version } else { '(not set)' })"
    Write-Status "  Platform: $platform-$arch"
    Write-Status "  Repository: $GitHubRepo"
    Write-Status "  Auto-download: $AutoDownload"
    Write-Status "  Skip SLSA: $SkipSLSA"
    Write-Status "  Skip Hash: $SkipHash"
    Write-Status "  Verify SBOM: $VerifySBOM"
    Write-Host ""
    
    if ($platform -eq "unknown" -or $arch -eq "unknown") {
        Write-Error "Unsupported platform or architecture"
        Write-Error "Please specify -PlatformOverride and -ArchOverride manually"
        return 1
    }
    
    # Auto-download if requested
    if ($AutoDownload) {
        if (-not $Version) {
            Write-Error "Version is required for auto-download"
            return 1
        }
        if (-not (Get-ReleaseFiles -Version $Version -Platform $platform -Arch $arch)) {
            return 1
        }
    }
    
    # Use script-level variables if set by auto-download
    if ($Script:BinaryPath) { $BinaryPath = $Script:BinaryPath }
    if ($Script:HashFile) { $HashFile = $Script:HashFile }
    if ($Script:ProvenancePath) { $ProvenancePath = $Script:ProvenancePath }
    
    # Validate required files
    if (-not $SkipHash -and (-not $BinaryPath -or -not $HashFile)) {
        Write-Error "Binary path and hash file are required for hash verification"
        return 1
    }
    
    if (-not $SkipSLSA -and (-not $BinaryPath -or -not $ProvenancePath)) {
        Write-Error "Binary path and provenance file are required for SLSA verification"
        return 1
    }
    
    $overallResult = $true
    
    # Perform hash verification
    if (-not $SkipHash) {
        if (-not (Test-FileHash -BinaryFile $BinaryPath -HashFile $HashFile)) {
            $overallResult = $false
        }
        Write-Host ""
    }
    
    # Perform SLSA verification
    if (-not $SkipSLSA) {
        if (-not (Test-SLSAProvenance -BinaryFile $BinaryPath -ProvenanceFile $ProvenancePath)) {
            $overallResult = $false
        }
        Write-Host ""
    }
    
    # Perform SBOM verification if requested
    if ($VerifySBOM) {
        if ($Version) {
            Test-SBOM -Version $Version -Platform $platform -Arch $arch
        } else {
            Write-Warning "Version required for SBOM verification, skipping"
        }
        Write-Host ""
    }
    
    # Final result
    Write-Header "Verification Results"
    if ($overallResult) {
        Write-Status "🎉 All verifications PASSED!"
        Write-Status "The binary is authentic and can be trusted."
        return 0
    } else {
        Write-Error "💥 One or more verifications FAILED!"
        Write-Error "DO NOT use this binary - it may be compromised."
        return 1
    }
}

# Run main function
exit (Main)