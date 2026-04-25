package com.example.investigator.domain;

import java.time.Instant;
import java.util.Map;

public record ObservedEvent(
        String protocol,
        String source,
        String channel,
        Instant observedAt,
        String payload,
        Map<String, String> metadata,
        String traceId,
        Instant sourceSentAt
) {
}