package com.example.investigator.api;

import com.example.investigator.domain.export.SessionImportSummary;
import com.example.investigator.service.SessionImportService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SessionControllerTests {

    @Test
    void shouldDelegateImportToService() throws Exception {
        SessionImportService service = mock(SessionImportService.class);

        byte[] bytes = new byte[]{1, 2, 3};

        SessionImportSummary summary = new SessionImportSummary(
                "IMPORTED",
                "Session package imported successfully.",
                "system-flow-investigator-session",
                "1.0",
                Instant.parse("2026-04-27T04:50:00Z"),
                3,
                "Main Investigation",
                List.of("main-lab-flow")
        );

        when(service.importSession(bytes)).thenReturn(summary);

        SessionController controller = new SessionController(service);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "investigation-session.zip",
                "application/zip",
                bytes
        );

        SessionImportSummary result = controller.importSession(file);

        assertThat(result).isSameAs(summary);
        verify(service).importSession(bytes);
    }
}