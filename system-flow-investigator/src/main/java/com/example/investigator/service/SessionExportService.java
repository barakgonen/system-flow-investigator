package com.example.investigator.service;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.config.InvestigationConfig;
import com.example.investigator.domain.export.EventExportQuery;
import com.example.investigator.domain.export.SessionExportMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class SessionExportService {

    private final EventExportService eventExportService;
    private final InvestigationConfigService configService;
    private final ObjectMapper objectMapper;

    public SessionExportService(EventExportService eventExportService,
                                InvestigationConfigService configService) {
        this.eventExportService = eventExportService;
        this.configService = configService;
        this.objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public byte[] exportSession(EventExportQuery query) {
        try {
            List<ObservedEvent> events = eventExportService.exportEvents(query);
            String eventsNdjson = eventExportService.exportNdjson(query);
            InvestigationConfig config = configService.getConfig();

            SessionExportMetadata metadata = new SessionExportMetadata(
                    "system-flow-investigator-session",
                    "1.0",
                    Instant.now(),
                    query,
                    events.size(),
                    Map.of(
                            "metadata", "metadata.json",
                            "config", "config.json",
                            "events", "events.ndjson"
                    )
            );

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                addJson(zip, "metadata.json", metadata);
                addJson(zip, "config.json", config);
                addText(zip, "events.ndjson", eventsNdjson);
            }

            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed exporting investigation session", e);
        }
    }

    private void addJson(ZipOutputStream zip, String fileName, Object value) throws Exception {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        addText(zip, fileName, json);
    }

    private void addText(ZipOutputStream zip, String fileName, String content) throws Exception {
        ZipEntry entry = new ZipEntry(fileName);
        zip.putNextEntry(entry);
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}