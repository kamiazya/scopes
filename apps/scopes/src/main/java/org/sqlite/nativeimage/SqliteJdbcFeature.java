package org.sqlite.nativeimage;

import org.graalvm.nativeimage.hosted.Feature;

/**
 * Dummy implementation to override SQLite JDBC's missing Feature class.
 * This prevents the "SqliteJdbcFeature class not found" error during native image compilation.
 */
public class SqliteJdbcFeature implements Feature {
    @Override
    public String getDescription() {
        return "Dummy SQLite JDBC Feature to prevent native-image errors";
    }
}
