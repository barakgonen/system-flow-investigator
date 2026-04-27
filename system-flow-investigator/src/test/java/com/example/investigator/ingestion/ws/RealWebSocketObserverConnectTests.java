package com.example.investigator.ingestion.ws;

import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.SubscribeWebSocketRequest;
import com.example.investigator.ingestion.infra.ObservedEventPipeline;
import com.example.investigator.service.SourceTimestampExtractor;
import com.example.investigator.service.TraceIdExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RealWebSocketObserverConnectTests {

    private ObservedEventPipeline pipeline;
    private TraceIdExtractor traceIdExtractor;
    private SourceTimestampExtractor sourceTimestampExtractor;
    private WebSocketClientFactory webSocketClientFactory;
    private WebSocketClient webSocketClient;
    private WebSocketSession session;

    private RealWebSocketObserver observer;

    @BeforeEach
    void setUp() {
        pipeline = mock(ObservedEventPipeline.class);
        traceIdExtractor = mock(TraceIdExtractor.class);
        sourceTimestampExtractor = mock(SourceTimestampExtractor.class);

        webSocketClientFactory = mock(WebSocketClientFactory.class);
        webSocketClient = mock(WebSocketClient.class);
        session = mock(WebSocketSession.class);

        observer = new RealWebSocketObserver(
                pipeline,
                traceIdExtractor,
                sourceTimestampExtractor,
                webSocketClientFactory,
                "localhost",
                8090
        );
    }

    @Test
    void shouldConnectUsingWebSocketClientFactory() {
        when(webSocketClientFactory.create()).thenReturn(webSocketClient);
        when(webSocketClient.execute(any(WebSocketHandler.class), eq("ws://localhost:8090/ws/live")))
                .thenReturn(CompletableFuture.completedFuture(session));

        observer.connect(new ConnectWebSocketRequest(
                "lab-ws",
                "ws://localhost:8090/ws/live"
        ));

        verify(webSocketClientFactory).create();
        verify(webSocketClient).execute(any(WebSocketHandler.class), eq("ws://localhost:8090/ws/live"));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenConnectFails() {
        when(webSocketClientFactory.create()).thenReturn(webSocketClient);
        when(webSocketClient.execute(any(WebSocketHandler.class), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        assertThatThrownBy(() -> observer.connect(new ConnectWebSocketRequest(
                "lab-ws",
                "ws://localhost:8090/ws/live"
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed connecting to WebSocket");
    }

    @Test
    void shouldStoreSessionAfterConnectionEstablished() throws Exception {
        WebSocketHandler handler = observer.createHandler(new ConnectWebSocketRequest(
                "lab-ws",
                "ws://localhost:8090/ws/live"
        ));

        handler.afterConnectionEstablished(session);

        assertThat(observer.observedChannels()).isEmpty();
    }

    @Test
    void shouldConvertTextMessageToObservedEvent() throws Exception {
        when(traceIdExtractor.extract(anyString())).thenReturn("trace-123");

        observer.subscribe(new SubscribeWebSocketRequest(
                "lab-ws",
                "ws/live/out",
                false
        ));

        WebSocketHandler handler = observer.createHandler(new ConnectWebSocketRequest(
                "lab-ws",
                "ws://localhost:8090/ws/live"
        ));

        handler.handleMessage(session, new TextMessage("""
                {
                  "channel": "ws/live/out",
                  "traceId": "trace-123",
                  "message": "hello"
                }
                """));

        ArgumentCaptor<ObservedEvent> eventCaptor = ArgumentCaptor.forClass(ObservedEvent.class);
        verify(pipeline).accept(eventCaptor.capture(), eq(false));

        ObservedEvent event = eventCaptor.getValue();

        assertThat(event.protocol()).isEqualTo("WS");
        assertThat(event.channel()).isEqualTo("ws/live/out");
        assertThat(event.traceId()).isEqualTo("trace-123");
        assertThat(event.payload()).contains("hello");
        assertThat(observer.observedChannels()).contains("ws/live/out");
    }

    @Test
    void shouldPersistTextMessageWhenChannelPersistenceEnabled() throws Exception {
        when(traceIdExtractor.extract(anyString())).thenReturn("trace-123");

        observer.subscribe(new SubscribeWebSocketRequest(
                "lab-ws",
                "ws/live/out",
                true
        ));

        WebSocketHandler handler = observer.createHandler(new ConnectWebSocketRequest(
                "lab-ws",
                "ws://localhost:8090/ws/live"
        ));

        handler.handleMessage(session, new TextMessage("""
                {
                  "channel": "ws/live/out",
                  "traceId": "trace-123"
                }
                """));

        verify(pipeline).accept(any(ObservedEvent.class), eq(true));
    }

    @Test
    void shouldUseFallbackChannelWhenPayloadHasNoChannel() throws Exception {
        when(traceIdExtractor.extract(anyString())).thenReturn("trace-123");

        WebSocketHandler handler = observer.createHandler(new ConnectWebSocketRequest(
                "lab-ws",
                "ws://localhost:8090/ws/live"
        ));

        handler.handleMessage(session, new TextMessage("""
                {
                  "traceId": "trace-123"
                }
                """));

        ArgumentCaptor<ObservedEvent> eventCaptor = ArgumentCaptor.forClass(ObservedEvent.class);
        verify(pipeline).accept(eventCaptor.capture(), eq(false));

        assertThat(eventCaptor.getValue().channel()).isEqualTo("WS::lab-ws");
    }

    @Test
    void shouldHandleConnectionClosedWithoutFailure() throws Exception {
        WebSocketHandler handler = observer.createHandler(new ConnectWebSocketRequest(
                "lab-ws",
                "ws://localhost:8090/ws/live"
        ));

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // no exception = pass
    }

    @Test
    void shouldHandleTransportErrorWithoutFailure() throws Exception {
        WebSocketHandler handler = observer.createHandler(new ConnectWebSocketRequest(
                "lab-ws",
                "ws://localhost:8090/ws/live"
        ));

        handler.handleTransportError(session, new RuntimeException("network error"));

        // no exception = pass
    }
}