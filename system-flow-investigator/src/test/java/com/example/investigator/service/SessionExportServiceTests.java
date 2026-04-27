package com.example.investigator.service;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.config.*;
import com.example.investigator.domain.export.EventExportQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionExportServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldExportSessionZipWithMetadataConfigAndEvents() throws Exception {
        EventExportService eventExportService = mock(EventExportService.class);
        InvestigationConfigService configService = mock(InvestigationConfigService.class);

        EventExportQuery query = new EventExportQuery(
                "lab/flow/in",
                "trace-1",
                "MQTT",
                "localhost:1883",
                Instant.parse("2026-04-27T04:49:00Z"),
                Instant.parse("2026-04-27T04:50:00Z")
        );

        when(eventExportService.exportEvents(query)).thenReturn(List.of(event()));
        when(eventExportService.exportNdjson(query)).thenReturn("{\"traceId\":\"trace-1\"}\n");
        when(configService.getConfig()).thenReturn(config());

        SessionExportService service = new SessionExportService(eventExportService, configService);

        byte[] zipBytes = service.exportSession(query);

        Map<String, String> zipEntries = unzipToStringMap(zipBytes);

        assertThat(zipEntries).containsKeys("metadata.json", "config.json", "events.ndjson");

        JsonNode metadata = objectMapper.readTree(zipEntries.get("metadata.json"));
        assertThat(metadata.get("format").asText()).isEqualTo("system-flow-investigator-session");
        assertThat(metadata.get("version").asText()).isEqualTo("1.0");
        assertThat(metadata.get("eventCount").asInt()).isEqualTo(1);
        assertThat(metadata.get("files").get("metadata").asText()).isEqualTo("metadata.json");
        assertThat(metadata.get("files").get("config").asText()).isEqualTo("config.json");
        assertThat(metadata.get("files").get("events").asText()).isEqualTo("events.ndjson");

        assertThat(metadata.get("query").get("channel").asText()).isEqualTo("lab/flow/in");
        assertThat(metadata.get("query").get("traceId").asText()).isEqualTo("trace-1");
        assertThat(metadata.get("query").get("protocol").asText()).isEqualTo("MQTT");
        assertThat(metadata.get("query").get("source").asText()).isEqualTo("localhost:1883");

        JsonNode configJson = objectMapper.readTree(zipEntries.get("config.json"));
        assertThat(configJson.get("name").asText()).isEqualTo("Main Investigation");
        assertThat(configJson.get("flows")).hasSize(1);
        assertThat(configJson.get("flows").get(0).get("id").asText()).isEqualTo("main-lab-flow");

        assertThat(zipEntries.get("events.ndjson")).isEqualTo("{\"traceId\":\"trace-1\"}\n");

        verify(eventExportService).exportEvents(query);
        verify(eventExportService).exportNdjson(query);
        verify(configService).getConfig();
    }

    @Test
    void shouldSupportEmptyExports() throws Exception {
        EventExportService eventExportService = mock(EventExportService.class);
        InvestigationConfigService configService = mock(InvestigationConfigService.class);

        when(eventExportService.exportEvents(any())).thenReturn(List.of());
        when(eventExportService.exportNdjson(any())).thenReturn("");
        when(configService.getConfig()).thenReturn(config());

        SessionExportService service = new SessionExportService(eventExportService, configService);

        byte[] zipBytes = service.exportSession(null);

        Map<String, String> zipEntries = unzipToStringMap(zipBytes);

        JsonNode metadata = objectMapper.readTree(zipEntries.get("metadata.json"));
        assertThat(metadata.get("eventCount").asInt()).isZero();

        assertThat(zipEntries.get("events.ndjson")).isEmpty();
        assertThat(zipEntries).containsKeys("metadata.json", "config.json", "events.ndjson");
    }

    @Test
    void shouldCreateReadableZipFile() throws Exception {
        EventExportService eventExportService = mock(EventExportService.class);
        InvestigationConfigService configService = mock(InvestigationConfigService.class);

        when(eventExportService.exportEvents(any())).thenReturn(List.of(event()));
        when(eventExportService.exportNdjson(any())).thenReturn("{\"event\":1}\n");
        when(configService.getConfig()).thenReturn(config());

        SessionExportService service = new SessionExportService(eventExportService, configService);

        byte[] zipBytes = service.exportSession(new EventExportQuery(
                null,
                null,
                null,
                null,
                null,
                null
        ));

        Map<String, String> zipEntries = unzipToStringMap(zipBytes);

        assertThat(zipEntries.keySet())
                .containsExactlyInAnyOrder("metadata.json", "config.json", "events.ndjson");
    }

    private Map<String, String> unzipToStringMap(byte[] zipBytes) throws Exception {
        Map<String, String> result = new java.util.LinkedHashMap<>();

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                String content = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                result.put(entry.getName(), content);
                zip.closeEntry();
            }
        }

        return result;
    }

    private InvestigationConfig config() {
        return new InvestigationConfig(
                "Main Investigation",
                "Test config",
                List.of(
                        new FlowDefinition(
                                "main-lab-flow",
                                "Main Lab Flow",
                                "Main flow",
                                List.of(
                                        new ExpectedFlowStep(1, "MQTT", "lab/flow/in", "Producer")
                                )
                        )
                ),
                new FlowValidationRules(50, true)
        );
    }

    private ObservedEvent event() {
        return new ObservedEvent(
                "MQTT",
                "localhost:1883",
                "lab/flow/in",
                Instant.parse("2026-04-27T04:49:32Z"),
                "{\"traceId\":\"trace-1\"}",
                Map.of("qos", "1"),
                "trace-1",
                Instant.parse("2026-04-27T04:49:31Z")
        );
    }
}