package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.AttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the {@code libs} extension.
 */
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final ArrowLibraryAccessors laccForArrowLibraryAccessors = new ArrowLibraryAccessors(owner);
    private final GraalvmLibraryAccessors laccForGraalvmLibraryAccessors = new GraalvmLibraryAccessors(owner);
    private final KoinLibraryAccessors laccForKoinLibraryAccessors = new KoinLibraryAccessors(owner);
    private final KotestLibraryAccessors laccForKotestLibraryAccessors = new KotestLibraryAccessors(owner);
    private final KotlinLibraryAccessors laccForKotlinLibraryAccessors = new KotlinLibraryAccessors(owner);
    private final KotlinxLibraryAccessors laccForKotlinxLibraryAccessors = new KotlinxLibraryAccessors(owner);
    private final LogbackLibraryAccessors laccForLogbackLibraryAccessors = new LogbackLibraryAccessors(owner);
    private final Slf4jLibraryAccessors laccForSlf4jLibraryAccessors = new Slf4jLibraryAccessors(owner);
    private final SqldelightLibraryAccessors laccForSqldelightLibraryAccessors = new SqldelightLibraryAccessors(owner);
    private final SqliteLibraryAccessors laccForSqliteLibraryAccessors = new SqliteLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, AttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

    /**
     * Dependency provider for <b>clikt</b> with <b>com.github.ajalt.clikt:clikt</b> coordinates and
     * with version reference <b>clikt</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getClikt() {
        return create("clikt");
    }

    /**
     * Dependency provider for <b>konsist</b> with <b>com.lemonappdev:konsist</b> coordinates and
     * with version reference <b>konsist</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getKonsist() {
        return create("konsist");
    }

    /**
     * Dependency provider for <b>kulid</b> with <b>com.github.guepardoapps:kulid</b> coordinates and
     * with version reference <b>kulid</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getKulid() {
        return create("kulid");
    }

    /**
     * Dependency provider for <b>mockk</b> with <b>io.mockk:mockk</b> coordinates and
     * with version reference <b>mockk</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getMockk() {
        return create("mockk");
    }

    /**
     * Group of libraries at <b>arrow</b>
     */
    public ArrowLibraryAccessors getArrow() {
        return laccForArrowLibraryAccessors;
    }

    /**
     * Group of libraries at <b>graalvm</b>
     */
    public GraalvmLibraryAccessors getGraalvm() {
        return laccForGraalvmLibraryAccessors;
    }

    /**
     * Group of libraries at <b>koin</b>
     */
    public KoinLibraryAccessors getKoin() {
        return laccForKoinLibraryAccessors;
    }

    /**
     * Group of libraries at <b>kotest</b>
     */
    public KotestLibraryAccessors getKotest() {
        return laccForKotestLibraryAccessors;
    }

    /**
     * Group of libraries at <b>kotlin</b>
     */
    public KotlinLibraryAccessors getKotlin() {
        return laccForKotlinLibraryAccessors;
    }

    /**
     * Group of libraries at <b>kotlinx</b>
     */
    public KotlinxLibraryAccessors getKotlinx() {
        return laccForKotlinxLibraryAccessors;
    }

    /**
     * Group of libraries at <b>logback</b>
     */
    public LogbackLibraryAccessors getLogback() {
        return laccForLogbackLibraryAccessors;
    }

    /**
     * Group of libraries at <b>slf4j</b>
     */
    public Slf4jLibraryAccessors getSlf4j() {
        return laccForSlf4jLibraryAccessors;
    }

    /**
     * Group of libraries at <b>sqldelight</b>
     */
    public SqldelightLibraryAccessors getSqldelight() {
        return laccForSqldelightLibraryAccessors;
    }

    /**
     * Group of libraries at <b>sqlite</b>
     */
    public SqliteLibraryAccessors getSqlite() {
        return laccForSqliteLibraryAccessors;
    }

    /**
     * Group of versions at <b>versions</b>
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Group of bundles at <b>bundles</b>
     */
    public BundleAccessors getBundles() {
        return baccForBundleAccessors;
    }

    /**
     * Group of plugins at <b>plugins</b>
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class ArrowLibraryAccessors extends SubDependencyFactory {

        public ArrowLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>io.arrow-kt:arrow-core</b> coordinates and
         * with version reference <b>arrow</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("arrow.core");
        }

    }

    public static class GraalvmLibraryAccessors extends SubDependencyFactory {

        public GraalvmLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>sdk</b> with <b>org.graalvm.sdk:graal-sdk</b> coordinates and
         * with version reference <b>graalvm.sdk</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSdk() {
            return create("graalvm.sdk");
        }

    }

    public static class KoinLibraryAccessors extends SubDependencyFactory {

        public KoinLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>bom</b> with <b>io.insert-koin:koin-bom</b> coordinates and
         * with version reference <b>koin.bom</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getBom() {
            return create("koin.bom");
        }

        /**
         * Dependency provider for <b>core</b> with <b>io.insert-koin:koin-core</b> coordinates and
         * with <b>no version specified</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("koin.core");
        }

        /**
         * Dependency provider for <b>test</b> with <b>io.insert-koin:koin-test</b> coordinates and
         * with <b>no version specified</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTest() {
            return create("koin.test");
        }

    }

    public static class KotestLibraryAccessors extends SubDependencyFactory {
        private final KotestAssertionsLibraryAccessors laccForKotestAssertionsLibraryAccessors = new KotestAssertionsLibraryAccessors(owner);
        private final KotestRunnerLibraryAccessors laccForKotestRunnerLibraryAccessors = new KotestRunnerLibraryAccessors(owner);

        public KotestLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>property</b> with <b>io.kotest:kotest-property</b> coordinates and
         * with version reference <b>kotest</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getProperty() {
            return create("kotest.property");
        }

        /**
         * Group of libraries at <b>kotest.assertions</b>
         */
        public KotestAssertionsLibraryAccessors getAssertions() {
            return laccForKotestAssertionsLibraryAccessors;
        }

        /**
         * Group of libraries at <b>kotest.runner</b>
         */
        public KotestRunnerLibraryAccessors getRunner() {
            return laccForKotestRunnerLibraryAccessors;
        }

    }

    public static class KotestAssertionsLibraryAccessors extends SubDependencyFactory {

        public KotestAssertionsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>arrow</b> with <b>io.kotest.extensions:kotest-assertions-arrow</b> coordinates and
         * with version <b>2.0.0</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getArrow() {
            return create("kotest.assertions.arrow");
        }

        /**
         * Dependency provider for <b>core</b> with <b>io.kotest:kotest-assertions-core</b> coordinates and
         * with version reference <b>kotest</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("kotest.assertions.core");
        }

    }

    public static class KotestRunnerLibraryAccessors extends SubDependencyFactory {

        public KotestRunnerLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>junit5</b> with <b>io.kotest:kotest-runner-junit5</b> coordinates and
         * with version reference <b>kotest</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJunit5() {
            return create("kotest.runner.junit5");
        }

    }

    public static class KotlinLibraryAccessors extends SubDependencyFactory {

        public KotlinLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>stdlib</b> with <b>org.jetbrains.kotlin:kotlin-stdlib</b> coordinates and
         * with <b>no version specified</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getStdlib() {
            return create("kotlin.stdlib");
        }

    }

    public static class KotlinxLibraryAccessors extends SubDependencyFactory {
        private final KotlinxCoroutinesLibraryAccessors laccForKotlinxCoroutinesLibraryAccessors = new KotlinxCoroutinesLibraryAccessors(owner);
        private final KotlinxSerializationLibraryAccessors laccForKotlinxSerializationLibraryAccessors = new KotlinxSerializationLibraryAccessors(owner);

        public KotlinxLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>datetime</b> with <b>org.jetbrains.kotlinx:kotlinx-datetime</b> coordinates and
         * with version reference <b>kotlinx.datetime</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getDatetime() {
            return create("kotlinx.datetime");
        }

        /**
         * Group of libraries at <b>kotlinx.coroutines</b>
         */
        public KotlinxCoroutinesLibraryAccessors getCoroutines() {
            return laccForKotlinxCoroutinesLibraryAccessors;
        }

        /**
         * Group of libraries at <b>kotlinx.serialization</b>
         */
        public KotlinxSerializationLibraryAccessors getSerialization() {
            return laccForKotlinxSerializationLibraryAccessors;
        }

    }

    public static class KotlinxCoroutinesLibraryAccessors extends SubDependencyFactory {

        public KotlinxCoroutinesLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>org.jetbrains.kotlinx:kotlinx-coroutines-core</b> coordinates and
         * with version reference <b>kotlinx.coroutines</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("kotlinx.coroutines.core");
        }

        /**
         * Dependency provider for <b>test</b> with <b>org.jetbrains.kotlinx:kotlinx-coroutines-test</b> coordinates and
         * with version reference <b>kotlinx.coroutines</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTest() {
            return create("kotlinx.coroutines.test");
        }

    }

    public static class KotlinxSerializationLibraryAccessors extends SubDependencyFactory {

        public KotlinxSerializationLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>json</b> with <b>org.jetbrains.kotlinx:kotlinx-serialization-json</b> coordinates and
         * with version reference <b>kotlinx.serialization</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJson() {
            return create("kotlinx.serialization.json");
        }

    }

    public static class LogbackLibraryAccessors extends SubDependencyFactory {

        public LogbackLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>classic</b> with <b>ch.qos.logback:logback-classic</b> coordinates and
         * with version reference <b>logback</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getClassic() {
            return create("logback.classic");
        }

    }

    public static class Slf4jLibraryAccessors extends SubDependencyFactory {

        public Slf4jLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>org.slf4j:slf4j-api</b> coordinates and
         * with version reference <b>slf4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getApi() {
            return create("slf4j.api");
        }

        /**
         * Dependency provider for <b>simple</b> with <b>org.slf4j:slf4j-simple</b> coordinates and
         * with version reference <b>slf4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSimple() {
            return create("slf4j.simple");
        }

    }

    public static class SqldelightLibraryAccessors extends SubDependencyFactory {
        private final SqldelightDialectLibraryAccessors laccForSqldelightDialectLibraryAccessors = new SqldelightDialectLibraryAccessors(owner);
        private final SqldelightDriverLibraryAccessors laccForSqldelightDriverLibraryAccessors = new SqldelightDriverLibraryAccessors(owner);
        private final SqldelightNativeLibraryAccessors laccForSqldelightNativeLibraryAccessors = new SqldelightNativeLibraryAccessors(owner);

        public SqldelightLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>coroutines</b> with <b>app.cash.sqldelight:coroutines-extensions</b> coordinates and
         * with version reference <b>sqldelight</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCoroutines() {
            return create("sqldelight.coroutines");
        }

        /**
         * Group of libraries at <b>sqldelight.dialect</b>
         */
        public SqldelightDialectLibraryAccessors getDialect() {
            return laccForSqldelightDialectLibraryAccessors;
        }

        /**
         * Group of libraries at <b>sqldelight.driver</b>
         */
        public SqldelightDriverLibraryAccessors getDriver() {
            return laccForSqldelightDriverLibraryAccessors;
        }

        /**
         * Group of libraries at <b>sqldelight.native</b>
         */
        public SqldelightNativeLibraryAccessors getNative() {
            return laccForSqldelightNativeLibraryAccessors;
        }

    }

    public static class SqldelightDialectLibraryAccessors extends SubDependencyFactory {

        public SqldelightDialectLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>sqlite</b> with <b>app.cash.sqldelight:sqlite-3-38-dialect</b> coordinates and
         * with version reference <b>sqldelight</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSqlite() {
            return create("sqldelight.dialect.sqlite");
        }

    }

    public static class SqldelightDriverLibraryAccessors extends SubDependencyFactory {

        public SqldelightDriverLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>sqlite</b> with <b>app.cash.sqldelight:sqlite-driver</b> coordinates and
         * with version reference <b>sqldelight</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSqlite() {
            return create("sqldelight.driver.sqlite");
        }

    }

    public static class SqldelightNativeLibraryAccessors extends SubDependencyFactory {

        public SqldelightNativeLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>driver</b> with <b>app.cash.sqldelight:native-driver</b> coordinates and
         * with version reference <b>sqldelight</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getDriver() {
            return create("sqldelight.native.driver");
        }

    }

    public static class SqliteLibraryAccessors extends SubDependencyFactory {

        public SqliteLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>jdbc</b> with <b>org.xerial:sqlite-jdbc</b> coordinates and
         * with version reference <b>sqlite</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJdbc() {
            return create("sqlite.jdbc");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        private final GraalvmVersionAccessors vaccForGraalvmVersionAccessors = new GraalvmVersionAccessors(providers, config);
        private final GradleVersionAccessors vaccForGradleVersionAccessors = new GradleVersionAccessors(providers, config);
        private final KoinVersionAccessors vaccForKoinVersionAccessors = new KoinVersionAccessors(providers, config);
        private final KotlinxVersionAccessors vaccForKotlinxVersionAccessors = new KotlinxVersionAccessors(providers, config);
        private final KtlintVersionAccessors vaccForKtlintVersionAccessors = new KtlintVersionAccessors(providers, config);
        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>arrow</b> with value <b>2.1.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getArrow() { return getVersion("arrow"); }

        /**
         * Version alias <b>clikt</b> with value <b>4.4.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getClikt() { return getVersion("clikt"); }

        /**
         * Version alias <b>detekt</b> with value <b>1.23.8</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getDetekt() { return getVersion("detekt"); }

        /**
         * Version alias <b>konsist</b> with value <b>0.17.3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getKonsist() { return getVersion("konsist"); }

        /**
         * Version alias <b>kotest</b> with value <b>6.0.3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getKotest() { return getVersion("kotest"); }

        /**
         * Version alias <b>kotlin</b> with value <b>2.2.10</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getKotlin() { return getVersion("kotlin"); }

        /**
         * Version alias <b>kulid</b> with value <b>2.0.0.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getKulid() { return getVersion("kulid"); }

        /**
         * Version alias <b>logback</b> with value <b>1.5.18</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLogback() { return getVersion("logback"); }

        /**
         * Version alias <b>mockk</b> with value <b>1.14.5</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getMockk() { return getVersion("mockk"); }

        /**
         * Version alias <b>slf4j</b> with value <b>2.0.17</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSlf4j() { return getVersion("slf4j"); }

        /**
         * Version alias <b>spotless</b> with value <b>7.2.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSpotless() { return getVersion("spotless"); }

        /**
         * Version alias <b>sqldelight</b> with value <b>2.1.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSqldelight() { return getVersion("sqldelight"); }

        /**
         * Version alias <b>sqlite</b> with value <b>3.50.3.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSqlite() { return getVersion("sqlite"); }

        /**
         * Group of versions at <b>versions.graalvm</b>
         */
        public GraalvmVersionAccessors getGraalvm() {
            return vaccForGraalvmVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.gradle</b>
         */
        public GradleVersionAccessors getGradle() {
            return vaccForGradleVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.koin</b>
         */
        public KoinVersionAccessors getKoin() {
            return vaccForKoinVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.kotlinx</b>
         */
        public KotlinxVersionAccessors getKotlinx() {
            return vaccForKotlinxVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.ktlint</b>
         */
        public KtlintVersionAccessors getKtlint() {
            return vaccForKtlintVersionAccessors;
        }

    }

    public static class GraalvmVersionAccessors extends VersionFactory  {

        public GraalvmVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>graalvm.buildtools</b> with value <b>0.11.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getBuildtools() { return getVersion("graalvm.buildtools"); }

        /**
         * Version alias <b>graalvm.sdk</b> with value <b>23.0.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSdk() { return getVersion("graalvm.sdk"); }

    }

    public static class GradleVersionAccessors extends VersionFactory  {

        public GradleVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>gradle.develocity</b> with value <b>4.1.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getDevelocity() { return getVersion("gradle.develocity"); }

    }

    public static class KoinVersionAccessors extends VersionFactory  {

        public KoinVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>koin.bom</b> with value <b>4.1.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getBom() { return getVersion("koin.bom"); }

    }

    public static class KotlinxVersionAccessors extends VersionFactory  {

        public KotlinxVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>kotlinx.coroutines</b> with value <b>1.10.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getCoroutines() { return getVersion("kotlinx.coroutines"); }

        /**
         * Version alias <b>kotlinx.datetime</b> with value <b>0.6.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getDatetime() { return getVersion("kotlinx.datetime"); }

        /**
         * Version alias <b>kotlinx.serialization</b> with value <b>1.9.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSerialization() { return getVersion("kotlinx.serialization"); }

    }

    public static class KtlintVersionAccessors extends VersionFactory  {

        public KtlintVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>ktlint.gradle</b> with value <b>13.1.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getGradle() { return getVersion("ktlint.gradle"); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, AttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

        /**
         * Dependency bundle provider for <b>kotest</b> which contains the following dependencies:
         * <ul>
         *    <li>io.kotest:kotest-runner-junit5</li>
         *    <li>io.kotest:kotest-assertions-core</li>
         *    <li>io.kotest.extensions:kotest-assertions-arrow</li>
         *    <li>io.kotest:kotest-property</li>
         * </ul>
         * <p>
         * This bundle was declared in catalog libs.versions.toml
         */
        public Provider<ExternalModuleDependencyBundle> getKotest() {
            return createBundle("kotest");
        }

    }

    public static class PluginAccessors extends PluginFactory {
        private final GraalvmPluginAccessors paccForGraalvmPluginAccessors = new GraalvmPluginAccessors(providers, config);
        private final GradlePluginAccessors paccForGradlePluginAccessors = new GradlePluginAccessors(providers, config);
        private final KotlinPluginAccessors paccForKotlinPluginAccessors = new KotlinPluginAccessors(providers, config);

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>detekt</b> with plugin id <b>io.gitlab.arturbosch.detekt</b> and
         * with version reference <b>detekt</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getDetekt() { return createPlugin("detekt"); }

        /**
         * Plugin provider for <b>ktlint</b> with plugin id <b>org.jlleitschuh.gradle.ktlint</b> and
         * with version reference <b>ktlint.gradle</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getKtlint() { return createPlugin("ktlint"); }

        /**
         * Plugin provider for <b>spotless</b> with plugin id <b>com.diffplug.spotless</b> and
         * with version reference <b>spotless</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getSpotless() { return createPlugin("spotless"); }

        /**
         * Plugin provider for <b>sqldelight</b> with plugin id <b>app.cash.sqldelight</b> and
         * with version reference <b>sqldelight</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getSqldelight() { return createPlugin("sqldelight"); }

        /**
         * Group of plugins at <b>plugins.graalvm</b>
         */
        public GraalvmPluginAccessors getGraalvm() {
            return paccForGraalvmPluginAccessors;
        }

        /**
         * Group of plugins at <b>plugins.gradle</b>
         */
        public GradlePluginAccessors getGradle() {
            return paccForGradlePluginAccessors;
        }

        /**
         * Group of plugins at <b>plugins.kotlin</b>
         */
        public KotlinPluginAccessors getKotlin() {
            return paccForKotlinPluginAccessors;
        }

    }

    public static class GraalvmPluginAccessors extends PluginFactory {

        public GraalvmPluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>graalvm.native</b> with plugin id <b>org.graalvm.buildtools.native</b> and
         * with version reference <b>graalvm.buildtools</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getNative() { return createPlugin("graalvm.native"); }

    }

    public static class GradlePluginAccessors extends PluginFactory {

        public GradlePluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>gradle.develocity</b> with plugin id <b>com.gradle.develocity</b> and
         * with version reference <b>gradle.develocity</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getDevelocity() { return createPlugin("gradle.develocity"); }

    }

    public static class KotlinPluginAccessors extends PluginFactory {

        public KotlinPluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>kotlin.jvm</b> with plugin id <b>org.jetbrains.kotlin.jvm</b> and
         * with version reference <b>kotlin</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getJvm() { return createPlugin("kotlin.jvm"); }

        /**
         * Plugin provider for <b>kotlin.serialization</b> with plugin id <b>org.jetbrains.kotlin.plugin.serialization</b> and
         * with version reference <b>kotlin</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getSerialization() { return createPlugin("kotlin.serialization"); }

    }

}
