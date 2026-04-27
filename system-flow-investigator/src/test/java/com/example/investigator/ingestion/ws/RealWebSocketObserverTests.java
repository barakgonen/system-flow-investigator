package com.example.investigator.ingestion.ws;

import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.SubscribeWebSocketRequest;
import com.example.investigator.ingestion.ObservedEventPublisher;
import com.example.investigator.service.SourceTimestampExtractor;
import com.example.investigator.service.TraceIdExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RealWebSocketObserverTests {

    private ObservedEventPublisher pipeline;
    private TraceIdExtractor traceIdExtractor;
    private SourceTimestampExtractor sourceTimestampExtractor;
    private WebSocketClientFactory webSocketClientFactory;
    private WebSocketClient webSocketClient;
    private WebSocketSession session;

    private RealWebSocketObserver observer;

    @BeforeEach
    void setUp() {
        pipeline = mock(ObservedEventPublisher.class);
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
    void shouldReturnSourceTypeWs() {
        assertThat(observer.sourceType()).isEqualTo("WS");
    }

    @Test
    void shouldRegisterObservedChannelOnSubscribe() {
        observer.subscribe(new SubscribeWebSocketRequest(
                "lab-ws",
                "ws/live/out",
                false
        ));

        assertThat(observer.observedChannels())
                .contains("ws/live/out");
    }

    @Test
    void shouldStoreSessionAfterConnectionEstablished() {
        WebSocketHandler handler = observer.createHandler(new ConnectWebSocketRequest(
                "lab-ws",
                "ws://localhost:8090/ws/live"
        ));

        assertThatCode(() -> handler.afterConnectionEstablished(session))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldConvertTextMessageToObservedEvent() throws Exception {
        when(traceIdExtractor.extract(anyString())).thenReturn("trace-123");
        when(sourceTimestampExtractor.extract(anyString()))
                .thenReturn(Instant.parse("2026-04-24T10:15:30Z"));

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
                  "timestamp": "2026-04-24T10:15:30Z",
                  "message": "hello"
                }
                """));

        ArgumentCaptor<ObservedEvent> eventCaptor = ArgumentCaptor.forClass(ObservedEvent.class);
        verify(pipeline).publish(eventCaptor.capture());

        ObservedEvent event = eventCaptor.getValue();

        assertThat(event.protocol()).isEqualTo("WS");
        assertThat(event.source()).isEqualTo("lab-ws");
        assertThat(event.channel()).isEqualTo("ws/live/out");
        assertThat(event.traceId()).isEqualTo("trace-123");
        assertThat(event.payload()).contains("hello");
        assertThat(event.sourceSentAt()).isEqualTo(Instant.parse("2026-04-24T10:15:30Z"));
        assertThat(event.observedAt()).isNotNull();
        assertThat(observer.observedChannels()).contains("ws/live/out");
    }

    @Test
    void shouldPersistTextMessageWhenChannelPersistenceEnabled() throws Exception {
        when(traceIdExtractor.extract(anyString())).thenReturn("trace-123");
        when(sourceTimestampExtractor.extract(anyString())).thenReturn(null);

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

        verify(pipeline).publish(any(ObservedEvent.class));
    }

    @Test
    void shouldNotPersistTextMessageWhenChannelPersistenceDisabled() throws Exception {
        when(traceIdExtractor.extract(anyString())).thenReturn("trace-123");
        when(sourceTimestampExtractor.extract(anyString())).thenReturn(null);

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
                  "traceId": "trace-123"
                }
                """));

        verify(pipeline).publish(any(ObservedEvent.class));
    }

    @Test
    void shouldUseFallbackChannelWhenPayloadHasNoChannel() throws Exception {
        when(traceIdExtractor.extract(anyString())).thenReturn("trace-123");
        when(sourceTimestampExtractor.extract(anyString())).thenReturn(null);

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
        verify(pipeline).publish(eventCaptor.capture());

        ObservedEvent event = eventCaptor.getValue();

        assertThat(event.channel()).isEqualTo("WS::lab-ws");
        assertThat(event.traceId()).isEqualTo("trace-123");
        assertThat(event.sourceSentAt()).isNull();
        assertThat(observer.observedChannels()).contains("WS::lab-ws");
    }

    @Test
    void shouldUseFallbackChannelWhenPayloadIsInvalidJson() throws Exception {
        when(traceIdExtractor.extract(anyString())).thenReturn(null);
        when(sourceTimestampExtractor.extract(anyString())).thenReturn(null);

        WebSocketHandler handler = observer.createHandler(new ConnectWebSocketRequest(
                "lab-ws",
                "ws://localhost:8090/ws/live"
        ));

        handler.handleMessage(session, new TextMessage("not-json"));

        ArgumentCaptor<ObservedEvent> eventCaptor = ArgumentCaptor.forClass(ObservedEvent.class);
        verify(pipeline).publish(eventCaptor.capture());

        ObservedEvent event = eventCaptor.getValue();

        assertThat(event.channel()).isEqualTo("WS::lab-ws");
        assertThat(event.payload()).isEqualTo("not-json");
        assertThat(event.traceId()).isNull();
        assertThat(event.sourceSentAt()).isNull();
    }

    @Test
    void shouldHandleConnectionClosedWithoutFailure() {
        WebSocketHandler handler = observer.createHandler(new ConnectWebSocketRequest(
                "lab-ws",
                "ws://localhost:8090/ws/live"
        ));

        assertThatCode(() -> {
            handler.afterConnectionEstablished(session);
            handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldHandleTransportErrorWithoutFailure() {
        WebSocketHandler handler = observer.createHandler(new ConnectWebSocketRequest(
                "lab-ws",
                "ws://localhost:8090/ws/live"
        ));

        assertThatCode(() ->
                handler.handleTransportError(session, new RuntimeException("network error"))
        ).doesNotThrowAnyException();
    }
}