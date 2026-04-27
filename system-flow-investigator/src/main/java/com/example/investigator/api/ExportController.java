package com.example.investigator.api;

import com.example.investigator.domain.export.EventExportQuery;
import com.example.investigator.service.EventExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final EventExportService exportService;

    public ExportController(EventExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping(value = "/events", produces = "application/x-ndjson")
    public ResponseEntity<String> exportEvents(
            @RequestParam(name = "channel", required = false) String channel,
            @RequestParam(name = "traceId", required = false) String traceId,
            @RequestParam(name = "protocol", required = false) String protocol,
            @RequestParam(name = "source", required = false) String source,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        String ndjson = exportService.exportNdjson(
                new EventExportQuery(channel, traceId, protocol, source, from, to)
        );

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/x-ndjson"))
                .body(ndjson);
    }

    @GetMapping(value = "/events/download", produces = "application/x-ndjson")
    public ResponseEntity<String> downloadEvents(
            @RequestParam(name = "channel", required = false) String channel,
            @RequestParam(name = "traceId", required = false) String traceId,
            @RequestParam(name = "protocol", required = false) String protocol,
            @RequestParam(name = "source", required = false) String source,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        String ndjson = exportService.exportNdjson(
                new EventExportQuery(channel, traceId, protocol, source, from, to)
        );

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/x-ndjson"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"investigation-events.ndjson\""
                )
                .body(ndjson);
    }
}