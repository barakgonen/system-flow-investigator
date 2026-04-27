package com.example.investigator.service;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.config.FlowDefinition;
import com.example.investigator.domain.config.InvestigationConfig;
import com.example.investigator.domain.export.SessionExportMetadata;
import com.example.investigator.domain.export.SessionImportSummary;
import com.example.investigator.domain.session.ImportedSession;
import com.example.investigator.storage.ImportedSessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipInputStream;

@Service
public class SessionImportService {

    private final ImportedSessionStore importedSessionStore;
    private final ObjectMapper objectMapper;

    public SessionImportService(ImportedSessionStore importedSessionStore) {
        this.importedSessionStore = importedSessionStore;
        this.objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public SessionImportSummary importSession(byte[] zipBytes) {
        try {
            ImportedZipContent content = readZip(zipBytes);

            if (content.metadataJson == null) {
                throw new IllegalArgumentException("Missing metadata.json");
            }

            if (content.configJson == null) {
                throw new IllegalArgumentException("Missing config.json");
            }

            if (content.eventsNdjson == null) {
                throw new IllegalArgumentException("Missing events.ndjson");
            }

            SessionExportMetadata metadata = objectMapper.readValue(
                    content.metadataJson,
                    SessionExportMetadata.class
            );

            InvestigationConfig config = objectMapper.readValue(
                    content.configJson,
                    InvestigationConfig.class
            );

            List<ObservedEvent> events = parseEvents(content.eventsNdjson);

            String sessionId = UUID.randomUUID().toString();

            ImportedSession session = new ImportedSession(
                    sessionId,
                    config.name(),
                    Instant.now(),
                    metadata,
                    config,
                    events
            );

            importedSessionStore.save(session);

            List<String> flowIds = config.flows() == null
                    ? List.of()
                    : config.flows().stream()
                    .map(FlowDefinition::id)
                    .toList();

            return new SessionImportSummary(
                    "IMPORTED",
                    "Session package imported successfully.",
                    sessionId,
                    metadata.format(),
                    metadata.version(),
                    metadata.exportedAt(),
                    events.size(),
                    config.name(),
                    flowIds
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed importing investigation session", e);
        }
    }

    private ImportedZipContent readZip(byte[] zipBytes) throws Exception {
        ImportedZipContent content = new ImportedZipContent();

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                String entryContent = new String(zip.readAllBytes(), StandardCharsets.UTF_8);

                switch (entry.getName()) {
                    case "metadata.json" -> content.metadataJson = entryContent;
                    case "config.json" -> content.configJson = entryContent;
                    case "events.ndjson" -> content.eventsNdjson = entryContent;
                    default -> {
                        // Ignore unknown files for forward compatibility.
                    }
                }

                zip.closeEntry();
            }
        }

        return content;
    }

    private List<ObservedEvent> parseEvents(String ndjson) throws Exception {
        List<ObservedEvent> events = new ArrayList<>();

        if (ndjson == null || ndjson.isBlank()) {
            return events;
        }

        for (String line : ndjson.split("\\R")) {
            if (line == null || line.isBlank()) {
                continue;
            }

            events.add(objectMapper.readValue(line, ObservedEvent.class));
        }

        return events;
    }

    private static class ImportedZipContent {
        private String metadataJson;
        private String configJson;
        private String eventsNdjson;
    }
}