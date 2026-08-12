package com.wol.updater.infrastructure;

import com.wol.updater.application.VersionDetector;
import com.wol.updater.domain.VersionSignature;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Optional;

public class HashingVersionDetector implements VersionDetector {

    @Override
    public Optional<VersionSignature> detect(Path installationPath) {
        try {
            String strHash = getFileMd5(installationPath.resolve("data/stringtabley.xml"));
            String techHash = getFileMd5(installationPath.resolve("data/techtreey.xml"));
            String protoHash = getFileMd5(installationPath.resolve("data/protoy.xml"));

            return Optional.of(new VersionSignature(strHash, techHash, protoHash));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String getFileMd5(Path filePath) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
