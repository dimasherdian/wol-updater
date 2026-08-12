package com.wol.updater.infrastructure;

import com.wol.updater.domain.VersionSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashingVersionDetectorTest {

    @Test
    void testDetect_Success(@TempDir Path tempDir) throws Exception {
        Path dataDir = tempDir.resolve("data");
        Files.createDirectories(dataDir);
        
        Files.writeString(dataDir.resolve("stringtabley.xml"), "string_data");
        Files.writeString(dataDir.resolve("techtreey.xml"), "tech_data");
        Files.writeString(dataDir.resolve("protoy.xml"), "proto_data");

        HashingVersionDetector detector = new HashingVersionDetector();
        Optional<VersionSignature> result = detector.detect(tempDir);

        assertTrue(result.isPresent());
        
        // MD5 of "string_data" is 697a0c1711784ac7d869bcdd8708e178
        assertEquals("697a0c1711784ac7d869bcdd8708e178", result.get().stringTableHash());
    }

    @Test
    void testDetect_MissingFile(@TempDir Path tempDir) throws Exception {
        Path dataDir = tempDir.resolve("data");
        Files.createDirectories(dataDir);
        
        // Missing protoy.xml
        Files.writeString(dataDir.resolve("stringtabley.xml"), "string_data");
        Files.writeString(dataDir.resolve("techtreey.xml"), "tech_data");

        HashingVersionDetector detector = new HashingVersionDetector();
        Optional<VersionSignature> result = detector.detect(tempDir);

        assertTrue(result.isEmpty());
    }
}
