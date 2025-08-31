# Duration Aspects Usage Guide

This guide explains how to use Duration aspects in Scopes for time-based scope management. Duration aspects use the ISO 8601 duration format, providing precise and standardized time tracking.

## Overview

Duration aspects allow you to track time-related information for scopes such as:
- **Estimated effort** (`estimatedTime`)
- **Actual time spent** (`actualTime`)
- **Remaining work** (`remainingTime`)
- **Time budgets** (`timeBudget`)
- **Deadlines relative to creation** (`deadline`)

## ISO 8601 Duration Format

Duration aspects use ISO 8601 duration format, which provides precise and international-standard time representation.

### Basic Format Structure
```
P[n]Y[n]M[n]DT[n]H[n]M[n]S
```

Where:
- `P` = Period designator (required, must be first)
- `Y` = Years
- `M` = Months (before T)
- `D` = Days
- `T` = Time designator (required before time components)
- `H` = Hours
- `M` = Minutes (after T)
- `S` = Seconds

### Common Duration Examples

#### Basic Time Units
```bash
# Minutes
"PT30M"     # 30 minutes
"PT45M"     # 45 minutes

# Hours
"PT1H"      # 1 hour
"PT2H"      # 2 hours
"PT8H"      # 8 hours (1 work day)

# Days
"P1D"       # 1 day (24 hours)
"P3D"       # 3 days
"P1W"       # 1 week (7 days)

# Seconds
"PT30S"     # 30 seconds
"PT90S"     # 90 seconds (1.5 minutes)
```

#### Combined Durations
```bash
# Hours and minutes
"PT2H30M"   # 2 hours and 30 minutes
"PT1H15M"   # 1 hour and 15 minutes
"PT8H30M"   # 8.5 hours

# Days and hours
"P1DT2H"    # 1 day and 2 hours
"P2DT8H"    # 2 days and 8 hours

# Complex combinations
"P1DT2H30M"     # 1 day, 2 hours, 30 minutes
"P2DT3H15M30S"  # 2 days, 3 hours, 15 minutes, 30 seconds
"P1W2DT4H"      # 1 week, 2 days, 4 hours
```

## Setting Up Duration Aspects

### Define Duration Aspects
```bash
# Define estimation aspect
$ scopes aspect define estimatedTime --type duration --description "Estimated effort"
✓ Defined aspect 'estimatedTime' (duration): ISO 8601 durations

# Define actual time tracking
$ scopes aspect define actualTime --type duration --description "Actual time spent"
✓ Defined aspect 'actualTime' (duration): ISO 8601 durations

# Define time budget
$ scopes aspect define timeBudget --type duration --description "Time budget allocated"
✓ Defined aspect 'timeBudget' (duration): ISO 8601 durations
```

### Set Duration Values
```bash
# Set estimated time for a scope
$ scopes aspect set auth-feature estimatedTime=PT8H
✓ Set aspects on scope 'auth-feature':
    estimatedTime: PT8H

# Set multiple duration aspects
$ scopes aspect set login-ui estimatedTime=PT4H actualTime=PT2H30M
✓ Set aspects on scope 'login-ui':
    estimatedTime: PT4H
    actualTime: PT2H30M

# Set weekly sprint budget
$ scopes aspect set sprint-42 timeBudget=P1W
✓ Set aspects on scope 'sprint-42':
    timeBudget: P1W
```

## Querying Duration Aspects

### Basic Duration Queries
```bash
# Find quick tasks (less than 2 hours)
$ scopes list -a "estimatedTime<PT2H"
Found 8 scopes with estimatedTime<PT2H:

form-validation      Add form validation           estimatedTime=PT1H30M
fix-typo            Fix documentation typo        estimatedTime=PT15M
update-deps         Update dependencies           estimatedTime=PT45M

# Find day-long tasks
$ scopes list -a "estimatedTime>=P1D"
Found 3 scopes with estimatedTime>=P1D:

redesign-ui         Complete UI redesign          estimatedTime=P3D
data-migration      Migrate user data             estimatedTime=P2D
security-audit      Security audit                estimatedTime=P1W

# Find work within time budget
$ scopes list -a "actualTime<=PT8H"
Found 12 scopes with actualTime<=PT8H:

auth-feature        Implement authentication       actualTime=PT6H30M
login-form          Design login form              actualTime=PT3H
password-reset      Password reset flow            actualTime=PT4H45M
```

### Advanced Duration Queries
```bash
# Tasks that took longer than estimated
$ scopes list -a "actualTime>estimatedTime"

# Tasks completed within 4-hour sessions
$ scopes list -a "actualTime>=PT2H AND actualTime<=PT4H"

# Weekly tasks that are quick wins
$ scopes list -a "estimatedTime<=PT2H AND timeBudget<=P1W"

# Over-budget tasks
$ scopes list -a "actualTime>timeBudget"

# Complex time-based planning query
$ scopes list -a "(estimatedTime<=PT4H AND priority>=medium) OR (estimatedTime<=PT1H AND priority=low)"
```

## Practical Workflows

### Sprint Planning
```bash
# Create sprint scope with time budget
$ scopes create "Sprint 42" --alias sprint-42
$ scopes aspect set sprint-42 timeBudget=P2W priority=high

# Add tasks with estimates
$ scopes create "User authentication" --parent sprint-42 --alias auth-task
$ scopes aspect set auth-task estimatedTime=PT16H priority=high

$ scopes create "Bug fixes" --parent sprint-42 --alias bug-fixes
$ scopes aspect set bug-fixes estimatedTime=PT8H priority=medium

# Check sprint capacity
$ scopes list -a "parent=sprint-42" --show-totals
Sprint 42 tasks (2 scopes):
Total estimated time: PT24H (3 days)
Sprint budget: P2W (14 days)
Remaining capacity: P11DT8H
```

### Time Tracking
```bash
# Start work session - record actual time
$ scopes aspect set auth-task actualTime=PT2H status=in-progress
✓ Set aspects on scope 'auth-task':
    actualTime: PT2H
    status: in-progress

# Update time after work session
$ scopes aspect set auth-task actualTime=PT4H30M
✓ Set aspects on scope 'auth-task':
    actualTime: PT4H30M

# Mark completion with final time
$ scopes aspect set auth-task actualTime=PT14H status=completed
✓ Set aspects on scope 'auth-task':
    actualTime: PT14H
    status: completed
```

### Daily Planning
```bash
# Find tasks for today (8-hour workday)
$ scopes list -a "estimatedTime<=PT8H AND status=ready"

# Find quick wins for filling time gaps
$ scopes list -a "estimatedTime<=PT30M AND priority>=medium"

# Check what can be finished this week
$ scopes list -a "estimatedTime<=P5D AND status!=completed"

# Find overrunning tasks requiring attention  
$ scopes list -a "actualTime>estimatedTime AND status=in-progress"
```

### Project Reporting
```bash
# Weekly summary context
$ scopes context create "week-summary" --filter "actualTime>PT0S"
$ scopes context switch week-summary

# Generate time reports
$ scopes list --show-summary
Week Summary (15 scopes):
Total actual time: P3DT14H30M
Average per scope: PT6H18M
Completed scopes: 12 of 15

# Find efficiency patterns
$ scopes list -a "actualTime<estimatedTime" --show-aspects
High performers (under-estimated):
login-form          PT2H30M actual vs PT4H estimated    (-37%)
password-reset      PT3H15M actual vs PT5H estimated    (-35%)

$ scopes list -a "actualTime>estimatedTime" --show-aspects  
Over-estimates (need attention):
auth-backend        PT12H actual vs PT8H estimated      (+50%)
ui-components       PT18H actual vs PT12H estimated     (+50%)
```

## Best Practices

### Estimation Guidelines
```bash
# Use consistent granularity
"PT30M"   # 30-minute increments for small tasks
"PT1H"    # 1-hour increments for medium tasks  
"P1D"     # Day increments for large features

# Prefer explicit formats over shortcuts
"PT2H30M" # ✓ Clear and precise
"2.5h"    # ✗ Not ISO 8601 format
```

### Tracking Patterns
```bash
# Start with rough estimates
$ scopes aspect set feature-x estimatedTime=P1D

# Refine as you learn more
$ scopes aspect set feature-x estimatedTime=PT6H

# Track actual time regularly
$ scopes aspect set feature-x actualTime=PT2H    # After 2 hours
$ scopes aspect set feature-x actualTime=PT4H30M # After another session
```

### Query Optimization
```bash
# Combine duration with status for actionable queries
$ scopes list -a "estimatedTime<=PT2H AND status=ready AND priority>=medium"

# Use ranges for time-boxed planning
$ scopes list -a "estimatedTime>=PT1H AND estimatedTime<=PT4H"

# Track velocity over time
$ scopes list -a "actualTime>PT0S AND status=completed" --created-after="last-week"
```

## Common Duration Values Reference

### Development Tasks
```bash
"PT15M"     # Quick fix, typo correction
"PT30M"     # Small bug fix, minor feature
"PT1H"      # Code review, testing
"PT2H"      # Medium feature, refactoring
"PT4H"      # Half-day feature work
"PT8H"      # Full day feature (1 sprint day)
"P1D"       # Large feature (24 hours total)
"P3D"       # Complex feature
"P1W"       # Epic or major feature
"P2W"       # Sprint duration
```

### Meeting and Planning
```bash
"PT30M"     # Standup, brief meeting
"PT1H"      # Regular meeting, code review
"PT2H"      # Sprint planning, architecture session
"PT4H"      # Workshop, training session
```

### Administrative Tasks
```bash
"PT15M"     # Email, quick updates
"PT30M"     # Documentation writing
"PT1H"      # Research, investigation
"PT2H"      # Documentation, reporting
```

## Troubleshooting

### Common Formatting Errors
```bash
# ✗ Invalid formats
"2h"        # Missing PT prefix
"2H30M"     # Missing PT prefix
"P2H"       # Hours must be after T
"PT2h30m"   # Must be uppercase
"1D2H"      # Missing P and T

# ✓ Correct formats
"PT2H"      # 2 hours
"PT2H30M"   # 2 hours 30 minutes
"P1DT2H"    # 1 day 2 hours
"P1W"       # 1 week
```

### Query Debugging
```bash
# Check aspect definition
$ scopes aspect show estimatedTime

# Validate duration format
$ scopes aspect set test-scope estimatedTime=invalid-duration
Error: Invalid duration format 'invalid-duration'. Expected ISO 8601 format (e.g., 'PT2H30M', 'P1D')

# List scopes with duration aspects
$ scopes list -a "estimatedTime>PT0S"
```

## See Also

- [CLI Quick Reference](../reference/cli-quick-reference.md) - Complete command reference
- [Aspect-Based Classification User Story](../explanation/user-stories/0003-aspect-based-scope-classification.md) - Usage scenarios
- [ISO 8601 Duration Standard](https://en.wikipedia.org/wiki/ISO_8601#Durations) - Official specification
