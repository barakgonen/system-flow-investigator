package com.example.investigator.domain.session;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.config.InvestigationConfig;
import com.example.investigator.domain.export.SessionExportMetadata;

import java.time.Instant;
import java.util.List;

public record ImportedSession(
        String sessionId,
        String name,
        Instant importedAt,
        SessionExportMetadata metadata,
        InvestigationConfig config,
        List<ObservedEvent> events
) {
}