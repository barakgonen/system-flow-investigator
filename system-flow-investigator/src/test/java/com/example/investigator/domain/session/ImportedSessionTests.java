package com.example.investigator.domain.session;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.config.FlowValidationRules;
import com.example.investigator.domain.config.InvestigationConfig;
import com.example.investigator.domain.export.EventExportQuery;
import com.example.investigator.domain.export.SessionExportMetadata;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImportedSessionTests {

    @Test
    void shouldExposeAllFieldsAndSupportEquality() {
        Instant importedAt = Instant.parse("2026-04-27T05:00:00Z");

        SessionExportMetadata metadata = new SessionExportMetadata(
                "system-flow-investigator-session",
                "1.0",
                Instant.parse("2026-04-27T04:50:00Z"),
                new EventExportQuery(null, null, null, null, null, null),
                1,
                Map.of("events", "events.ndjson")
        );

        InvestigationConfig config = new InvestigationConfig(
                "Imported Investigation",
                "desc",
                List.of(),
                new FlowValidationRules(50, true)
        );

        ObservedEvent event = event("trace-1");

        ImportedSession one = new ImportedSession(
                "session-1",
                "Imported Investigation",
                importedAt,
                metadata,
                config,
                List.of(event)
        );

        ImportedSession two = new ImportedSession(
                "session-1",
                "Imported Investigation",
                importedAt,
                metadata,
                config,
                List.of(event)
        );

        assertThat(one.sessionId()).isEqualTo("session-1");
        assertThat(one.name()).isEqualTo("Imported Investigation");
        assertThat(one.importedAt()).isEqualTo(importedAt);
        assertThat(one.metadata()).isEqualTo(metadata);
        assertThat(one.config()).isEqualTo(config);
        assertThat(one.events()).containsExactly(event);

        assertThat(one).isEqualTo(two);
        assertThat(one.hashCode()).isEqualTo(two.hashCode());
        assertThat(one.toString()).contains("session-1", "Imported Investigation");
    }

    private ObservedEvent event(String traceId) {
        return new ObservedEvent(
                "MQTT",
                "localhost:1883",
                "lab/flow/in",
                Instant.parse("2026-04-27T04:49:32Z"),
                "{\"traceId\":\"%s\"}".formatted(traceId),
                Map.of("qos", "1"),
                traceId,
                Instant.parse("2026-04-27T04:49:31Z")
        );
    }
}