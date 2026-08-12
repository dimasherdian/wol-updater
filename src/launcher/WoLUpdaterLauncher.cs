using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;
using System.Text.RegularExpressions;

namespace WoLUpdater {
    class Launcher {
        [STAThread]
        static void Main(string[] args) {
            string javaExe = FindJavaPath();
            
            if (javaExe == null) {
                MessageBox.Show(
                    "Java 17 or higher is required to run the Wars of Liberty Updater, but it was not found on your system.\n\n" +
                    "Please install Eclipse Temurin JRE 17 (or another compatible Java 17 runtime) and try again.", 
                    "Java Required", 
                    MessageBoxButtons.OK, 
                    MessageBoxIcon.Error
                );
                return;
            }

            string appDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "app");
            string jarPath = Path.Combine(appDir, "updater-1.0-SNAPSHOT.jar");

            if (!File.Exists(jarPath)) {
                MessageBox.Show(
                    string.Format("Application JAR not found at:\n{0}\n\nPlease ensure the application is installed correctly.", jarPath),
                    "Missing Application Files",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error
                );
                return;
            }

            try {
                ProcessStartInfo psi = new ProcessStartInfo {
                    FileName = javaExe,
                    Arguments = string.Format("-jar \"{0}\" {1}", jarPath, string.Join(" ", args)),
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    WorkingDirectory = AppDomain.CurrentDomain.BaseDirectory
                };
                
                Process p = Process.Start(psi);
                // We do not wait for the process to exit so the launcher can close.
            } catch (Exception ex) {
                MessageBox.Show(
                    string.Format("Failed to start the application:\n{0}", ex.Message),
                    "Launch Error",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error
                );
            }
        }

        static string FindJavaPath() {
            // First check for bundled JRE (Minimal Distribution)
            string bundledJava = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "runtime", "bin", "java.exe");
            if (File.Exists(bundledJava)) {
                return bundledJava; // Trust the bundled JRE without checking version
            }

            // Then check PATH
            string pathEnv = Environment.GetEnvironmentVariable("PATH");
            if (pathEnv != null) {
                foreach (string path in pathEnv.Split(Path.PathSeparator)) {
                    string fullPath = Path.Combine(path.Trim('\"'), "java.exe");
                    if (File.Exists(fullPath)) {
                        if (CheckJavaVersion(fullPath)) {
                            return fullPath;
                        }
                    }
                }
            }

            // Could also check registry for JAVA_HOME or JRE installations, but PATH is the standard
            string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (javaHome != null) {
                string fullPath = Path.Combine(javaHome, "bin", "java.exe");
                if (File.Exists(fullPath)) {
                    if (CheckJavaVersion(fullPath)) {
                        return fullPath;
                    }
                }
            }

            return null;
        }

        static bool CheckJavaVersion(string javaExe) {
            try {
                ProcessStartInfo psi = new ProcessStartInfo {
                    FileName = javaExe,
                    Arguments = "-version",
                    UseShellExecute = false,
                    RedirectStandardError = true, // java -version outputs to stderr
                    CreateNoWindow = true
                };
                
                using (Process process = Process.Start(psi)) {
                    string output = process.StandardError.ReadToEnd();
                    process.WaitForExit();
                    
                    // Match java version "17.X" or openjdk version "17.X"
                    Match match = Regex.Match(output, @"version\s+""(\d+)");
                    if (match.Success) {
                        int majorVersion;
                        if (int.TryParse(match.Groups[1].Value, out majorVersion)) {
                            // Java 1.8 returns "1", so we need to handle that, but for >=9 it returns the major version directly
                            if (majorVersion == 1) {
                                match = Regex.Match(output, @"version\s+""1\.(\d+)");
                                if (match.Success && int.TryParse(match.Groups[1].Value, out majorVersion)) {
                                    return majorVersion >= 17;
                                }
                            }
                            return majorVersion >= 17;
                        }
                    }
                }
            } catch {
                return false;
            }
            return false;
        }
    }
}
