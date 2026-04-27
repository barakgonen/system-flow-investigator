package com.example.investigator.api;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.TraceTimelineResponse;
import com.example.investigator.domain.export.SessionImportSummary;
import com.example.investigator.domain.session.ImportedSessionSummary;
import com.example.investigator.service.ImportedSessionService;
import com.example.investigator.service.SessionImportService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SessionControllerTests {

    @Test
    void shouldDelegateImportToService() throws Exception {
        SessionImportService importService = mock(SessionImportService.class);
        ImportedSessionService importedSessionService = mock(ImportedSessionService.class);

        byte[] bytes = new byte[]{1, 2, 3};

        SessionImportSummary summary = new SessionImportSummary(
                "IMPORTED",
                "Session package imported successfully.",
                "session-1",
                "system-flow-investigator-session",
                "1.0",
                Instant.parse("2026-04-27T04:50:00Z"),
                3,
                "Main Investigation",
                List.of("main-lab-flow")
        );

        when(importService.importSession(bytes)).thenReturn(summary);

        SessionController controller = new SessionController(importService, importedSessionService);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "investigation-session.zip",
                "application/zip",
                bytes
        );

        SessionImportSummary result = controller.importSession(file);

        assertThat(result).isSameAs(summary);
        verify(importService).importSession(bytes);
        verifyNoInteractions(importedSessionService);
    }

    @Test
    void shouldDelegateImportedSessionsLookup() {
        SessionImportService importService = mock(SessionImportService.class);
        ImportedSessionService importedSessionService = mock(ImportedSessionService.class);

        List<ImportedSessionSummary> summaries = List.of(
                new ImportedSessionSummary(
                        "session-1",
                        "Imported Investigation",
                        Instant.parse("2026-04-27T05:00:00Z"),
                        3
                )
        );

        when(importedSessionService.listImportedSessions()).thenReturn(summaries);

        SessionController controller = new SessionController(importService, importedSessionService);

        List<ImportedSessionSummary> result = controller.importedSessions();

        assertThat(result).isSameAs(summaries);
        verify(importedSessionService).listImportedSessions();
    }

    @Test
    void shouldDelegateImportedEventsLookup() {
        SessionImportService importService = mock(SessionImportService.class);
        ImportedSessionService importedSessionService = mock(ImportedSessionService.class);

        List<ObservedEvent> events = List.of(event("trace-1"));

        when(importedSessionService.events("session-1")).thenReturn(events);

        SessionController controller = new SessionController(importService, importedSessionService);

        List<ObservedEvent> result = controller.importedEvents("session-1");

        assertThat(result).isSameAs(events);
        verify(importedSessionService).events("session-1");
    }

    @Test
    void shouldDelegateImportedTraceLookup() {
        SessionImportService importService = mock(SessionImportService.class);
        ImportedSessionService importedSessionService = mock(ImportedSessionService.class);

        TraceTimelineResponse response = new TraceTimelineResponse(
                "trace-1",
                0,
                null,
                null,
                null,
                null,
                List.of(),
                null
        );

        when(importedSessionService.trace("session-1", "trace-1", "main-lab-flow"))
                .thenReturn(response);

        SessionController controller = new SessionController(importService, importedSessionService);

        TraceTimelineResponse result = controller.importedTrace(
                "session-1",
                "trace-1",
                "main-lab-flow"
        );

        assertThat(result).isSameAs(response);
        verify(importedSessionService).trace("session-1", "trace-1", "main-lab-flow");
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