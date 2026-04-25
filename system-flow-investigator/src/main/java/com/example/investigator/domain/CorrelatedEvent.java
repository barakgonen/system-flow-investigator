package com.example.investigator.domain;

import java.time.Instant;
import java.util.Map;

public record CorrelatedEvent(
        int index,
        String protocol,
        String source,
        String channel,
        Instant sourceSentAt,
        Instant observedAt,
        Long deltaFromPreviousSourceMs,
        Long deltaFromPreviousObservedMs,
        String payload,
        Map<String, String> metadata
) {
}