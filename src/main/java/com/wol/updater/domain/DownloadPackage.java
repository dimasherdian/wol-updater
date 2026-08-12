package com.wol.updater.domain;

import java.util.Optional;

public record DownloadPackage(
    int id,
    long sizeBytes,
    String crc32,
    String primaryUrl,
    Optional<String> fallbackUrl,
    String targetVersion,
    Optional<String> postUpdateDeleteList,
    Optional<String> postUpdateBrowserPage
) {}
