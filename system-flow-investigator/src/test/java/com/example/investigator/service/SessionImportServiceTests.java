package com.example.investigator.service;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.config.*;
import com.example.investigator.domain.export.EventExportQuery;
import com.example.investigator.domain.export.SessionExportMetadata;
import com.example.investigator.domain.export.SessionImportSummary;
import com.example.investigator.storage.ImportedSessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionImportServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ImportedSessionStore store = new ImportedSessionStore();
    private final SessionImportService service = new SessionImportService(store);

    @Test
    void shouldImportValidSessionPackage() throws Exception {
        byte[] zip = sessionZip(
                metadata(),
                config(),
                objectMapper.writeValueAsString(event("trace-1")) + "\n"
        );

        SessionImportSummary summary = service.importSession(zip);

        assertThat(summary.status()).isEqualTo("IMPORTED");
        assertThat(summary.format()).isEqualTo("system-flow-investigator-session");
        assertThat(summary.version()).isEqualTo("1.0");
        assertThat(summary.exportedAt()).isEqualTo(Instant.parse("2026-04-27T04:50:00Z"));
        assertThat(summary.importedEventCount()).isEqualTo(1);
        assertThat(summary.investigationName()).isEqualTo("Main Investigation");
        assertThat(summary.flowIds()).containsExactly("main-lab-flow");
        assertThat(summary.sessionId()).isNotBlank();
    }

    @Test
    void shouldImportSessionWithEmptyEventsFile() throws Exception {
        byte[] zip = sessionZip(
                metadata(),
                config(),
                ""
        );

        SessionImportSummary summary = service.importSession(zip);

        assertThat(summary.status()).isEqualTo("IMPORTED");
        assertThat(summary.importedEventCount()).isZero();
    }

    @Test
    void shouldIgnoreUnknownZipEntries() throws Exception {
        byte[] zip = zip(Map.of(
                "metadata.json", objectMapper.writeValueAsString(metadata()),
                "config.json", objectMapper.writeValueAsString(config()),
                "events.ndjson", objectMapper.writeValueAsString(event("trace-1")) + "\n",
                "notes.txt", "ignored"
        ));

        SessionImportSummary summary = service.importSession(zip);

        assertThat(summary.status()).isEqualTo("IMPORTED");
        assertThat(summary.importedEventCount()).isEqualTo(1);
    }

    @Test
    void shouldFailWhenMetadataMissing() throws Exception {
        byte[] zip = zip(Map.of(
                "config.json", objectMapper.writeValueAsString(config()),
                "events.ndjson", objectMapper.writeValueAsString(event("trace-1")) + "\n"
        ));

        assertThatThrownBy(() -> service.importSession(zip))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed importing investigation session")
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Missing metadata.json");
    }

    @Test
    void shouldFailWhenConfigMissing() throws Exception {
        byte[] zip = zip(Map.of(
                "metadata.json", objectMapper.writeValueAsString(metadata()),
                "events.ndjson", objectMapper.writeValueAsString(event("trace-1")) + "\n"
        ));

        assertThatThrownBy(() -> service.importSession(zip))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed importing investigation session")
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Missing config.json");
    }

    @Test
    void shouldFailWhenEventsMissing() throws Exception {
        byte[] zip = zip(Map.of(
                "metadata.json", objectMapper.writeValueAsString(metadata()),
                "config.json", objectMapper.writeValueAsString(config())
        ));

        assertThatThrownBy(() -> service.importSession(zip))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed importing investigation session")
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Missing events.ndjson");
    }

    @Test
    void shouldFailWhenEventsNdjsonContainsInvalidJson() throws Exception {
        byte[] zip = sessionZip(
                metadata(),
                config(),
                "not-json\n"
        );

        assertThatThrownBy(() -> service.importSession(zip))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed importing investigation session");
    }

    @Test
    void shouldSupportConfigWithoutFlows() throws Exception {
        InvestigationConfig configWithoutFlows = new InvestigationConfig(
                "No Flows",
                "desc",
                null,
                new FlowValidationRules(50, true)
        );

        byte[] zip = sessionZip(
                metadata(),
                configWithoutFlows,
                objectMapper.writeValueAsString(event("trace-1")) + "\n"
        );

        SessionImportSummary summary = service.importSession(zip);

        assertThat(summary.investigationName()).isEqualTo("No Flows");
        assertThat(summary.flowIds()).isEmpty();
    }

    private byte[] sessionZip(SessionExportMetadata metadata,
                              InvestigationConfig config,
                              String eventsNdjson) throws Exception {
        return zip(Map.of(
                "metadata.json", objectMapper.writeValueAsString(metadata),
                "config.json", objectMapper.writeValueAsString(config),
                "events.ndjson", eventsNdjson
        ));
    }

    private byte[] zip(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }

        return output.toByteArray();
    }

    private SessionExportMetadata metadata() {
        return new SessionExportMetadata(
                "system-flow-investigator-session",
                "1.0",
                Instant.parse("2026-04-27T04:50:00Z"),
                new EventExportQuery(
                        null,
                        "trace-1",
                        null,
                        null,
                        null,
                        null
                ),
                1,
                Map.of(
                        "metadata", "metadata.json",
                        "config", "config.json",
                        "events", "events.ndjson"
                )
        );
    }

    private InvestigationConfig config() {
        return new InvestigationConfig(
                "Main Investigation",
                "desc",
                List.of(
                        new FlowDefinition(
                                "main-lab-flow",
                                "Main Lab Flow",
                                "desc",
                                List.of(
                                        new ExpectedFlowStep(
                                                1,
                                                "MQTT",
                                                "lab/flow/in",
                                                "Producer"
                                        )
                                )
                        )
                ),
                new FlowValidationRules(50, true)
        );
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