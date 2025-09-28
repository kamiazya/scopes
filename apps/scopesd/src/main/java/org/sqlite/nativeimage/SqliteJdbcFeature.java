package org.sqlite.nativeimage;

import org.graalvm.nativeimage.hosted.Feature;

/**
 * Stub implementation of SqliteJdbcFeature to satisfy GraalVM native-image build.
 * The real feature is in the multi-release JAR under META-INF/versions/9/,
 * but GraalVM has issues loading it.
 */
public class SqliteJdbcFeature implements Feature {
    
    @Override
    public String getDescription() {
        return "SQLite JDBC Feature (stub)";
    }
    
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        // No-op stub implementation
        // The actual configuration is handled through native-image.properties
    }
}