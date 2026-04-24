package com.example.investigator.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ObservedEventTests {

    @Test
    public void testConstructorAndAccessors() {
        Instant observedAt = Instant.now();
        Instant sourceSentAt = Instant.parse("2024-01-01T00:00:01Z");

        ObservedEvent event = new ObservedEvent(
                "MQTT",
                "broker-1",
                "topic/a",
                observedAt,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123",
                sourceSentAt
        );

        assertEquals("MQTT", event.protocol());
        assertEquals("broker-1", event.source());
        assertEquals("topic/a", event.channel());
        assertEquals(observedAt, event.observedAt());
        assertEquals("{\"key\":\"value\"}", event.payload());
        assertEquals(Map.of("content-type", "application/json"), event.metadata());
        assertEquals("trace-123", event.traceId());
        assertEquals(sourceSentAt, event.sourceSentAt());
    }

    @Test
    public void testEquality_equalObjects() {
        Instant observedAt = Instant.parse("2024-01-01T00:00:00Z");
        Instant sourceSentAt = Instant.parse("2024-01-01T00:00:01Z");

        ObservedEvent event1 = new ObservedEvent(
                "MQTT",
                "broker-1",
                "topic/a",
                observedAt,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123",
                sourceSentAt
        );

        ObservedEvent event2 = new ObservedEvent(
                "MQTT",
                "broker-1",
                "topic/a",
                observedAt,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123",
                sourceSentAt
        );

        assertEquals(event1, event2);
    }

    @Test
    public void testEquality_differentObjects() {
        Instant observedAt = Instant.parse("2024-01-01T00:00:00Z");

        ObservedEvent event1 = new ObservedEvent(
                "MQTT",
                "broker-1",
                "topic/a",
                observedAt,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123",
                Instant.parse("2024-01-01T00:00:01Z")
        );

        ObservedEvent event2 = new ObservedEvent(
                "WS",
                "broker-2",
                "topic/b",
                observedAt,
                "{\"other\":\"data\"}",
                Map.of("content-type", "text/plain"),
                "trace-456",
                Instant.parse("2024-01-01T00:00:02Z")
        );

        assertNotEquals(event1, event2);
    }

    @Test
    public void testHashCode_equalObjects() {
        Instant observedAt = Instant.parse("2024-01-01T00:00:00Z");
        Instant sourceSentAt = Instant.parse("2024-01-01T00:00:01Z");

        ObservedEvent event1 = new ObservedEvent(
                "MQTT",
                "broker-1",
                "topic/a",
                observedAt,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123",
                sourceSentAt
        );

        ObservedEvent event2 = new ObservedEvent(
                "MQTT",
                "broker-1",
                "topic/a",
                observedAt,
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123",
                sourceSentAt
        );

        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    public void testToString_containsAllFields() {
        ObservedEvent event = new ObservedEvent(
                "MQTT",
                "broker-1",
                "topic/a",
                Instant.parse("2024-01-01T00:00:00Z"),
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                "trace-123",
                Instant.parse("2024-01-01T00:00:01Z")
        );

        String result = event.toString();

        assertTrue(result.contains("MQTT"));
        assertTrue(result.contains("broker-1"));
        assertTrue(result.contains("topic/a"));
        assertTrue(result.contains("2024-01-01T00:00:00Z"));
        assertTrue(result.contains("2024-01-01T00:00:01Z"));
        assertTrue(result.contains("trace-123"));
    }

    @Test
    public void testNullFields() {
        ObservedEvent event = new ObservedEvent(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertNull(event.protocol());
        assertNull(event.source());
        assertNull(event.channel());
        assertNull(event.observedAt());
        assertNull(event.payload());
        assertNull(event.metadata());
        assertNull(event.traceId());
        assertNull(event.sourceSentAt());
    }

    @Test
    public void testEmptyMetadata() {
        ObservedEvent event = new ObservedEvent(
                "MQTT",
                "broker-1",
                "topic/a",
                Instant.now(),
                "payload",
                Map.of(),
                "trace-123",
                null
        );

        assertEquals(Map.of(), event.metadata());
    }

    @Test
    public void testMultipleMetadataEntries() {
        Map<String, String> metadata = Map.of(
                "content-type", "application/json",
                "x-trace-id", "trace-123",
                "x-source", "broker-1"
        );

        ObservedEvent event = new ObservedEvent(
                "MQTT",
                "broker-1",
                "topic/a",
                Instant.now(),
                "payload",
                metadata,
                "trace-123",
                Instant.parse("2024-01-01T00:00:01Z")
        );

        assertEquals(metadata, event.metadata());
        assertEquals(3, event.metadata().size());
    }
}