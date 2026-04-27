package com.example.investigator.domain.export;

import java.time.Instant;

public record EventExportQuery(
        String channel,
        String traceId,
        String protocol,
        String source,
        Instant from,
        Instant to
) {
}