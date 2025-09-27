---
"scopes": patch
---

feat: replace individual assets with bundled packages for easier downloads

Replace the previous 28 individual release assets with organized bundle packages to eliminate download confusion. Users now choose from:
- 6 platform-specific bundles (~20MB each) containing binary, installer, SBOM, and verification files
- 1 unified offline package (~260MB) for enterprise/multi-platform deployments
- SLSA provenance for supply chain security

This provides 92% reduction in download size for most users while maintaining all existing security features (SLSA Level 3, SHA256 verification) and preparing for future daemon binary distribution.