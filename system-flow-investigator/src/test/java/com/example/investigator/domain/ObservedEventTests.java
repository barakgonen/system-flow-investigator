package com.example.investigator.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ObservedEventTests {

    @Test
    public void testConstructorAndAccessors() {
        // == Arrange
        Instant now = Instant.now();

        // == Act
        ObservedEvent event = new ObservedEvent(
                "MQTT", "broker-1", "topic/a", now,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123"
        );

        // == Assert
        assertEquals("MQTT", event.protocol());
        assertEquals("broker-1", event.sourceName());
        assertEquals("topic/a", event.channel());
        assertEquals(now, event.receivedAt());
        assertEquals("{\"key\":\"value\"}", event.payload());
        assertEquals(Map.of("content-type", "application/json"), event.headers());
        assertEquals("trace-123", event.traceId());
    }

    @Test
    public void testEquality_equalObjects() {
        // == Arrange
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        ObservedEvent event1 = new ObservedEvent(
                "MQTT", "broker-1", "topic/a", now,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123"
        );
        ObservedEvent event2 = new ObservedEvent(
                "MQTT", "broker-1", "topic/a", now,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123"
        );

        // == Act & Assert
        assertEquals(event1, event2);
    }

    @Test
    public void testEquality_differentObjects() {
        // == Arrange
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        ObservedEvent event1 = new ObservedEvent(
                "MQTT", "broker-1", "topic/a", now,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123"
        );
        ObservedEvent event2 = new ObservedEvent(
                "WS", "broker-2", "topic/b", now,
                "{\"other\":\"data\"}",
                Map.of("content-type", "text/plain"),
                "trace-456"
        );

        // == Act & Assert
        assertNotEquals(event1, event2);
    }

    @Test
    public void testHashCode_equalObjects() {
        // == Arrange
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        ObservedEvent event1 = new ObservedEvent(
                "MQTT", "broker-1", "topic/a", now,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123"
        );
        ObservedEvent event2 = new ObservedEvent(
                "MQTT", "broker-1", "topic/a", now,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123"
        );

        // == Act & Assert
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    public void testToString_containsAllFields() {
        // == Arrange
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        ObservedEvent event = new ObservedEvent(
                "MQTT", "broker-1", "topic/a", now,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123"
        );

        // == Act
        String result = event.toString();

        // == Assert
        assertTrue(result.contains("MQTT"));
        assertTrue(result.contains("broker-1"));
        assertTrue(result.contains("topic/a"));
        assertTrue(result.contains("2024-01-01T00:00:00Z"));
        assertTrue(result.contains("trace-123"));
    }

    @Test
    public void testNullFields() {
        // == Arrange & Act
        ObservedEvent event = new ObservedEvent(
                null, null, null, null, null, null, null
        );

        // == Assert
        assertNull(event.protocol());
        assertNull(event.sourceName());
        assertNull(event.channel());
        assertNull(event.receivedAt());
        assertNull(event.payload());
        assertNull(event.headers());
        assertNull(event.traceId());
    }

    @Test
    public void testEmptyHeaders() {
        // == Arrange & Act
        ObservedEvent event = new ObservedEvent(
                "MQTT", "broker-1", "topic/a", Instant.now(),
                "payload", Map.of(), "trace-123"
        );

        // == Assert
        assertEquals(Map.of(), event.headers());
    }

    @Test
    public void testMultipleHeaders() {
        // == Arrange
        Map<String, String> headers = Map.of(
                "content-type", "application/json",
                "x-trace-id", "trace-123",
                "x-source", "broker-1"
        );

        // == Act
        ObservedEvent event = new ObservedEvent(
                "MQTT", "broker-1", "topic/a", Instant.now(),
                "payload", headers, "trace-123"
        );

        // == Assert
        assertEquals(headers, event.headers());
        assertEquals(3, event.headers().size());
    }
}