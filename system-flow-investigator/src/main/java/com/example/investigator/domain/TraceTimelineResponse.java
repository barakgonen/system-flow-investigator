package com.example.investigator.domain;

import java.time.Instant;
import java.util.List;

public record TraceTimelineResponse(
        String traceId,
        int eventCount,
        Instant startedAt,
        Instant lastObservedAt,
        Long totalSourceDurationMs,
        Long totalObservedDurationMs,
        List<CorrelatedEvent> events
) {
}