package com.example.investigator.api;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.TraceTimelineResponse;
import com.example.investigator.domain.export.SessionImportSummary;
import com.example.investigator.domain.session.ImportedSessionSummary;
import com.example.investigator.service.ImportedSessionService;
import com.example.investigator.service.SessionImportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionImportService sessionImportService;
    private final ImportedSessionService importedSessionService;

    public SessionController(SessionImportService sessionImportService,
                             ImportedSessionService importedSessionService) {
        this.sessionImportService = sessionImportService;
        this.importedSessionService = importedSessionService;
    }

    @PostMapping("/import")
    public SessionImportSummary importSession(@RequestParam(name = "file") MultipartFile file) throws Exception {
        return sessionImportService.importSession(file.getBytes());
    }

    @GetMapping("/imported")
    public List<ImportedSessionSummary> importedSessions() {
        return importedSessionService.listImportedSessions();
    }

    @GetMapping("/imported/{sessionId}/events")
    public List<ObservedEvent> importedEvents(@PathVariable("sessionId") String sessionId) {
        return importedSessionService.events(sessionId);
    }

    @GetMapping("/imported/{sessionId}/trace/{traceId}")
    public TraceTimelineResponse importedTrace(@PathVariable("sessionId") String sessionId,
                                               @PathVariable("traceId") String traceId,
                                               @RequestParam(name = "flowId", required = false) String flowId) {
        return importedSessionService.trace(sessionId, traceId, flowId);
    }
}