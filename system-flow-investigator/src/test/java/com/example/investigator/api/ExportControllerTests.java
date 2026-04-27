package com.example.investigator.api;

import com.example.investigator.service.EventExportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ExportControllerTests {

    @Test
    void shouldExportEventsAsNdjson() {
        EventExportService service = mock(EventExportService.class);

        when(service.exportNdjson(argThat(query ->
                query.channel() == null
                        && query.traceId() == null
                        && query.protocol() == null
                        && query.source() == null
                        && query.from() == null
                        && query.to() == null
        ))).thenReturn("{\"event\":1}\n");

        ExportController controller = new ExportController(service);

        ResponseEntity<String> response = controller.exportEvents(
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("application/x-ndjson");
        assertThat(response.getBody()).isEqualTo("{\"event\":1}\n");

        verify(service).exportNdjson(any());
    }

    @Test
    void shouldPassFiltersToExportService() {
        EventExportService service = mock(EventExportService.class);

        Instant from = Instant.parse("2026-04-27T04:49:00Z");
        Instant to = Instant.parse("2026-04-27T04:50:00Z");

        when(service.exportNdjson(argThat(query ->
                "lab/flow/in".equals(query.channel())
                        && "trace-1".equals(query.traceId())
                        && "MQTT".equals(query.protocol())
                        && "localhost:1883".equals(query.source())
                        && from.equals(query.from())
                        && to.equals(query.to())
        ))).thenReturn("{\"event\":1}\n");

        ExportController controller = new ExportController(service);

        ResponseEntity<String> response = controller.exportEvents(
                "lab/flow/in",
                "trace-1",
                "MQTT",
                "localhost:1883",
                from,
                to
        );

        assertThat(response.getBody()).isEqualTo("{\"event\":1}\n");

        verify(service).exportNdjson(argThat(query ->
                "lab/flow/in".equals(query.channel())
                        && "trace-1".equals(query.traceId())
                        && "MQTT".equals(query.protocol())
                        && "localhost:1883".equals(query.source())
                        && from.equals(query.from())
                        && to.equals(query.to())
        ));
    }

    @Test
    void shouldDownloadEventsAsAttachment() {
        EventExportService service = mock(EventExportService.class);

        when(service.exportNdjson(any()))
                .thenReturn("{\"event\":1}\n");

        ExportController controller = new ExportController(service);

        ResponseEntity<String> response = controller.downloadEvents(
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("application/x-ndjson");
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=\"investigation-events.ndjson\"");
        assertThat(response.getBody()).isEqualTo("{\"event\":1}\n");

        verify(service).exportNdjson(any());
    }

    @Test
    void shouldPassFiltersToDownloadService() {
        EventExportService service = mock(EventExportService.class);

        Instant from = Instant.parse("2026-04-27T04:49:00Z");
        Instant to = Instant.parse("2026-04-27T04:50:00Z");

        when(service.exportNdjson(argThat(query ->
                "lab/flow/out".equals(query.channel())
                        && "trace-2".equals(query.traceId())
                        && "WS".equals(query.protocol())
                        && "web".equals(query.source())
                        && from.equals(query.from())
                        && to.equals(query.to())
        ))).thenReturn("{\"event\":2}\n");

        ExportController controller = new ExportController(service);

        ResponseEntity<String> response = controller.downloadEvents(
                "lab/flow/out",
                "trace-2",
                "WS",
                "web",
                from,
                to
        );

        assertThat(response.getBody()).isEqualTo("{\"event\":2}\n");

        verify(service).exportNdjson(argThat(query ->
                "lab/flow/out".equals(query.channel())
                        && "trace-2".equals(query.traceId())
                        && "WS".equals(query.protocol())
                        && "web".equals(query.source())
                        && from.equals(query.from())
                        && to.equals(query.to())
        ));
    }
}