# WoL Updater — Agent Instructions

## Read First

Before making significant changes, read:
- `PROJECT.md`
- `DEVELOPMENT.md`

## Ponytail Methodology & Architecture

- **The Ladder** (stop at first holding rung): 1. YAGNI (skip if not needed) -> 2. Codebase (reuse existing patterns) -> 3. Stdlib -> 4. Native platform features -> 5. Installed dependency -> 6. One-liner -> 7. Minimum working code.
- **Philosophy**: Deletion over addition. Boring over clever. Fewest files possible. Shortest working diff wins after tracing end-to-end data flow.
- **Bug Fixes**: Target root causes, not symptoms. Fix shared functions once rather than patching individual caller paths.
- **Clear Naming** (Self-Documenting): Names must clearly describe WHAT they do and WHY without requiring extra comments. Never add meta-comments explaining your compliance with these rules inside the code.
- **Clean Architecture** (SRP): Strictly separate Presentation/UI, Business Logic, and Data/Storage layers. Respect existing codebase architecture.
- **Functional & Immutability**: Favor pure functions without side effects and immutable data structures (const, final, readonly). Prefer map, filter, and reduce over loops.
- **Guardrails**: NEVER compromise trust-boundary validation, error handling, data-loss prevention, security, or accessibility. Mark deliberate shortcuts with a ponytail comment explaining trade-offs.

## Mandatory Constraints

- Target Java 17
- Use Eclipse Temurin JDK 17
- Do not require Oracle JDK
- Use Maven Wrapper
- Do not require globally installed Maven
- On Windows use `.\mvnw.cmd`
- Do not depend on Antigravity-specific APIs

## Repository Safety

`re/` is local reverse-engineering material and MUST remain Git-ignored.

Never:
- Commit files from `re/`
- Copy original WoL binaries/assets into `src/`
- Copy extracted original JARs into the project
- Copy decompiled proprietary source directly into the project
- Add proprietary WoL assets to the public repository

Use reverse engineering to understand verified behavior, then implement clean code.

## Reverse Engineering Workflow

Before compatibility implementation:

1. Inspect the original updater.
2. Identify relevant classes.
3. Understand control flow.
4. Identify network endpoints.
5. Identify update manifest format.
6. Identify version detection.
7. Identify file/package formats.
8. Identify installation behavior.
9. Document findings.
10. Design the implementation.
11. Implement clean source code.
12. Test.

Never blindly reproduce decompiled code.

Classify findings as:
- **Confirmed**
- **Likely**
- **Unknown**

Do not present assumptions as facts.

## Architecture Rules

Keep separate:
- UI
- Update logic
- Network logic
- File installation
- Version parsing
- Compatibility handling

The UI must not contain update business logic.

Use abstractions only where they provide real value.

## Compatibility

Do not create legacy/modern adapters until reverse engineering confirms the need.

When formats differ, prefer:

```text
UpdateSource
    ├── LegacyUpdateSource
    └── ModernUpdateSource
```

Both should produce a common `UpdatePlan`.

## Version Rules

Never use simple string comparison for WoL versions. Base parsing on observed real formats. Do not hard-code future version lists.

## Update Safety

Prefer:

```text
Download → Verify → Stage → Backup if necessary → Install → Verify
```

Do not overwrite the live installation before downloaded data is validated.

If hashes are provided, verify them. Do not invent verification data.

## Code Quality

Prefer:
- Small focused classes
- Clear names
- Unit tests
- Java standard library
- `Path`
- `Files`
- `HttpClient`
- Minimal justified dependencies

Avoid:
- Giant classes
- Static global state
- UI-driven business logic
- Hard-coded absolute/user paths
- Credentials/API keys
- Unnecessary frameworks

## Change Discipline

Before editing:
1. Inspect the current file.
2. Understand how it is used.
3. Make the smallest reasonable change.
4. Run relevant tests/build.
5. Report what changed.

Do not perform large refactors unless requested or clearly necessary.

Do not create placeholder architecture for functionality that has not been investigated.

## Current Stage

The project is currently in setup/reverse-engineering preparation.

Do NOT implement the complete updater yet.

First verify:
1. Repository structure
2. `pom.xml`
3. Maven Wrapper
4. Java 17
5. `.\mvnw.cmd test`
6. Git status
7. `.gitignore`
8. `re/` is ignored

Only after the clean project skeleton is confirmed should reverse engineering proceed.

## Communication

For reverse-engineering findings, separate:
- Confirmed
- Likely
- Unknown

Before large architectural changes, explain the rationale.

Do not claim a feature works until it has been tested.
