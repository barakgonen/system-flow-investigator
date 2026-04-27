package com.example.investigator.api;

import com.example.investigator.ingestion.IngestionSourceRegistry;
import com.example.investigator.ingestion.IngestionSourceStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingestion/sources")
public class IngestionSourceController {

    private final IngestionSourceRegistry registry;

    public IngestionSourceController(IngestionSourceRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<IngestionSourceStatus> sources() {
        return registry.statuses();
    }

    @GetMapping("/{sourceId}")
    public IngestionSourceStatus source(@PathVariable String sourceId) {
        return registry.get(sourceId).status();
    }
}