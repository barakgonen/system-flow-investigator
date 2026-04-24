package com.example.investigator.service;

import com.example.investigator.domain.*;
import com.example.investigator.ingestion.mqtt.MqttObserver;
import com.example.investigator.ingestion.websocket.WebSocketObserver;
import com.example.investigator.storage.RecentEventStore;
import com.example.investigator.stream.EventHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvestigatorFacadeTests {

    @Mock private EventHub eventHub;
    @Mock private RecentEventStore recentEventStore;
    @Mock private MqttObserver mqttObserver;
    @Mock private WebSocketObserver webSocketObserver;

    private InvestigatorFacade facade;

    @BeforeEach
    public void setUp() {
        facade = new InvestigatorFacade(eventHub, recentEventStore, mqttObserver, webSocketObserver);
    }

    // == streamEvents

    @Test
    public void testStreamEvents_delegatesToEventHub() {
        // == Arrange
        Flux<ObservedEvent> flux = Mockito.mock(Flux.class);
        when(eventHub.stream()).thenReturn(flux);

        // == Act
        facade.streamEvents();

        // == Assert
        verify(eventHub).stream();
    }

    // == getRecentEvents(String)

    @Test
    public void testGetRecentEvents_byChannel_delegatesToStore() {
        // == Arrange
        List<ObservedEvent> events = List.of(observedEvent("topic/a", "trace-1"));
        when(recentEventStore.getRecent("topic/a")).thenReturn(events);

        // == Act
        List<ObservedEvent> result = facade.getRecentEvents("topic/a");

        // == Assert
        assertEquals(events, result);
        verify(recentEventStore).getRecent("topic/a");
    }

    @Test
    public void testGetRecentEvents_nullChannel_returnsAllRecent() {
        // == Arrange
        List<ObservedEvent> events = List.of(observedEvent("topic/a", "trace-1"));
        when(recentEventStore.getAllRecent()).thenReturn(events);

        // == Act
        List<ObservedEvent> result = facade.getRecentEvents((String) null);

        // == Assert
        assertEquals(events, result);
        verify(recentEventStore).getAllRecent();
    }

    @Test
    public void testGetRecentEvents_blankChannel_returnsAllRecent() {
        // == Arrange
        List<ObservedEvent> events = List.of(observedEvent("topic/a", "trace-1"));
        when(recentEventStore.getAllRecent()).thenReturn(events);

        // == Act
        List<ObservedEvent> result = facade.getRecentEvents("   ");

        // == Assert
        assertEquals(events, result);
        verify(recentEventStore).getAllRecent();
    }

    // == getRecentEvents(Set, String, String)

    @Test
    public void testGetRecentEvents_withChannelFilter_returnsMatchingEvents() {
        // == Arrange
        ObservedEvent matchingEvent = observedEvent("topic/a", "trace-1");
        ObservedEvent nonMatchingEvent = observedEvent("topic/b", "trace-2");
        when(recentEventStore.getAllRecent()).thenReturn(List.of(matchingEvent, nonMatchingEvent));

        // == Act
        List<ObservedEvent> result = facade.getRecentEvents(Set.of("topic/a"), null, null);

        // == Assert
        assertEquals(List.of(matchingEvent), result);
    }

    @Test
    public void testGetRecentEvents_withTextContainsFilter_returnsMatchingEvents() {
        // == Arrange
        ObservedEvent matchingEvent = observedEventWithPayload("topic/a", "trace-1", "{\"key\":\"hello\"}");
        ObservedEvent nonMatchingEvent = observedEventWithPayload("topic/a", "trace-2", "{\"key\":\"world\"}");
        when(recentEventStore.getAllRecent()).thenReturn(List.of(matchingEvent, nonMatchingEvent));

        // == Act
        List<ObservedEvent> result = facade.getRecentEvents(Set.of(), "hello", null);

        // == Assert
        assertEquals(List.of(matchingEvent), result);
    }

    @Test
    public void testGetRecentEvents_withTraceIdFilter_returnsMatchingEvents() {
        // == Arrange
        ObservedEvent matchingEvent = observedEvent("topic/a", "trace-target");
        ObservedEvent nonMatchingEvent = observedEvent("topic/a", "trace-other");
        when(recentEventStore.getAllRecent()).thenReturn(List.of(matchingEvent, nonMatchingEvent));

        // == Act
        List<ObservedEvent> result = facade.getRecentEvents(Set.of(), null, "trace-target");

        // == Assert
        assertEquals(List.of(matchingEvent), result);
    }

    @Test
    public void testGetRecentEvents_nullPayload_doesNotThrow() {
        // == Arrange
        ObservedEvent eventWithNullPayload = new ObservedEvent(
                "MQTT",
                "broker-1",
                "topic/a",
                Instant.parse("2024-01-01T00:00:00Z"),
                null,
                Map.of(),
                "trace-1",
                Instant.parse("2024-01-01T00:00:00Z")
        );
        when(recentEventStore.getAllRecent()).thenReturn(List.of(eventWithNullPayload));

        // == Act & Assert
        assertDoesNotThrow(() -> facade.getRecentEvents(Set.of(), "hello", null));
    }

    @Test
    public void testGetRecentEvents_emptyChannels_noTextNoTrace_returnsAll() {
        // == Arrange
        ObservedEvent event1 = observedEvent("topic/a", "trace-1");
        ObservedEvent event2 = observedEvent("topic/b", "trace-2");
        when(recentEventStore.getAllRecent()).thenReturn(List.of(event1, event2));

        // == Act
        List<ObservedEvent> result = facade.getRecentEvents(Set.of(), null, null);

        // == Assert
        assertEquals(List.of(event1, event2), result);
    }

    // == observedMqttChannels

    @Test
    public void testObservedMqttChannels_delegatesToMqttObserver() {
        // == Arrange
        Set<String> channels = Set.of("topic/a", "topic/b");
        when(mqttObserver.observedChannels()).thenReturn(channels);

        // == Act & Assert
        assertEquals(channels, facade.observedMqttChannels());
        verify(mqttObserver).observedChannels();
    }

    // == observedWebSocketChannels

    @Test
    public void testObservedWebSocketChannels_delegatesToWebSocketObserver() {
        // == Arrange
        Set<String> channels = Set.of("ws-channel-1");
        when(webSocketObserver.observedChannels()).thenReturn(channels);

        // == Act & Assert
        assertEquals(channels, facade.observedWebSocketChannels());
        verify(webSocketObserver).observedChannels();
    }

    // == observedAllChannels

    @Test
    public void testObservedAllChannels_mergesMqttAndWebSocketChannels() {
        // == Arrange
        when(mqttObserver.observedChannels()).thenReturn(Set.of("topic/a", "topic/b"));
        when(webSocketObserver.observedChannels()).thenReturn(Set.of("ws-channel-1"));

        // == Act
        Set<String> result = facade.observedAllChannels();

        // == Assert
        assertEquals(Set.of("topic/a", "topic/b", "ws-channel-1"), result);
    }

    @Test
    public void testObservedAllChannels_deduplicatesOverlappingChannels() {
        // == Arrange
        when(mqttObserver.observedChannels()).thenReturn(Set.of("shared-channel", "topic/a"));
        when(webSocketObserver.observedChannels()).thenReturn(Set.of("shared-channel", "ws-channel-1"));

        // == Act
        Set<String> result = facade.observedAllChannels();

        // == Assert
        assertEquals(Set.of("shared-channel", "topic/a", "ws-channel-1"), result);
    }

    // == connectMqtt

    @Test
    public void testConnectMqtt_delegatesToMqttObserver() {
        // == Arrange
        ConnectMqttRequest request = Mockito.mock(ConnectMqttRequest.class);

        // == Act
        facade.connectMqtt(request);

        // == Assert
        verify(mqttObserver).connect(request);
    }

    // == subscribeMqtt

    @Test
    public void testSubscribeMqtt_delegatesToMqttObserver() {
        // == Arrange
        SubscribeMqttRequest request = Mockito.mock(SubscribeMqttRequest.class);

        // == Act
        facade.subscribeMqtt(request);

        // == Assert
        verify(mqttObserver).subscribe(request);
    }

    // == connectWebSocket

    @Test
    public void testConnectWebSocket_delegatesToWebSocketObserver() {
        // == Arrange
        ConnectWebSocketRequest request = Mockito.mock(ConnectWebSocketRequest.class);

        // == Act
        facade.connectWebSocket(request);

        // == Assert
        verify(webSocketObserver).connect(request);
    }

    // == subscribeWebSocket

    @Test
    public void testSubscribeWebSocket_delegatesToWebSocketObserver() {
        // == Arrange
        SubscribeWebSocketRequest request = Mockito.mock(SubscribeWebSocketRequest.class);

        // == Act
        facade.subscribeWebSocket(request);

        // == Assert
        verify(webSocketObserver).subscribe(request);
    }

    // == getDashboardSummary

    @Test
    public void testGetDashboardSummary_correctlySetsAllFields() {
        // == Arrange
        ObservedEvent event1 = observedEvent("topic/a", "trace-1");
        ObservedEvent event2 = observedEvent("topic/b", "trace-2");
        when(recentEventStore.getAllRecent()).thenReturn(List.of(event1, event2));
        when(mqttObserver.observedChannels()).thenReturn(Set.of("topic/a", "topic/b"));
        when(webSocketObserver.observedChannels()).thenReturn(Set.of("ws-channel-1"));

        // == Act
        DashboardSummary summary = facade.getDashboardSummary();

        // == Assert
        assertTrue(summary.mqttConnected());
        assertEquals(2, summary.observedTopicCount());
        assertEquals(1, summary.observedWebSocketChannelCount());
        assertEquals(2, summary.recentEventCount());
        assertEquals(List.of("topic/a", "topic/b"), summary.latestTopics());
        assertEquals(List.of("trace-1", "trace-2"), summary.latestTraceIds());
    }

    @Test
    public void testGetDashboardSummary_deduplicatesTopicsAndTraceIds() {
        // == Arrange
        ObservedEvent event1 = observedEvent("topic/a", "trace-1");
        ObservedEvent event2 = observedEvent("topic/a", "trace-1");
        when(recentEventStore.getAllRecent()).thenReturn(List.of(event1, event2));
        when(mqttObserver.observedChannels()).thenReturn(Set.of());
        when(webSocketObserver.observedChannels()).thenReturn(Set.of());

        // == Act
        DashboardSummary summary = facade.getDashboardSummary();

        // == Assert
        assertEquals(List.of("topic/a"), summary.latestTopics());
        assertEquals(List.of("trace-1"), summary.latestTraceIds());
    }

    @Test
    public void testGetDashboardSummary_limitsTopicsAndTraceIdsToTen() {
        // == Arrange
        List<ObservedEvent> events = new java.util.ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            events.add(observedEvent("topic/" + i, "trace-" + i));
        }
        when(recentEventStore.getAllRecent()).thenReturn(events);
        when(mqttObserver.observedChannels()).thenReturn(Set.of());
        when(webSocketObserver.observedChannels()).thenReturn(Set.of());

        // == Act
        DashboardSummary summary = facade.getDashboardSummary();

        // == Assert
        assertEquals(10, summary.latestTopics().size());
        assertEquals(10, summary.latestTraceIds().size());
    }

    @Test
    public void testGetDashboardSummary_filtersBlankChannelsAndTraceIds() {
        // == Arrange
        ObservedEvent eventWithBlankChannel = new ObservedEvent(
                "MQTT",
                "broker-1",
                "   ",
                Instant.parse("2024-01-01T00:00:00Z"),
                "payload",
                Map.of(),
                "   ",
                Instant.parse("2024-01-01T00:00:00Z")
        );
        when(recentEventStore.getAllRecent()).thenReturn(List.of(eventWithBlankChannel));
        when(mqttObserver.observedChannels()).thenReturn(Set.of());
        when(webSocketObserver.observedChannels()).thenReturn(Set.of());

        // == Act
        DashboardSummary summary = facade.getDashboardSummary();

        // == Assert
        assertEquals(List.of(), summary.latestTopics());
        assertEquals(List.of(), summary.latestTraceIds());
    }

    @Test
    public void testGetDashboardSummary_emptyStore() {
        // == Arrange
        when(recentEventStore.getAllRecent()).thenReturn(List.of());
        when(mqttObserver.observedChannels()).thenReturn(Set.of());
        when(webSocketObserver.observedChannels()).thenReturn(Set.of());

        // == Act
        DashboardSummary summary = facade.getDashboardSummary();

        // == Assert
        assertEquals(0, summary.recentEventCount());
        assertEquals(List.of(), summary.latestTopics());
        assertEquals(List.of(), summary.latestTraceIds());
    }

    // == Helpers

    private ObservedEvent observedEvent(String channel, String traceId) {
        return new ObservedEvent(
                "MQTT",
                "broker-1",
                channel,
                Instant.parse("2024-01-01T00:00:00Z"),
                "{\"key\":\"value\"}",
                Map.of("content-type", "application/json"),
                traceId,
                Instant.parse("2024-01-01T00:00:00Z")
        );
    }

    private ObservedEvent observedEventWithPayload(String channel, String traceId, String payload) {
        return new ObservedEvent(
                "MQTT",
                "broker-1",
                channel,
                Instant.parse("2024-01-01T00:00:00Z"),
                payload,
                Map.of("content-type", "application/json"),
                traceId,
                Instant.parse("2024-01-01T00:00:00Z")
        );
    }
}