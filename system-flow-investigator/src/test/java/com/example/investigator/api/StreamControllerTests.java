package com.example.investigator.api;

import com.example.investigator.service.SseStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class StreamControllerTests {

    @Mock
    private SseStreamService sseStreamService;
    private StreamController streamController;

    @BeforeEach
    public void setUp() {
        streamController = new StreamController(sseStreamService);
    }

    @Test
    public void testStreamEventsEndpoint_withChannels() {
        // == Arrange
        SseEmitter emitter = Mockito.mock(SseEmitter.class);
        Mockito.when(sseStreamService.openStream(Set.of("topic/a", "topic/b"), "hello", "trace-123")).thenReturn(emitter);

        // == Act
        streamController.streamEvents(List.of("topic/a", "topic/b"), "hello", "trace-123");

        // == Assert
        Mockito.verify(sseStreamService).openStream(Set.of("topic/a", "topic/b"), "hello", "trace-123");
    }

    @Test
    public void testStreamEventsEndpoint_nullChannel_passesEmptySet() {
        // == Arrange
        SseEmitter emitter = Mockito.mock(SseEmitter.class);
        Mockito.when(sseStreamService.openStream(Set.of(), null, null)).thenReturn(emitter);

        // == Act
        streamController.streamEvents(null, null, null);

        // == Assert
        Mockito.verify(sseStreamService).openStream(Set.of(), null, null);
    }
}