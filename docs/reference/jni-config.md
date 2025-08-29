# JNI Configuration Reference

## Overview

The `jni-config.json` file in `/apps/scopes/src/main/resources/META-INF/native-image/` is required for GraalVM native image compilation when using SQLite JDBC driver.

## Why It's Necessary

1. **Native Library Access**: SQLite JDBC uses JNI (Java Native Interface) to communicate with the native SQLite library
2. **GraalVM Requirements**: GraalVM's native-image tool needs to know which JNI calls will be made at runtime
3. **Reflection Configuration**: SQLite JDBC uses reflection to load native methods dynamically

## Configuration Breakdown

### Core Java Classes
```json
{
  "name": "java.lang.System",
  "methods": [
    { "name": "loadLibrary", "parameterTypes": ["java.lang.String"] }
  ]
}
```
Required for loading the native SQLite library at runtime.

### SQLite JDBC Classes

#### NativeDB
The main JNI interface to SQLite:
```json
{
  "name": "org.sqlite.core.NativeDB",
  "allDeclaredConstructors": true,
  "methods": [
    { "name": "_open_utf8", "parameterTypes": ["byte[]", "int"] },
    { "name": "_prepare_utf8", "parameterTypes": ["long", "byte[]"] },
    // ... all native methods
  ]
}
```

#### Function Support
For custom SQL functions:
```json
{
  "name": "org.sqlite.Function",
  "allDeclaredConstructors": true,
  "allDeclaredMethods": true
}
```

#### JDBC Driver Classes
```json
{
  "name": "org.sqlite.JDBC",
  "allDeclaredConstructors": true
},
{
  "name": "org.sqlite.SQLiteConnection",
  "allDeclaredConstructors": true
}
```

## Maintenance

### When to Update

1. **SQLite JDBC Version Changes**: Different versions may use different native methods
2. **New Features**: Using SQLite features not previously used (e.g., custom collations, functions)
3. **Build Failures**: Missing JNI configuration causes runtime errors in native image

### How to Generate

Use GraalVM's tracing agent during testing:
```bash
./gradlew run -Pagent=true
# Exercise all SQLite features used by the app
# Configuration will be generated in build directory
```

### Validation

Run native image tests to ensure all JNI calls work:
```bash
./gradlew nativeTest
```

## Common Issues

1. **UnsatisfiedLinkError**: Missing native method registration
  - Solution: Add the method to jni-config.json

2. **ClassNotFoundException**: Missing class registration
  - Solution: Add the class with appropriate access flags

3. **NoSuchMethodError**: Method signature mismatch
  - Solution: Verify parameter types match exactly

## Security Considerations

- Only expose necessary methods and classes
- Avoid `allPublicMethods` unless required
- Review configuration when updating dependencies

## References

- [GraalVM JNI Documentation](https://www.graalvm.org/latest/reference-manual/native-image/dynamic-features/JNI/)
- [SQLite JDBC Documentation](https://github.com/xerial/sqlite-jdbc)
- [Native Image Configuration](https://www.graalvm.org/latest/reference-manual/native-image/overview/BuildConfiguration/)
