# WoL Updater — Project

## Overview

This is an unofficial, community-made updater for Wars of Liberty (WoL), a fan-made modification for Age of Empires III.

Goals:
- Legacy and modern version compatibility
- Installation and version detection
- Update manifest handling
- Download management
- Integrity verification
- Safe installation
- Modern UI
- Bundled Java runtime for final Windows distribution

This is NOT an official Wars of Liberty project. Do not claim affiliation, endorsement, or ownership by the Wars of Liberty development team.

## Target Architecture

```text
WoL Updater
    ├── UI
    └── Update Core
         ├── Version System
         ├── Downloader
         └── Installer
              └── Legacy / Modern adapters when evidence requires them
```

Keep UI and update business logic separate.

## Compatibility

Conceptually:

```text
UpdateSource
    ├── LegacyUpdateSource
    └── ModernUpdateSource
```

Both should produce a common representation such as `UpdatePlan`. Do not create legacy/modern implementations until reverse engineering confirms that the formats actually differ.

## Version Handling

Do not compare versions using simple string comparison. Observed examples include `1.0.15d` and `1.0.16`. Build a dedicated version representation/comparator based on actual observed WoL formats.

## Update Flow

```text
Detect Installation
    ↓
Detect Installed Version
    ↓
Fetch Update Information
    ↓
Parse Update Information
    ↓
Create UpdatePlan
    ↓
Download
    ↓
Verify
    ↓
Stage
    ↓
Backup if necessary
    ↓
Install
    ↓
Verify Installation
    ↓
Complete
```

Avoid overwriting the live installation before downloads and validation succeed.

## Integrity

If the update source provides hashes/checksums, verify them before installation. Prefer SHA-256. Never invent verification data.

## UI Goals

Eventually provide:
- Current and available versions
- Update status
- Download progress
- Update number / total
- Download speed
- ETA
- Errors
- Cancel
- Completion state
- Play/Launch where appropriate

The UI consumes state/events from the update core and does not implement the update algorithm.

## Runtime

For final Windows distribution, prefer bundling a Java runtime so users do not need to install Java separately. This is a later packaging phase.
