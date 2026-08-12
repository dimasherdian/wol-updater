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

<p align="center">
  <img src="assets/demo.gif" width="860" alt="Wars of Liberty Updater Demo">
</p>

## The Numbers & Difference

The original updater had a few critical flaws that made it prone to breaking the game. Here is a direct comparison of how this new implementation solves them:

| Feature | Legacy Updater | New Java Updater |
|---|:---:|:---:|
| **Network Fault Tolerance** | ❌ Corrupts game on disconnect | ✅ **Safe atomic caching** |
| **Directory Validation** | ❌ Blind installation anywhere | ✅ **Strict `age3y.exe` verify** |
| **Data Retention** | ❌ Zero backups | ✅ **Built-in profile backup UI** |
| **Maintainability** | ❌ Proprietary black-box | ✅ **Clean Architecture (Open-Source)** |
| **UI Rendering** | ❌ Outdated forms | ✅ **Hardware-Accelerated JavaFX** |

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
