package com.example.investigator.domain.export;

import java.time.Instant;
import java.util.List;

public record SessionImportSummary(
        String status,
        String message,
        String sessionId,
        String format,
        String version,
        Instant exportedAt,
        int importedEventCount,
        String investigationName,
        List<String> flowIds
) {
}