package com.example.investigator.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardSummaryTests {

    @Test
    public void testConstructorAndAccessors() {
        // == Arrange & Act
        DashboardSummary summary = new DashboardSummary(
                true, 5, 3, 42,
                List.of("topic/a", "topic/b"),
                List.of("trace-1", "trace-2")
        );

        // == Assert
        assertTrue(summary.mqttConnected());
        assertEquals(5, summary.observedTopicCount());
        assertEquals(3, summary.observedWebSocketChannelCount());
        assertEquals(42, summary.recentEventCount());
        assertEquals(List.of("topic/a", "topic/b"), summary.latestTopics());
        assertEquals(List.of("trace-1", "trace-2"), summary.latestTraceIds());
    }

    @Test
    public void testEquality_equalObjects() {
        // == Arrange
        DashboardSummary summary1 = new DashboardSummary(
                true, 5, 3, 42,
                List.of("topic/a", "topic/b"),
                List.of("trace-1", "trace-2")
        );
        DashboardSummary summary2 = new DashboardSummary(
                true, 5, 3, 42,
                List.of("topic/a", "topic/b"),
                List.of("trace-1", "trace-2")
        );

        // == Act & Assert
        assertEquals(summary1, summary2);
    }

    @Test
    public void testEquality_differentObjects() {
        // == Arrange
        DashboardSummary summary1 = new DashboardSummary(
                true, 5, 3, 42,
                List.of("topic/a", "topic/b"),
                List.of("trace-1", "trace-2")
        );
        DashboardSummary summary2 = new DashboardSummary(
                false, 2, 1, 10,
                List.of("topic/c"),
                List.of("trace-3")
        );

        // == Act & Assert
        assertNotEquals(summary1, summary2);
    }

    @Test
    public void testHashCode_equalObjects() {
        // == Arrange
        DashboardSummary summary1 = new DashboardSummary(
                true, 5, 3, 42,
                List.of("topic/a", "topic/b"),
                List.of("trace-1", "trace-2")
        );
        DashboardSummary summary2 = new DashboardSummary(
                true, 5, 3, 42,
                List.of("topic/a", "topic/b"),
                List.of("trace-1", "trace-2")
        );

        // == Act & Assert
        assertEquals(summary1.hashCode(), summary2.hashCode());
    }

    @Test
    public void testToString_containsAllFields() {
        // == Arrange
        DashboardSummary summary = new DashboardSummary(
                true, 5, 3, 42,
                List.of("topic/a", "topic/b"),
                List.of("trace-1", "trace-2")
        );

        // == Act
        String result = summary.toString();

        // == Assert
        assertTrue(result.contains("true"));
        assertTrue(result.contains("5"));
        assertTrue(result.contains("3"));
        assertTrue(result.contains("42"));
        assertTrue(result.contains("topic/a"));
        assertTrue(result.contains("topic/b"));
        assertTrue(result.contains("trace-1"));
        assertTrue(result.contains("trace-2"));
    }

    @Test
    public void testMqttConnected_false() {
        // == Arrange & Act
        DashboardSummary summary = new DashboardSummary(
                false, 0, 0, 0, List.of(), List.of()
        );

        // == Assert
        assertFalse(summary.mqttConnected());
    }

    @Test
    public void testNullFields() {
        // == Arrange & Act
        DashboardSummary summary = new DashboardSummary(
                false, 0, 0, 0, null, null
        );

        // == Assert
        assertNull(summary.latestTopics());
        assertNull(summary.latestTraceIds());
    }

    @Test
    public void testEmptyLists() {
        // == Arrange & Act
        DashboardSummary summary = new DashboardSummary(
                false, 0, 0, 0, List.of(), List.of()
        );

        // == Assert
        assertEquals(List.of(), summary.latestTopics());
        assertEquals(List.of(), summary.latestTraceIds());
    }
}