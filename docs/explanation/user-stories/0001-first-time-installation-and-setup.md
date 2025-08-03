# US-001: First-Time Installation and Setup

## User Story

- **As a** developer interested in trying Scopes
- **I want** to install and set up Scopes quickly and easily
- **So that** I can start using it for my task management without friction

## Persona Context

- **User Type**: CLI Tool User / Any Developer
- **Experience Level**: Beginner to Expert (wide range)
- **Context**: Heard about Scopes through documentation, recommendations, or online discussions. Wants to evaluate the tool for their workflow.

## Detailed Scenario

A developer has learned about Scopes and wants to try it out. They expect a smooth installation process similar to other modern CLI tools they use. They want to:
- Install the tool using familiar package managers
- Get immediate feedback that installation succeeded
- Understand basic usage without reading extensive documentation
- Create their first scope to validate the tool works
- Know how to get help when needed

Current pain points with similar tools:
- Complex installation procedures
- Unclear setup requirements
- No clear "next steps" after installation
- Overwhelming initial configuration

## Acceptance Criteria

```gherkin
Feature: First-time installation and setup

Scenario: Install via package manager
    Given I am a developer with npm/homebrew/cargo installed
    When I run the standard installation command
    Then Scopes is installed successfully
    And I can verify the installation with --version
    And I see a success message with next steps

Scenario: First command execution
    Given I have just installed Scopes
    When I run 'scopes' without arguments
    Then I see helpful usage information
    And I see suggestions for getting started
    And I see how to get more detailed help

Scenario: Create first scope
    Given I have Scopes installed
    When I follow the getting started guidance
    Then I can create my first scope successfully
    And I see confirmation of the created scope
    And I understand what I can do next

Scenario: Get help when stuck
    Given I am using Scopes for the first time
    When I use the help system
    Then I can find answers to common questions
    And I can find examples of basic usage
    And I know where to get additional support

Scenario: Handle missing package manager
    Given I am on a system without npm/homebrew/cargo installed
    When I try to install Scopes using instructions
    Then I see a clear error message about missing dependencies
    And I receive guidance on alternative installation methods
    And I am directed to platform-specific installation instructions

Scenario: Handle installation permission errors
    Given I have npm installed but no global install permissions
    When I run 'npm install -g scopes' without sudo
    Then I see a clear permission error message
    And I receive suggestions for resolving the issue
    And I am informed about npx usage as an alternative

Scenario: Handle corrupted installation
    Given I have a partially installed or corrupted Scopes installation
    When I run 'scopes --version'
    Then I see an error indicating the installation is broken
    And I receive instructions for clean reinstallation
    And I am provided troubleshooting resources
      ```typescript

## User Journey

1. **Discovery**: User learns about Scopes and decides to try it
2. **Installation**: User installs via their preferred package manager
3. **Verification**: User confirms installation worked correctly
4. **First Use**: User runs Scopes to understand basic functionality
5. **Initial Success**: User creates their first scope
6. **Exploration**: User understands next steps and available features

```mermaid
---
title: First-Time User Installation Journey
---
journey
        title First-Time Installation and Setup
        section Discovery
          Learn about Scopes     : 3: User
          Decide to try it       : 3: User
        section Installation
          Choose install method  : 4: User
          Run install command    : 5: User, System
          Verify installation    : 5: User, System
        section First Use
          Run 'scopes' command   : 4: User, System
          Read getting started   : 4: User
          Create first scope     : 5: User, System
        section Success
          See scope created      : 5: User, System
          Understand next steps  : 4: User
          Feel confident         : 5: User
      ```typescript

## Success Metrics

- **Installation Success Rate**: >95% of attempts result in working installation
- **Time to First Scope**: Users create first scope within 5 minutes of installation
- **Help System Usage**: Users can find basic answers without external documentation
- **User Retention**: Users who complete first-time setup continue using Scopes

## Dependencies

### Requires
- Package distribution infrastructure (npm, brew, GitHub releases)
- Basic CLI framework with help system
- Simple scope creation functionality

### Enables
- All subsequent user workflows
- User onboarding and adoption
- Community growth and feedback

## Implementation Notes

### Installation Methods
- **npm**: `npm install -g scopes` (primary method for Node.js ecosystem)
- **Homebrew**: `brew install scopes` (macOS users)
- **Cargo**: `cargo install scopes` (if implemented in Rust)
- **GitHub Releases**: Manual download for all platforms
- **Package managers**: apt, yum, pacman for Linux distributions

### First-Run Experience
- Clear welcome message with version info
- Basic usage examples in help output
- Guidance on creating first workspace/scope
- Links to documentation and community resources

### Error Handling
- Clear error messages for common installation issues
- Helpful suggestions for troubleshooting
- Platform-specific guidance when needed
- Contact information for additional support

## Related Stories

This story enables all other user stories by providing the foundation for users to start using Scopes.

