package com.example.investigator.domain;

import java.time.Instant;
import java.util.Map;

public record ObservedEvent(
        String protocol,
        String sourceName,
        String channel,
        Instant receivedAt,
        String payload,
        Map<String, String> headers,
        String traceId
) {
}