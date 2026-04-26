package com.example.investigator.domain;

import com.example.investigator.domain.config.FlowValidationResult;

import java.time.Instant;
import java.util.List;

public record TraceTimelineResponse(
        String traceId,
        int eventCount,
        Instant startedAt,
        Instant lastObservedAt,
        Long totalSourceDurationMs,
        Long totalObservedDurationMs,
        List<CorrelatedEvent> events,
        FlowValidationResult validation
) {
}