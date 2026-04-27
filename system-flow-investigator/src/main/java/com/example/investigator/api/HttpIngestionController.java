package com.example.investigator.api;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.ingestion.http.HttpIngestionEventRequest;
import com.example.investigator.ingestion.http.HttpIngestionSource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ingestion/http")
public class HttpIngestionController {

    private final HttpIngestionSource source;

    public HttpIngestionController(HttpIngestionSource source) {
        this.source = source;
    }

    @PostMapping("/events")
    public ObservedEvent ingest(@RequestBody HttpIngestionEventRequest request) {
        return source.ingest(request);
    }
}