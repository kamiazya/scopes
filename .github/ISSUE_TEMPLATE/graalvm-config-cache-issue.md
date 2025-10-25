---
name: "[DEPRECATED] GraalVM Native Build Configuration Cache Incompatibility"
about: "[DEPRECATED - No longer applicable] This issue template was for GraalVM Native Image builds, which have been replaced by JAR distribution"
title: "[DEPRECATED] GraalVM native build fails with configuration cache enabled"
labels: deprecated, wontfix
assignees: ''
---

> **⚠️ DEPRECATED**: This issue template is no longer applicable. Scopes has migrated from GraalVM Native Image compilation to JAR distribution. Native Image builds are no longer part of the release process.
>
> For current build issues, please use the standard bug report template.

## Problem Description

The GraalVM native build plugin (v0.11.0) is incompatible with Gradle's configuration cache feature, causing build failures in CI environments.

## Error Message

```
Execution failed for task ':boot:cli-launcher:nativeCompile'.
> Error while evaluating property 'compileOptions.excludeConfigArgs' of task ':boot:cli-launcher:nativeCompile'.
  > Failed to calculate the value of property 'excludeConfigArgs'.
    > Cannot invoke "org.gradle.api.artifacts.ConfigurationContainer.getByName(String)" because "configurations" is null
```

## Root Cause

The GraalVM plugin internally references `ConfigurationContainer` at execution time, which becomes null during configuration cache serialization/deserialization. This is a known issue tracked upstream:
- https://github.com/graalvm/native-build-tools/issues/477

## Current Workarounds Attempted

1. ✅ Added `notCompatibleWithConfigurationCache()` to native tasks
  - Result: Warnings are logged but error persists
  
2. ✅ Wrapped configuration in `afterEvaluate` block
  - Result: No effect on the error

3. ✅ Disabled configuration cache for GraalVM-related tasks
  - Result: Problems are reported but builds still fail

## Impact

- Native image builds fail in CI (Build workflow)
- All other workflows (Test, Code Quality, Security, Dependency Review) pass successfully
- Configuration cache provides significant performance benefits for non-native builds

## Temporary Solution

The Build workflow has been disabled for native compilation until the upstream issue is resolved.

## Resolution Criteria

This issue can be closed when:
1. GraalVM native-build-tools plugin supports configuration cache (track upstream issue)
2. OR an alternative native build solution is implemented
3. OR configuration cache can be selectively disabled for native build tasks only

## Action Items

- [ ] Monitor upstream issue: https://github.com/graalvm/native-build-tools/issues/477
- [ ] Test new GraalVM plugin versions when released
- [ ] Re-enable native builds when compatibility is fixed
- [ ] Consider alternative solutions if upstream fix is delayed

## Related Files

- `presentation-cli/build.gradle.kts` - Contains GraalVM configuration
- `.github/workflows/build.yml` - Build workflow with native compilation
- `gradle.properties` - Configuration cache settings

## References

- [GraalVM Native Build Tools Issue #477](https://github.com/graalvm/native-build-tools/issues/477)
- [Gradle Configuration Cache Documentation](https://docs.gradle.org/current/userguide/configuration_cache.html)
- [GraalVM Native Build Tools Documentation](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)
