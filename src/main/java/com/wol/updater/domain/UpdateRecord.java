package com.wol.updater.domain;

import java.time.LocalDateTime;

/**
 * Represents a historical record of an update performed by the application.
 */
public record UpdateRecord(
    LocalDateTime timestamp,
    String targetVersion,
    String status,
    String details
) {
    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }
}
