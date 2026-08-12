# Wars of Liberty Updater
<img width="88" height="31" alt="image" src="https://github.com/user-attachments/assets/2405a573-5b4e-47fb-8649-a2a3514b7d13" /> <img width="88" height="31" alt="image" src="https://github.com/user-attachments/assets/9d831bf7-aef0-4baa-96ac-7a76d6190e47" />


A robust, Java-based open-source updater designed specifically for the Wars of Liberty modification (Age of Empires III). 

**Disclaimer of Liability**: This application is currently in active development. It is provided "as is" without warranty of any kind, express or implied. The authors and contributors are not responsible for any data loss, game corruption, or any other damages that may arise from the use of this software.

**Notice**: This is an unofficial, community-driven project and is not affiliated with, endorsed by, or owned by the official Wars of Liberty development team.

## Architecture and Design Principles

The application is built upon Clean Architecture principles, ensuring a strict separation of concerns across Presentation, Business Logic, and Data/Storage layers. This architectural approach guarantees that the codebase remains highly maintainable, testable, and extensible for future community contributions.

## Key Capabilities

### Strict Validation and Integrity
The updater implements strict boundary validation to prevent destructive file operations. It proactively verifies the presence of the base game executable (`age3y.exe`) prior to initiating any installation sequence. This guarantees that modifications are only applied to valid Age of Empires III installation directories. Furthermore, payload integrity is verified through hash checks before and after extraction.

### Atomic Download and Extraction
To mitigate the risk of corrupted installations caused by network interruptions, the application employs an atomic update flow. Payloads are securely downloaded to a designated temporary directory. The live game state is never modified until the download is fully validated, ensuring the game remains playable even if an update process fails prematurely.

### State Management and Backup
The updater features an integrated User Data manager that automatically resolves the `My Games/Age of Empires 3` path. Users can safely archive their progression (save states, profiles, and home city decks) into compressed backup files. These archives can be restored directly through the application interface to recover from potential data loss.

### Presentation Layer
The presentation layer is implemented using JavaFX, delivering a hardware-accelerated, responsive graphical user interface. The UI relies on structured CSS, supporting dynamic theme switching (Dark and Light modes) and multi-language localization (English and Indonesian).

## Feature Comparison: Original Updater vs Current Implementation

This project addresses several critical limitations present in the legacy updater:
1. **Network Fault Tolerance**: The legacy updater extracts files concurrently during download, which frequently results in corrupted game states upon connection drops. The current implementation utilizes a safe temporary caching mechanism.
2. **Directory Validation**: The legacy updater permits installation to arbitrary paths without validation. The current implementation strictly enforces path verification.
3. **Data Retention**: The legacy updater lacks data retention utilities. The current implementation provides integrated backup and restore mechanisms for user profiles.
4. **Maintainability**: The current implementation is open-source and structured using Clean Architecture, allowing for straightforward community improvements.

## Build Instructions

The build process is automated via Maven Wrapper and PowerShell scripts.

### Requirements
* Java Development Kit (JDK) 17 or higher
* `JAVA_HOME` environment variable configured

### Compilation Profiles

**1. Minimal Distribution (Self-Contained)**
This profile utilizes `jlink` and `jpackage` to bundle a minimal Java Runtime Environment. The resulting executable does not require Java to be installed on the host operating system.
```powershell
.\build-minimal.ps1
```
Output directory: `dist\WoLUpdater-Minimal\`

**2. Java-Required Distribution**
This profile compiles a lightweight package utilizing a native C# launcher. It requires the host operating system to have a compatible Java 17 runtime installed.
```powershell
.\build-java-required.ps1
```
Output directory: `dist\WoLUpdater-JavaRequired\`

**3. Development Build**
To compile a standard executable JAR for testing environments:
```powershell
.\mvnw.cmd clean package
```
Output directory: `target\`

To execute the application directly from source:
```powershell
.\mvnw.cmd clean compile javafx:run
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. 

**Legal & Trademarks**: "Wars of Liberty", "Age of Empires III", and all related assets are the property of their respective owners and creators. This tool claims no ownership over the game, the mod, or its official assets. This is a clean-room implementation of a third-party client and does not contain proprietary source code from the original updater.
