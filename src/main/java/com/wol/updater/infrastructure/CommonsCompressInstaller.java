package com.wol.updater.infrastructure;

import com.wol.updater.application.UpdateInstaller;
import com.wol.updater.application.UpdateObserver;
import com.wol.updater.domain.DownloadPackage;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class CommonsCompressInstaller implements UpdateInstaller {

    @Override
    public boolean install(Path downloadedArchive, DownloadPackage pkg, Path targetInstallationPath, UpdateObserver observer) {
        Path backupDir = downloadedArchive.getParent().resolve("backup_" + pkg.id() + "_" + System.currentTimeMillis());
        List<Path> backedUpFiles = new ArrayList<>();

        try {
            Files.createDirectories(backupDir);
            long totalSize = Files.size(downloadedArchive);
            
            try (InputStream fi = Files.newInputStream(downloadedArchive);
                 FilterInputStream countingIn = new FilterInputStream(fi) {
                     private long count = 0;
                     private long lastUpdate = 0;
                     @Override public int read() throws IOException {
                         int r = super.read();
                         if (r != -1) count++;
                         report();
                         return r;
                     }
                     @Override public int read(byte[] b, int off, int len) throws IOException {
                         int r = super.read(b, off, len);
                         if (r != -1) count += r;
                         report();
                         return r;
                     }
                     private void report() {
                         long now = System.currentTimeMillis();
                         if (now - lastUpdate > 100) {
                             observer.onProgressUpdate(count, totalSize);
                             lastUpdate = now;
                         }
                     }
                 };
                 BufferedInputStream bi = new BufferedInputStream(countingIn);
                 XZCompressorInputStream xzi = new XZCompressorInputStream(bi);
                 TarArchiveInputStream tar = new TarArchiveInputStream(xzi)) {

                TarArchiveEntry entry;
                while ((entry = tar.getNextTarEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    Path targetFile = targetInstallationPath.resolve(entry.getName()).normalize();
                    // Prevent path traversal attacks
                    if (!targetFile.startsWith(targetInstallationPath.normalize())) {
                        throw new SecurityException("Zip Slip vulnerability detected: " + entry.getName());
                    }

                    // Backup original file if it exists
                    if (Files.exists(targetFile)) {
                        Path backupFile = backupDir.resolve(entry.getName());
                        Files.createDirectories(backupFile.getParent());
                        Files.copy(targetFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                        backedUpFiles.add(targetFile);
                    }

                    // Extract new file
                    Files.createDirectories(targetFile.getParent());
                    try (OutputStream os = Files.newOutputStream(targetFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = tar.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                    }
                }
            }
            
            // Clean up backup after success
            deleteDirectoryRecursively(backupDir);
            return true;

        } catch (Exception e) {
            observer.onError("Extraction failed, attempting rollback...", e);
            rollback(backedUpFiles, backupDir, targetInstallationPath, observer);
            return false;
        }
    }

    @Override
    public void executePostUpdateActions(DownloadPackage pkg, Path targetInstallationPath) {
        pkg.postUpdateDeleteList().ifPresent(deleteListFileName -> {
            Path deleteListFile = targetInstallationPath.resolve(deleteListFileName);
            if (Files.exists(deleteListFile)) {
                try (BufferedReader reader = Files.newBufferedReader(deleteListFile)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Path toDelete = targetInstallationPath.resolve(line.trim());
                        if (Files.exists(toDelete)) {
                            if (Files.isDirectory(toDelete)) {
                                deleteDirectoryRecursively(toDelete);
                            } else {
                                Files.delete(toDelete);
                            }
                        }
                    }
                } catch (IOException e) {
                    // Ignore or log delete list failures
                }
            }
        });
        
        pkg.postUpdateBrowserPage().ifPresent(page -> {
            try {
                if (page.startsWith("http://") || page.startsWith("https://")) {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(page));
                    }
                } else {
                    Path execPath = targetInstallationPath.resolve(page);
                    if (Files.exists(execPath)) {
                        if (page.toLowerCase().endsWith(".bat") || page.toLowerCase().endsWith(".cmd")) {
                            new ProcessBuilder("cmd", "/c", execPath.getFileName().toString())
                                .directory(execPath.getParent().toFile())
                                .start()
                                .waitFor();
                        } else {
                            if (java.awt.Desktop.isDesktopSupported()) {
                                java.awt.Desktop.getDesktop().open(execPath.toFile());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore failure to open post update page
            }
        });
    }

    private void rollback(List<Path> backedUpFiles, Path backupDir, Path targetInstallationPath, UpdateObserver observer) {
        for (Path targetFile : backedUpFiles) {
            Path backupFile = backupDir.resolve(targetInstallationPath.relativize(targetFile));
            if (Files.exists(backupFile)) {
                try {
                    Files.move(backupFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    observer.onError("Failed to rollback file: " + targetFile, e);
                }
            }
        }
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a)) // Reverse order so files are deleted before dirs
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException ignored) {}
                });
        }
    }
}
