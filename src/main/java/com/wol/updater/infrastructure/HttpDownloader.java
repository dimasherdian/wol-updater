package com.wol.updater.infrastructure;

import com.wol.updater.application.FileDownloader;
import com.wol.updater.application.UpdateObserver;
import com.wol.updater.domain.DownloadPackage;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Optional;
import java.util.zip.CRC32;

public class HttpDownloader implements FileDownloader {

    private final HttpClient httpClient;

    public HttpDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public Optional<Path> download(DownloadPackage pkg, Path temporaryDirectory, UpdateObserver observer) {
        Path targetFile = temporaryDirectory.resolve("update_" + pkg.id() + "_" + pkg.targetVersion() + ".tar.xz");
        
        boolean success = tryDownloadWithRetries(pkg.primaryUrl(), targetFile, pkg, observer, 5);
        if (!success && pkg.fallbackUrl().isPresent()) {
            success = tryDownloadWithRetries(pkg.fallbackUrl().get(), targetFile, pkg, observer, 5);
        }

        if (success) {
            return Optional.of(targetFile);
        }
        return Optional.empty();
    }

    private boolean tryDownloadWithRetries(String url, Path targetFile, DownloadPackage pkg, UpdateObserver observer, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            if (observer.isCancelled()) return false;
            
            boolean success = tryDownload(url, targetFile, pkg, observer);
            if (success) return true;
            
            if (observer.isCancelled()) return false;
            
            attempt++;
            if (attempt < maxRetries) {
                try {
                    // Exponential backoff: 2s, 4s, 8s, 16s...
                    long waitSeconds = (long) Math.pow(2, attempt); 
                    observer.onProgressUpdate(0, 0); // Reset or notify user if needed, though observer doesn't have a message method for this yet
                    Thread.sleep(1000L * waitSeconds);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private boolean tryDownload(String url, Path targetFile, DownloadPackage pkg, UpdateObserver observer) {
        try {
            long existingSize = 0;
            CRC32 crc32 = new CRC32();
            if (Files.exists(targetFile)) {
                existingSize = Files.size(targetFile);
                if (existingSize > pkg.sizeBytes()) {
                    Files.delete(targetFile);
                    existingSize = 0;
                } else if (existingSize > 0) {
                    try (InputStream is = Files.newInputStream(targetFile)) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = is.read(buf)) != -1) {
                            crc32.update(buf, 0, r);
                        }
                    }
                }
            }
            
            if (existingSize == pkg.sizeBytes()) {
                // Already downloaded completely, verify CRC directly
                return verifyCrc(crc32, pkg, targetFile, observer);
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET();
                    // We removed the 30-minute global timeout. Java 11 HttpClient doesn't support read timeout easily,
                    // but the underlying OS socket timeout and keep-alive will drop dead connections eventually.

            if (existingSize > 0) {
                requestBuilder.header("Range", "bytes=" + existingSize + "-");
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            
            if (response.statusCode() != 200 && response.statusCode() != 206) {
                return false;
            }

            if (response.statusCode() == 200 && existingSize > 0) {
                // Server ignored Range header
                existingSize = 0;
                crc32.reset();
            }

            long totalBytes = pkg.sizeBytes();
            long downloadedBytes = existingSize;

            Files.createDirectories(targetFile.getParent());
            StandardOpenOption openOption = (existingSize > 0) ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING;
            
            try (InputStream is = response.body();
                 OutputStream os = Files.newOutputStream(targetFile, StandardOpenOption.CREATE, openOption)) {
                
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    if (observer.isCancelled()) {
                        throw new java.util.concurrent.CancellationException();
                    }

                    os.write(buffer, 0, read);
                    crc32.update(buffer, 0, read);
                    
                    downloadedBytes += read;
                    observer.onProgressUpdate(downloadedBytes, totalBytes);
                }
            }

            return verifyCrc(crc32, pkg, targetFile, observer);
        } catch (java.util.concurrent.CancellationException e) {
            // Keep the partial file
            return false;
        } catch (Exception e) {
            // Keep the partial file in case it was a network error so we can resume
            return false;
        }
    }

    private boolean verifyCrc(CRC32 crc32, DownloadPackage pkg, Path targetFile, UpdateObserver observer) {
        String expectedCrc = pkg.crc32().toLowerCase();
        String actualCrc = Long.toHexString(crc32.getValue()).toLowerCase();
        
        while (actualCrc.length() < 8) {
            actualCrc = "0" + actualCrc;
        }

        if (!expectedCrc.equals(actualCrc)) {
            observer.onError("CRC32 mismatch. Expected: " + expectedCrc + ", Actual: " + actualCrc, null);
            try { Files.deleteIfExists(targetFile); } catch (Exception ignored) {}
            return false;
        }
        return true;
    }
}
