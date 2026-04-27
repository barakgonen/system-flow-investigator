package com.example.investigator.api;

import com.example.investigator.service.EventExportService;
import com.example.investigator.service.SessionExportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExportControllerTests {

    @Test
    void shouldExportEventsAsNdjson() {
        EventExportService eventExportService = mock(EventExportService.class);
        SessionExportService sessionExportService = mock(SessionExportService.class);

        when(eventExportService.exportNdjson(argThat(query ->
                query.channel() == null
                        && query.traceId() == null
                        && query.protocol() == null
                        && query.source() == null
                        && query.from() == null
                        && query.to() == null
        ))).thenReturn("{\"event\":1}\n");

        ExportController controller = new ExportController(eventExportService, sessionExportService);

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

        verify(eventExportService).exportNdjson(any());
        verifyNoInteractions(sessionExportService);
    }

    @Test
    void shouldPassFiltersToExportService() {
        EventExportService eventExportService = mock(EventExportService.class);
        SessionExportService sessionExportService = mock(SessionExportService.class);

        Instant from = Instant.parse("2026-04-27T04:49:00Z");
        Instant to = Instant.parse("2026-04-27T04:50:00Z");

        when(eventExportService.exportNdjson(argThat(query ->
                "lab/flow/in".equals(query.channel())
                        && "trace-1".equals(query.traceId())
                        && "MQTT".equals(query.protocol())
                        && "localhost:1883".equals(query.source())
                        && from.equals(query.from())
                        && to.equals(query.to())
        ))).thenReturn("{\"event\":1}\n");

        ExportController controller = new ExportController(eventExportService, sessionExportService);

        ResponseEntity<String> response = controller.exportEvents(
                "lab/flow/in",
                "trace-1",
                "MQTT",
                "localhost:1883",
                from,
                to
        );

        assertThat(response.getBody()).isEqualTo("{\"event\":1}\n");

        verify(eventExportService).exportNdjson(argThat(query ->
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
        EventExportService eventExportService = mock(EventExportService.class);
        SessionExportService sessionExportService = mock(SessionExportService.class);

        when(eventExportService.exportNdjson(any()))
                .thenReturn("{\"event\":1}\n");

        ExportController controller = new ExportController(eventExportService, sessionExportService);

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

        verify(eventExportService).exportNdjson(any());
    }

    @Test
    void shouldPassFiltersToDownloadService() {
        EventExportService eventExportService = mock(EventExportService.class);
        SessionExportService sessionExportService = mock(SessionExportService.class);

        Instant from = Instant.parse("2026-04-27T04:49:00Z");
        Instant to = Instant.parse("2026-04-27T04:50:00Z");

        when(eventExportService.exportNdjson(argThat(query ->
                "lab/flow/out".equals(query.channel())
                        && "trace-2".equals(query.traceId())
                        && "WS".equals(query.protocol())
                        && "web".equals(query.source())
                        && from.equals(query.from())
                        && to.equals(query.to())
        ))).thenReturn("{\"event\":2}\n");

        ExportController controller = new ExportController(eventExportService, sessionExportService);

        ResponseEntity<String> response = controller.downloadEvents(
                "lab/flow/out",
                "trace-2",
                "WS",
                "web",
                from,
                to
        );

        assertThat(response.getBody()).isEqualTo("{\"event\":2}\n");

        verify(eventExportService).exportNdjson(argThat(query ->
                "lab/flow/out".equals(query.channel())
                        && "trace-2".equals(query.traceId())
                        && "WS".equals(query.protocol())
                        && "web".equals(query.source())
                        && from.equals(query.from())
                        && to.equals(query.to())
        ));
    }

    @Test
    void shouldDownloadFullSessionAsZipAttachment() {
        EventExportService eventExportService = mock(EventExportService.class);
        SessionExportService sessionExportService = mock(SessionExportService.class);

        byte[] zipBytes = new byte[]{1, 2, 3};

        when(sessionExportService.exportSession(any()))
                .thenReturn(zipBytes);

        ExportController controller = new ExportController(eventExportService, sessionExportService);

        ResponseEntity<byte[]> response = controller.downloadSession(
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("application/zip");
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=\"investigation-session.zip\"");
        assertThat(response.getBody()).containsExactly(1, 2, 3);

        verify(sessionExportService).exportSession(any());
    }

    @Test
    void shouldPassFiltersToSessionExportService() {
        EventExportService eventExportService = mock(EventExportService.class);
        SessionExportService sessionExportService = mock(SessionExportService.class);

        Instant from = Instant.parse("2026-04-27T04:49:00Z");
        Instant to = Instant.parse("2026-04-27T04:50:00Z");

        when(sessionExportService.exportSession(argThat(query ->
                "lab/flow/in".equals(query.channel())
                        && "trace-1".equals(query.traceId())
                        && "MQTT".equals(query.protocol())
                        && "localhost:1883".equals(query.source())
                        && from.equals(query.from())
                        && to.equals(query.to())
        ))).thenReturn(new byte[]{9});

        ExportController controller = new ExportController(eventExportService, sessionExportService);

        ResponseEntity<byte[]> response = controller.downloadSession(
                "lab/flow/in",
                "trace-1",
                "MQTT",
                "localhost:1883",
                from,
                to
        );

        assertThat(response.getBody()).containsExactly(9);

        verify(sessionExportService).exportSession(argThat(query ->
                "lab/flow/in".equals(query.channel())
                        && "trace-1".equals(query.traceId())
                        && "MQTT".equals(query.protocol())
                        && "localhost:1883".equals(query.source())
                        && from.equals(query.from())
                        && to.equals(query.to())
        ));
    }
}