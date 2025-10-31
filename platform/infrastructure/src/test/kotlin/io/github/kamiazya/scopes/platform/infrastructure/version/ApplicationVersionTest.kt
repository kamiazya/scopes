package io.github.kamiazya.scopes.platform.infrastructure.version

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ApplicationVersionTest : DescribeSpec({
    describe("ApplicationVersion") {
        describe("version constants") {
            it("should have a current version") {
                ApplicationVersion.CURRENT_VERSION shouldNotBe null
                ApplicationVersion.CURRENT_VERSION shouldBe "0.1.0"
            }

            it("should have schema versions for each context") {
                ApplicationVersion.SchemaVersions.SCOPE_MANAGEMENT shouldBe 1L
                ApplicationVersion.SchemaVersions.EVENT_STORE shouldBe 1L
                ApplicationVersion.SchemaVersions.DEVICE_SYNCHRONIZATION shouldBe 1L
                ApplicationVersion.SchemaVersions.USER_PREFERENCES shouldBe 1L
            }
        }

        describe("parseVersion") {
            it("should parse semantic version correctly") {
                ApplicationVersion.parseVersion("1.2.3") shouldBe Triple(1, 2, 3)
                ApplicationVersion.parseVersion("10.20.30") shouldBe Triple(10, 20, 30)
                ApplicationVersion.parseVersion("0.1.0") shouldBe Triple(0, 1, 0)
            }

            it("should handle incomplete versions") {
                ApplicationVersion.parseVersion("1") shouldBe Triple(1, 0, 0)
                ApplicationVersion.parseVersion("1.2") shouldBe Triple(1, 2, 0)
                ApplicationVersion.parseVersion("") shouldBe Triple(0, 0, 0)
            }

            it("should handle invalid versions") {
                ApplicationVersion.parseVersion("abc") shouldBe Triple(0, 0, 0)
                ApplicationVersion.parseVersion("1.x.3") shouldBe Triple(1, 0, 3)
                ApplicationVersion.parseVersion("1.2.x") shouldBe Triple(1, 2, 0)
            }
        }

        describe("compareVersions") {
            it("should compare versions correctly") {
                // Equal
                ApplicationVersion.compareVersions("1.2.3", "1.2.3") shouldBe 0

                // Major version difference
                ApplicationVersion.compareVersions("2.0.0", "1.0.0") shouldBe 1
                ApplicationVersion.compareVersions("1.0.0", "2.0.0") shouldBe -1

                // Minor version difference
                ApplicationVersion.compareVersions("1.3.0", "1.2.0") shouldBe 1
                ApplicationVersion.compareVersions("1.2.0", "1.3.0") shouldBe -1

                // Patch version difference
                ApplicationVersion.compareVersions("1.2.4", "1.2.3") shouldBe 1
                ApplicationVersion.compareVersions("1.2.3", "1.2.4") shouldBe -1
            }
        }

        describe("getCurrentMapping") {
            it("should return the latest version mapping") {
                val mapping = ApplicationVersion.getCurrentMapping()
                mapping.appVersion shouldBe "0.1.0"
                mapping.scopeManagementSchema shouldBe 1L
                mapping.eventStoreSchema shouldBe 1L
                mapping.deviceSyncSchema shouldBe 1L
                mapping.userPreferencesSchema shouldBe 1L
            }
        }

        describe("isCompatible") {
            it("should check database compatibility") {
                // Compatible cases
                ApplicationVersion.isCompatible(1L, 1L) shouldBe true  // Same version
                ApplicationVersion.isCompatible(1L, 2L) shouldBe true  // Database older than context

                // Incompatible case
                ApplicationVersion.isCompatible(3L, 2L) shouldBe false // Database newer than context
            }
        }

        describe("versionHistory") {
            it("should contain at least one version mapping") {
                ApplicationVersion.versionHistory.size shouldBe 1
                ApplicationVersion.versionHistory.first().appVersion shouldBe "0.1.0"
            }
        }
    }
})