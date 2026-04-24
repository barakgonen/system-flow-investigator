package com.example.investigator.storage;

import com.example.investigator.domain.ObservedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RecentEventStoreTests {

    private RecentEventStore store;

    @BeforeEach
    public void setUp() {
        store = new RecentEventStore();
    }

    // == add

    @Test
    public void testAdd_singleEvent_isRetrievable() {
        // == Arrange
        ObservedEvent event = observedEvent("topic/a", "trace-1", "2024-01-01T00:00:00Z");

        // == Act
        store.add(event);

        // == Assert
        assertEquals(List.of(event), store.getRecent("topic/a"));
    }

    @Test
    public void testAdd_multipleEvents_sameChannel_allStored() {
        // == Arrange
        ObservedEvent event1 = observedEvent("topic/a", "trace-1", "2024-01-01T00:00:00Z");
        ObservedEvent event2 = observedEvent("topic/a", "trace-2", "2024-01-01T00:00:01Z");
        ObservedEvent event3 = observedEvent("topic/a", "trace-3", "2024-01-01T00:00:02Z");

        // == Act
        store.add(event1);
        store.add(event2);
        store.add(event3);

        // == Assert
        assertEquals(List.of(event1, event2, event3), store.getRecent("topic/a"));
    }

    @Test
    public void testAdd_multipleEvents_differentChannels_storedSeparately() {
        // == Arrange
        ObservedEvent eventA = observedEvent("topic/a", "trace-1", "2024-01-01T00:00:00Z");
        ObservedEvent eventB = observedEvent("topic/b", "trace-2", "2024-01-01T00:00:01Z");

        // == Act
        store.add(eventA);
        store.add(eventB);

        // == Assert
        assertEquals(List.of(eventA), store.getRecent("topic/a"));
        assertEquals(List.of(eventB), store.getRecent("topic/b"));
    }

    @Test
    public void testAdd_exceedsMaxPerChannel_evictsOldest() {
        // == Arrange
        ObservedEvent firstEvent = observedEvent("topic/a", "trace-0", "2024-01-01T00:00:00Z");
        store.add(firstEvent);

        for (int i = 1; i <= 10_000; i++) {
            store.add(observedEvent("topic/a", "trace-" + i, "2024-01-01T00:00:0" + (i % 10) + "Z"));
        }

        // == Assert
        List<ObservedEvent> recent = store.getRecent("topic/a");
        assertEquals(10_000, recent.size());
        assertFalse(recent.contains(firstEvent));
    }

    // == getRecent

    @Test
    public void testGetRecent_unknownChannel_returnsEmptyList() {
        // == Act
        List<ObservedEvent> result = store.getRecent("topic/unknown");

        // == Assert
        assertEquals(List.of(), result);
    }

    @Test
    public void testGetRecent_returnsEventsInInsertionOrder() {
        // == Arrange
        ObservedEvent event1 = observedEvent("topic/a", "trace-1", "2024-01-01T00:00:00Z");
        ObservedEvent event2 = observedEvent("topic/a", "trace-2", "2024-01-01T00:00:01Z");
        ObservedEvent event3 = observedEvent("topic/a", "trace-3", "2024-01-01T00:00:02Z");

        store.add(event1);
        store.add(event2);
        store.add(event3);

        // == Act
        List<ObservedEvent> result = store.getRecent("topic/a");

        // == Assert
        assertEquals(List.of(event1, event2, event3), result);
    }

    @Test
    public void testGetRecent_returnsCopy_mutationDoesNotAffectStore() {
        // == Arrange
        ObservedEvent event = observedEvent("topic/a", "trace-1", "2024-01-01T00:00:00Z");
        store.add(event);

        // == Act
        List<ObservedEvent> result = store.getRecent("topic/a");
        result.clear();

        // == Assert
        assertEquals(List.of(event), store.getRecent("topic/a"));
    }

    // == channels

    @Test
    public void testChannels_emptyStore_returnsEmptySet() {
        // == Act & Assert
        assertEquals(Set.of(), store.channels());
    }

    @Test
    public void testChannels_returnsAllKnownChannels() {
        // == Arrange
        store.add(observedEvent("topic/a", "trace-1", "2024-01-01T00:00:00Z"));
        store.add(observedEvent("topic/b", "trace-2", "2024-01-01T00:00:01Z"));
        store.add(observedEvent("topic/c", "trace-3", "2024-01-01T00:00:02Z"));

        // == Act & Assert
        assertEquals(Set.of("topic/a", "topic/b", "topic/c"), store.channels());
    }

    // == getAllRecent

    @Test
    public void testGetAllRecent_emptyStore_returnsEmptyList() {
        // == Act & Assert
        assertEquals(List.of(), store.getAllRecent());
    }

    @Test
    public void testGetAllRecent_singleChannel_returnsAllEvents() {
        // == Arrange
        ObservedEvent event1 = observedEvent("topic/a", "trace-1", "2024-01-01T00:00:00Z");
        ObservedEvent event2 = observedEvent("topic/a", "trace-2", "2024-01-01T00:00:01Z");

        store.add(event1);
        store.add(event2);

        // == Act & Assert
        assertEquals(List.of(event1, event2), store.getAllRecent());
    }

    @Test
    public void testGetAllRecent_multipleChannels_sortedByReceivedAt() {
        // == Arrange
        ObservedEvent event1 = observedEvent("topic/a", "trace-1", "2024-01-01T00:00:00Z");
        ObservedEvent event2 = observedEvent("topic/b", "trace-2", "2024-01-01T00:00:01Z");
        ObservedEvent event3 = observedEvent("topic/a", "trace-3", "2024-01-01T00:00:02Z");

        store.add(event3);
        store.add(event1);
        store.add(event2);

        // == Act
        List<ObservedEvent> result = store.getAllRecent();

        // == Assert
        assertEquals(List.of(event1, event2, event3), result);
    }

    // == Helpers

    private ObservedEvent observedEvent(String channel, String traceId, String receivedAt) {
        return new ObservedEvent(
                "MQTT", "broker-1", channel,
                Instant.parse(receivedAt),
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                traceId
        );
    }
}