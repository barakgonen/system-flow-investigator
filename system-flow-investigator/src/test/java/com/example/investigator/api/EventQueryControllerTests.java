package com.example.investigator.api;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.service.InvestigatorFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class EventQueryControllerTests {

    @Mock
    private InvestigatorFacade facade;
    private EventQueryController eventQueryController;

    @BeforeEach
    public void setUp() {
        eventQueryController = new EventQueryController(facade);
    }

    @Test
    public void testRecentEndpoint_singleChannel_noFilters_callsSingleChannelOverload() {
        // == Arrange
        List<ObservedEvent> events = Mockito.mock(List.class);
        Mockito.when(facade.getRecentEvents("topic/a")).thenReturn(events);

        // == Act
        eventQueryController.recent(List.of("topic/a"), null, null);

        // == Assert
        Mockito.verify(facade).getRecentEvents("topic/a");
        Mockito.verify(facade, Mockito.never()).getRecentEvents(Mockito.anySet(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testRecentEndpoint_singleChannel_withTextContains_callsMultiParamOverload() {
        // == Arrange
        List<ObservedEvent> events = Mockito.mock(List.class);
        Mockito.when(facade.getRecentEvents(Set.of("topic/a"), "hello", null)).thenReturn(events);

        // == Act
        eventQueryController.recent(List.of("topic/a"), "hello", null);

        // == Assert
        Mockito.verify(facade).getRecentEvents(Set.of("topic/a"), "hello", null);
        Mockito.verify(facade, Mockito.never()).getRecentEvents(Mockito.anyString());
    }

    @Test
    public void testRecentEndpoint_singleChannel_withTraceId_callsMultiParamOverload() {
        // == Arrange
        List<ObservedEvent> events = Mockito.mock(List.class);
        Mockito.when(facade.getRecentEvents(Set.of("topic/a"), null, "trace-123")).thenReturn(events);

        // == Act
        eventQueryController.recent(List.of("topic/a"), null, "trace-123");

        // == Assert
        Mockito.verify(facade).getRecentEvents(Set.of("topic/a"), null, "trace-123");
        Mockito.verify(facade, Mockito.never()).getRecentEvents(Mockito.anyString());
    }

    @Test
    public void testRecentEndpoint_multipleChannels_callsMultiParamOverload() {
        // == Arrange
        List<ObservedEvent> events = Mockito.mock(List.class);
        Mockito.when(facade.getRecentEvents(Set.of("topic/a", "topic/b"), null, null)).thenReturn(events);

        // == Act
        eventQueryController.recent(List.of("topic/a", "topic/b"), null, null);

        // == Assert
        Mockito.verify(facade).getRecentEvents(Set.of("topic/a", "topic/b"), null, null);
        Mockito.verify(facade, Mockito.never()).getRecentEvents(Mockito.anyString());
    }

    @Test
    public void testRecentEndpoint_nullChannel_callsMultiParamOverloadWithEmptySet() {
        // == Arrange
        List<ObservedEvent> events = Mockito.mock(List.class);
        Mockito.when(facade.getRecentEvents(Set.of(), null, null)).thenReturn(events);

        // == Act
        eventQueryController.recent(null, null, null);

        // == Assert
        Mockito.verify(facade).getRecentEvents(Set.of(), null, null);
        Mockito.verify(facade, Mockito.never()).getRecentEvents(Mockito.anyString());
    }

    @Test
    public void testMqttTopicsEndpoint() {
        // == Arrange
        Set<String> topics = Mockito.mock(Set.class);
        Mockito.when(facade.observedMqttChannels()).thenReturn(topics);

        // == Act
        eventQueryController.mqttTopics();

        // == Assert
        Mockito.verify(facade).observedMqttChannels();
    }

    @Test
    public void testWsChannelsEndpoint() {
        // == Arrange
        Set<String> channels = Mockito.mock(Set.class);
        Mockito.when(facade.observedWebSocketChannels()).thenReturn(channels);

        // == Act
        eventQueryController.wsChannels();

        // == Assert
        Mockito.verify(facade).observedWebSocketChannels();
    }
}