package com.example.investigator.storage;

import com.example.investigator.domain.ObservedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
public class MessageFileSink {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Path filePath = Path.of("data", "investigator-events.ndjson");

    public synchronized void append(ObservedEvent event) {
        try {
            Files.createDirectories(filePath.getParent());
            String jsonLine = objectMapper.writeValueAsString(event) + System.lineSeparator();
            Files.writeString(
                    filePath,
                    jsonLine,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist observed event", e);
        }
    }
}