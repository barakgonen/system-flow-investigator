package com.example.investigator.domain.export;

import java.time.Instant;
import java.util.Map;

public record SessionExportMetadata(
        String format,
        String version,
        Instant exportedAt,
        EventExportQuery query,
        int eventCount,
        Map<String, String> files
) {
}