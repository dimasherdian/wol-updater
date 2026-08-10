# WoL Updater — Development Guide

## Environment

### IDE
Primary IDE: Antigravity.

The project must remain compatible with standard Java workflows and must not depend on Antigravity-specific APIs.

### Java
- Eclipse Temurin JDK 17
- Current development JDK: Temurin 17.0.19
- Do not require Oracle JDK or Oracle-specific APIs.

## Build

Use Maven Wrapper. Do NOT require globally installed Maven.

Windows:

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd test
```

Commit and keep:
- `mvnw`
- `mvnw.cmd`
- `.mvn/`

Do not ignore Maven Wrapper files.

## Repository

```text
wol-updater/
├── .gitignore
├── README.md
├── PROJECT.md
├── DEVELOPMENT.md
├── AGENTS.md
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/com/wol/updater/
│   │   └── resources/
│   └── test/java/
└── re/
    ├── original/
    ├── extracted/
    └── decompiled/
```

`re/` is local reverse-engineering material and MUST NOT be committed.

## Reverse Engineering

Expected structure:

```text
re/
├── original/
│   └── Wars of Liberty Updater.exe
├── extracted/
└── decompiled/
```

Do not put original WoL binaries, proprietary assets, extracted original JARs, or decompiled proprietary source in the public repository. Do not copy decompiled proprietary implementation directly into the new application.

Purpose of reverse engineering:
- Application architecture
- Update protocol
- Version detection
- Manifest format
- Download behavior
- Installation behavior
- File formats
- Dependencies
- Error handling

Then implement clean, independently written code where appropriate.

## Known Findings

The original updater is a Windows `.exe` containing a Java application.

Previously identified:
- `wolUpdater.Updater`
- `wolUpdater.UpdaterUI`
- `org.eclipse.jdt.internal.jarinjarloader.JarRsrcLoader`

Observed libraries/components:
- Apache Commons Compress
- XZ
- Guava
- Commons IO
- JNA

The original updater was successfully run with Temurin Java 17 after resolving its original JAR/dependency packaging. Do not assume Oracle Java is required.

Observed UI behavior includes:

```text
Wars of Liberty Updater - v1.4
Current Wars of Liberty Installation: [path]
Fetching update info file...
Wars of Liberty version 1.0.15d detected
Downloading update #1 of 28
```

This suggests installation detection, version detection, update-info retrieval, update planning, sequential downloads, and installation. The exact server protocol/manifest format must be verified, not assumed.

## Coding Standards

Use:
- Java 17
- Clear names
- Small focused classes
- Interfaces when they provide real value
- Unit tests
- Minimal justified dependencies

Avoid:
- Giant classes
- Static global state
- UI-driven business logic
- Hard-coded absolute/user paths
- Hard-coded future version lists
- Credentials/API keys
- Unnecessary frameworks

Prefer:
- `Path`
- `Files`
- `HttpClient`
- Java standard library
- Maven dependencies only when justified

## Git / Licensing

Public repository should contain original project code, tests, documentation, Maven configuration, Maven Wrapper, and build configuration.

Do NOT commit:
- Original WoL updater executable
- WoL game files/assets
- Extracted original JARs
- Decompiled original source
- Decompiled proprietary libraries
- Local game installations
- Credentials/API keys
- Local environment configuration

The project is unofficial. Do not claim ownership of WoL IP. MIT may be used for original code if proprietary WoL code/assets are not included. If licensing is uncertain, do not add the component until understood.

README must state that the project is unofficial and not affiliated with or endorsed by the WoL development team.

## .gitignore Minimum

```gitignore
target/
*.class
*.log
.vscode/
.idea/
*.iml
.classpath
.project
.settings/
.DS_Store
Thumbs.db
.env
.env.*
/re/
```

## Development Phases

### Phase 1 — Environment
Verify Java 17, `javac` 17, Git, Maven Wrapper, and Antigravity Java tooling. Do not install global Maven.

### Phase 2 — Project Skeleton
Verify `pom.xml`, Maven Wrapper, source/test directories, README, and `.gitignore`.

Run:

```powershell
.\mvnw.cmd test
```

Initial commit:

```text
chore: initialize Maven Java 17 project
```

### Phase 3 — Reverse Engineering
Analyze `re/original/` with 7-Zip, Java decompiler, `javap`, and suitable static-analysis tools. Document findings without committing proprietary extracted files.

### Phase 4 — Protocol Analysis
Understand version detection, update metadata/list, download mechanism, package format, and installation mechanism. Do not implement assumptions.

### Phase 5 — Core
Implement:
- `InstallationDetector`
- `VersionDetector`
- `UpdateService`
- `UpdatePlan`
- `Downloader`
- `Verifier`
- `Installer`

Build core/CLI before final UI.

### Phase 6 — Compatibility
Add legacy/modern adapters only after evidence confirms the need.

### Phase 7 — UI
Build the modern UI on top of the core.

### Phase 8 — Packaging
Create Windows distribution with bundled Java runtime.

## Development Order

```text
Understand → Document → Design → Implement → Test → Integrate → Package
```

Accuracy is more important than speed.
