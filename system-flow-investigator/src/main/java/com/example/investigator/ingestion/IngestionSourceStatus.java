package com.example.investigator.ingestion;

import java.time.Instant;
import java.util.List;

public record IngestionSourceStatus(
        String id,
        String displayName,
        String protocol,
        boolean connected,
        List<String> observedChannels,
        Instant lastEventAt,
        String message
) {
}