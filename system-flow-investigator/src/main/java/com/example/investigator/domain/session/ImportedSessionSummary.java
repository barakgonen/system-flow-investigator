package com.example.investigator.domain.session;

import java.time.Instant;

public record ImportedSessionSummary(
        String sessionId,
        String name,
        Instant importedAt,
        int eventCount
) {
}