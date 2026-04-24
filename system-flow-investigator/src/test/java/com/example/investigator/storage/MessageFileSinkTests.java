package com.example.investigator.storage;

import com.example.investigator.domain.ObservedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MessageFileSinkTests {

    @TempDir
    Path tempDir;

    private MessageFileSink messageFlieSink;
    private Path filePath;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    public void setUp() throws Exception {
        messageFlieSink = new MessageFileSink();
        filePath = tempDir.resolve("investigator-events.ndjson");

        // Override the hardcoded filePath via reflection
        Field field = MessageFileSink.class.getDeclaredField("filePath");
        field.setAccessible(true);
        field.set(messageFlieSink, filePath);
    }

    @Test
    public void testAppend_createsFileIfNotExists() {
        // == Arrange
        ObservedEvent event = observedEvent("trace-1");

        // == Act
        messageFlieSink.append(event);

        // == Assert
        assertTrue(Files.exists(filePath));
    }

    @Test
    public void testAppend_writesValidJsonLine() throws Exception {
        // == Arrange
        ObservedEvent event = observedEvent("trace-1");

        // == Act
        messageFlieSink.append(event);

        // == Assert
        List<String> lines = Files.readAllLines(filePath);
        assertEquals(1, lines.size());
        ObservedEvent deserialized = objectMapper.readValue(lines.get(0), ObservedEvent.class);
        assertEquals(event, deserialized);
    }

    @Test
    public void testAppend_multipleEvents_eachOnOwnLine() throws Exception {
        // == Arrange
        ObservedEvent event1 = observedEvent("trace-1");
        ObservedEvent event2 = observedEvent("trace-2");
        ObservedEvent event3 = observedEvent("trace-3");

        // == Act
        messageFlieSink.append(event1);
        messageFlieSink.append(event2);
        messageFlieSink.append(event3);

        // == Assert
        List<String> lines = Files.readAllLines(filePath);
        assertEquals(3, lines.size());
        assertEquals(event1, objectMapper.readValue(lines.get(0), ObservedEvent.class));
        assertEquals(event2, objectMapper.readValue(lines.get(1), ObservedEvent.class));
        assertEquals(event3, objectMapper.readValue(lines.get(2), ObservedEvent.class));
    }

    @Test
    public void testAppend_appendsToExistingFile() throws Exception {
        // == Arrange
        ObservedEvent event1 = observedEvent("trace-1");
        ObservedEvent event2 = observedEvent("trace-2");

        // == Act
        messageFlieSink.append(event1);
        messageFlieSink.append(event2);

        // == Assert
        List<String> lines = Files.readAllLines(filePath);
        assertEquals(2, lines.size());
    }

    @Test
    public void testAppend_ioException_throwsIllegalStateException() throws Exception {
        // == Arrange
        // Make the file path point to a directory instead of a file — writing to a directory triggers IOException
        Files.createDirectories(filePath);

        Field field = MessageFileSink.class.getDeclaredField("filePath");
        field.setAccessible(true);
        field.set(messageFlieSink, filePath);

        ObservedEvent event = observedEvent("trace-1");

        // == Act & Assert
        assertThrows(IllegalStateException.class, () -> messageFlieSink.append(event));
    }

    // == Helpers

    private ObservedEvent observedEvent(String traceId) {
        return new ObservedEvent(
                "MQTT",
                "broker-1",
                "topic/a",
                Instant.parse("2024-01-01T00:00:00Z"),
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                traceId,
                Instant.parse("2024-01-01T00:00:01Z")
        );
    }
}