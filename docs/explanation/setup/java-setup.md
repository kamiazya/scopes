# Java Setup Guide for Scopes

Scopes requires **Java 21 or later** to run. This guide covers installation and verification on all supported platforms.

## Quick Start

### Check Current Java Version

```bash
# Check if Java is installed
java -version

# Expected output (version may vary):
# openjdk version "21.0.1" 2023-10-17
# OpenJDK Runtime Environment (build 21.0.1+12-29)
# OpenJDK 64-Bit Server VM (build 21.0.1+12-29, mixed mode, sharing)
```

### Minimum Requirements

- **Java Version**: 21 or later
- **Distribution**: Any Java SE implementation (OpenJDK, Oracle JDK, Temurin, etc.)
- **Architecture**: Must match your system (x64 or ARM64)

## Installation by Platform

### macOS

#### Option 1: Homebrew (Recommended)

```bash
# Install OpenJDK 21
brew install openjdk@21

# Link it to your system
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-21.jdk

# Add to PATH in ~/.zshrc or ~/.bash_profile
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

#### Option 2: Download from Adoptium

1. Visit [Adoptium](https://adoptium.net/)
2. Select:
   - **Version**: 21 - LTS
   - **Operating System**: macOS
   - **Architecture**: x64 or aarch64 (Apple Silicon)
3. Download the PKG installer
4. Run the installer and follow prompts

#### Option 3: SDKMAN! (Advanced)

```bash
# Install SDKMAN!
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java 21
sdk install java 21-tem

# Set as default
sdk default java 21-tem
```

### Linux

#### Debian/Ubuntu

```bash
# Update package list
sudo apt update

# Install OpenJDK 21
sudo apt install openjdk-21-jre

# Verify installation
java -version
```

#### Fedora/RHEL/CentOS

```bash
# Install OpenJDK 21
sudo dnf install java-21-openjdk

# Verify installation
java -version
```

#### Arch Linux

```bash
# Install OpenJDK 21
sudo pacman -S jdk21-openjdk

# Set as default (if multiple Java versions installed)
sudo archlinux-java set java-21-openjdk
```

#### Using Adoptium (All Distributions)

1. Visit [Adoptium](https://adoptium.net/)
2. Select:
   - **Version**: 21 - LTS
   - **Operating System**: Linux
   - **Architecture**: x64 or aarch64
   - **Package Type**: JRE (for runtime only) or JDK (for development)
3. Follow distribution-specific installation instructions

### Windows

#### Option 1: Chocolatey (Recommended for Command Line)

```powershell
# Install Chocolatey if not already installed
# See https://chocolatey.org/install

# Install OpenJDK 21
choco install openjdk21

# Verify installation
java -version
```

#### Option 2: Scoop (Alternative Package Manager)

```powershell
# Install Scoop if not already installed
# See https://scoop.sh

# Install OpenJDK 21
scoop bucket add java
scoop install openjdk21

# Verify installation
java -version
```

#### Option 3: Manual Download from Adoptium

1. Visit [Adoptium](https://adoptium.net/)
2. Select:
   - **Version**: 21 - LTS
   - **Operating System**: Windows
   - **Architecture**: x64 or aarch64
3. Download the MSI installer
4. Run the installer:
   - Check "Add to PATH" option
   - Check "Set JAVA_HOME variable" option
5. Restart terminal/PowerShell
6. Verify: `java -version`

#### Option 4: Oracle JDK (Official)

1. Visit [Oracle Java Downloads](https://www.oracle.com/java/technologies/downloads/)
2. Download Java 21 installer for Windows
3. Run the installer
4. Verify installation

## Verification

After installation, verify Java is correctly configured:

```bash
# Check version
java -version

# Expected: version "21" or higher

# Check JAVA_HOME (optional but recommended)
echo $JAVA_HOME        # Unix/macOS
echo %JAVA_HOME%       # Windows CMD
echo $env:JAVA_HOME    # Windows PowerShell
```

## Troubleshooting

### "java: command not found"

**Cause**: Java is not in your PATH.

**Solution**:

#### macOS/Linux
```bash
# Find Java installation
/usr/libexec/java_home -V  # macOS only
which java                  # Unix/Linux

# Add to PATH in ~/.bashrc, ~/.zshrc, or ~/.bash_profile
export PATH="/path/to/java/bin:$PATH"
export JAVA_HOME="/path/to/java"

# Reload shell configuration
source ~/.zshrc  # or ~/.bashrc
```

#### Windows
1. Open System Properties → Environment Variables
2. Edit "Path" in User or System variables
3. Add Java bin directory (e.g., `C:\Program Files\Java\jdk-21\bin`)
4. Restart terminal

### Java Version Too Old

```bash
# Check installed version
java -version

# If version < 21, install newer version using methods above
```

### Multiple Java Versions Installed

#### macOS - Use `/usr/libexec/java_home`

```bash
# List all installed Java versions
/usr/libexec/java_home -V

# Set default in ~/.zshrc or ~/.bash_profile
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

#### Linux - Use `update-alternatives`

```bash
# List all Java installations
sudo update-alternatives --config java

# Select Java 21 from the list
```

#### Windows - Set JAVA_HOME

1. Open System Properties → Environment Variables
2. Create/Edit "JAVA_HOME" system variable
3. Set to Java 21 installation path (e.g., `C:\Program Files\Java\jdk-21`)
4. Ensure `%JAVA_HOME%\bin` is in PATH before other Java versions

### JAVA_HOME Not Set (Optional)

While Scopes doesn't require JAVA_HOME, some tools benefit from it:

```bash
# macOS/Linux - add to ~/.zshrc or ~/.bashrc
export JAVA_HOME=$(/usr/libexec/java_home)  # macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64  # Linux example

# Windows - Environment Variables
# Set JAVA_HOME to: C:\Program Files\Java\jdk-21
```

## Advanced Configuration

### Using Different Java Distributions

Scopes works with any Java 21+ distribution:
- **OpenJDK**: Open-source reference implementation
- **Eclipse Temurin** (formerly AdoptOpenJDK): Recommended for production
- **Oracle JDK**: Commercial distribution with support
- **Amazon Corretto**: AWS-optimized OpenJDK
- **Microsoft Build of OpenJDK**: Microsoft-supported distribution
- **Azul Zulu**: Enterprise-grade OpenJDK
- **GraalVM**: High-performance JVM (works in standard JVM mode)

All are compatible as long as the version is 21 or later.

> **Note**: Scopes uses JAR distribution and does not require GraalVM Native Image compilation. Any standard Java 21+ JVM will work.

### Docker/Container Environments

```dockerfile
# Example Dockerfile using Scopes with Java 21
FROM eclipse-temurin:21-jre

# Copy Scopes JAR and wrapper
COPY scopes.jar /opt/scopes/lib/scopes.jar
COPY scopes /opt/scopes/bin/scopes

# Add to PATH
ENV PATH="/opt/scopes/bin:$PATH"

# Verify installation
RUN scopes --version

ENTRYPOINT ["scopes"]
```

### CI/CD Environments

```yaml
# GitHub Actions
- name: Setup Java
  uses: actions/setup-java@v4
  with:
    distribution: 'temurin'
    java-version: '21'

# GitLab CI
image: eclipse-temurin:21-jdk

# CircleCI
docker:
  - image: cimg/openjdk:21.0
```

## Performance Tuning (Optional)

For better performance, you can configure JVM options:

### Create scopes.conf (Unix/macOS)

```bash
# ~/.config/scopes/scopes.conf
JAVA_OPTS="-Xmx512m -XX:+UseG1GC"
```

### Create scopes.conf (Windows)

```
# %APPDATA%\scopes\scopes.conf
JAVA_OPTS=-Xmx512m -XX:+UseG1GC
```

### Common JVM Options

```bash
# Memory settings
-Xmx512m          # Maximum heap size
-Xms256m          # Initial heap size

# Garbage Collection
-XX:+UseG1GC      # Use G1 Garbage Collector (default in Java 21)

# Performance
-XX:+TieredCompilation
-XX:TieredStopAtLevel=1  # Fast startup (development)

# Debugging
-verbose:gc       # Print GC details
-XX:+PrintGCDetails
```

## Getting Help

If you encounter issues with Java setup:

1. **Check Scopes Documentation**: [Installation Guide](../installation.md)
2. **Java Documentation**: [OpenJDK Docs](https://openjdk.org/), [Oracle Docs](https://docs.oracle.com/en/java/)
3. **Community Support**: [GitHub Issues](https://github.com/kamiazya/scopes/issues)

## Next Steps

After Java is installed and verified:

1. **Install Scopes**: Follow the [Installation Guide](../../tutorials/getting-started.md)
2. **Verify Scopes**: Run `scopes --version`
3. **Start Using**: `scopes --help` for available commands

## References

- [Adoptium OpenJDK Builds](https://adoptium.net/) - Recommended Java distribution
- [Oracle Java Downloads](https://www.oracle.com/java/technologies/downloads/)
- [OpenJDK Project](https://openjdk.org/)
- [SDKMAN!](https://sdkman.io/) - Java version manager for Unix
- [Homebrew](https://brew.sh/) - Package manager for macOS
