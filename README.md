# Wars of Liberty Updater
<img width="88" height="31" alt="button-88x31" src="https://github.com/user-attachments/assets/14528d6c-bf9d-4048-afcd-7621a9be9887" />

<img width="88" height="31" alt="image" src="https://github.com/user-attachments/assets/2405a573-5b4e-47fb-8649-a2a3514b7d13" /> <img width="88" height="31" alt="image" src="https://github.com/user-attachments/assets/9d831bf7-aef0-4baa-96ac-7a76d6190e47" />


*A robust, Java-based open-source updater designed specifically for the Wars of Liberty modification (Age of Empires III).*

## The Backstory: Why Build Another Updater?

It started with a simple frustration. The legacy updater for Wars of Liberty looked outdated and lacked visual appeal. Then, the developers released a new C# launcher that behaved surprisingly similar to a Java application. That sparked a thought: *"Why not just build it in Java from the start?"*

But the real breaking point came during an update. I used the new official launcher to update the mod to the latest version. Everything seemed fine until I hit "Play"—the game immediately crashed with an error. 

I spent an entire day trying to fix that error. The last thing I wanted was to re-download gigabytes of mod data all over again just because an update process corrupted a few files.

That frustrating experience turned into an opportunity. I decided to dive into **reverse engineering** to understand exactly how the original updater communicated with the servers, downloaded files, and applied patches. My goal wasn't just to learn; it was to build a fail-safe, atomic updater that guarantees your game won't break if your internet drops or a file fails to extract.

This project is the result: a custom, reliable, and aesthetically pleasing updater built from scratch using Clean Architecture.

## Why This Updater is Better

- **Safe, Atomic Updates**: No more corrupted game states. Files are downloaded and verified in a temporary folder first. Your live game is never touched until the download is 100% validated.
- **Strict Directory Validation**: It actively checks for `age3y.exe` before doing anything, ensuring you never accidentally install gigabytes of data into the wrong folder.
- **Built-in Data Backup**: Easily backup and restore your profiles, save states, and home cities directly from the UI.
- **Hardware-Accelerated UI**: A modern JavaFX interface with dynamic Dark/Light modes and multi-language support.
- **Fully Open-Source**: Transparent, community-driven, and easy to maintain.

## Build Instructions

The build process is automated via Maven Wrapper and PowerShell scripts.

### Requirements
* Java Development Kit (JDK) 17 or higher
* `JAVA_HOME` environment variable configured

### Compilation Profiles

**1. Minimal Distribution (Self-Contained)**
This profile utilizes `jlink` to bundle a minimal Java Runtime Environment. The resulting executable does not require Java to be installed on the host operating system.
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
