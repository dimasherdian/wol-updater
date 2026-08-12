package com.wol.updater.infrastructure;

import com.wol.updater.application.HistoryManager;
import com.wol.updater.domain.UpdateRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileHistoryManager implements HistoryManager {

    private final Path historyFile;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String SEPARATOR = "||";

    public FileHistoryManager(Path dataDir) {
        this.historyFile = dataDir.resolve("updater-history.log");
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            if (!Files.exists(historyFile)) {
                Files.createFile(historyFile);
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize history file: " + e.getMessage());
        }
    }

    @Override
    public void addRecord(UpdateRecord record) {
        if (!Files.exists(historyFile)) return;
        
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(historyFile.toFile(), true)))) {
            String line = String.join(SEPARATOR, 
                record.timestamp().format(FORMATTER),
                record.targetVersion() != null ? record.targetVersion() : "Unknown",
                record.status() != null ? record.status() : "UNKNOWN",
                record.details() != null ? record.details() : ""
            );
            out.println(line);
        } catch (IOException e) {
            System.err.println("Failed to write to history file: " + e.getMessage());
        }
    }

    @Override
    public List<UpdateRecord> getHistory() {
        List<UpdateRecord> records = new ArrayList<>();
        if (!Files.exists(historyFile)) return records;

        try (BufferedReader reader = new BufferedReader(new FileReader(historyFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|\\|");
                if (parts.length >= 3) {
                    LocalDateTime timestamp = LocalDateTime.parse(parts[0], FORMATTER);
                    String version = parts[1];
                    String status = parts[2];
                    String details = parts.length > 3 ? parts[3] : "";
                    records.add(new UpdateRecord(timestamp, version, status, details));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to read history file: " + e.getMessage());
        }

        // Return newest to oldest
        Collections.reverse(records);
        return records;
    }

    @Override
    public void clearHistory() {
        try {
            Files.deleteIfExists(historyFile);
            Files.createFile(historyFile);
        } catch (IOException e) {
            System.err.println("Failed to clear history: " + e.getMessage());
        }
    }
}
