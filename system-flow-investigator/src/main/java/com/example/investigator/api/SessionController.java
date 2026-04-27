package com.example.investigator.api;

import com.example.investigator.domain.export.SessionImportSummary;
import com.example.investigator.service.SessionImportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionImportService sessionImportService;

    public SessionController(SessionImportService sessionImportService) {
        this.sessionImportService = sessionImportService;
    }

    @PostMapping("/import")
    public SessionImportSummary importSession(@RequestParam("file") MultipartFile file) throws Exception {
        return sessionImportService.importSession(file.getBytes());
    }
}