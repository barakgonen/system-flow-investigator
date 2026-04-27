package com.example.investigator.ingestion.http;

import java.util.Map;

public record HttpIngestionEventRequest(
        String protocol,
        String source,
        String channel,
        String payload,
        Map<String, String> metadata
) {
}