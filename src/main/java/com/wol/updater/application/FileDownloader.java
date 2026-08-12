package com.wol.updater.application;

import com.wol.updater.domain.DownloadPackage;
import java.nio.file.Path;
import java.util.Optional;

public interface FileDownloader {
    Optional<Path> download(DownloadPackage pkg, Path temporaryDirectory, UpdateObserver observer);
}
