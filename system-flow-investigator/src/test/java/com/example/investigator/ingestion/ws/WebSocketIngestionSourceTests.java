package com.example.investigator.ingestion.ws;

import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.SubscribeWebSocketRequest;
import com.example.investigator.ingestion.IngestionSourceRegistry;
import com.example.investigator.ingestion.IngestionSourceStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WebSocketIngestionSourceTests {

    @Test
    void shouldExposeMetadata() {
        WebSocketObserver observer = mock(WebSocketObserver.class);
        IngestionSourceRegistry registry = mock(IngestionSourceRegistry.class);

        WebSocketIngestionSource source = new WebSocketIngestionSource(observer, registry);

        assertThat(source.id()).isEqualTo("websocket");
        assertThat(source.displayName()).isEqualTo("WebSocket");
        assertThat(source.protocol()).isEqualTo("WS");
    }

    @Test
    void shouldRegisterItself() {
        WebSocketObserver observer = mock(WebSocketObserver.class);
        IngestionSourceRegistry registry = mock(IngestionSourceRegistry.class);

        WebSocketIngestionSource source = new WebSocketIngestionSource(observer, registry);

        source.register();

        verify(registry).register(source);
    }

    @Test
    void shouldDelegateConnectAndSubscribe() {
        WebSocketObserver observer = mock(WebSocketObserver.class);
        IngestionSourceRegistry registry = mock(IngestionSourceRegistry.class);

        WebSocketIngestionSource source = new WebSocketIngestionSource(observer, registry);

        ConnectWebSocketRequest connectRequest = new ConnectWebSocketRequest("conn1", "ws://localhost:8081/live");
        SubscribeWebSocketRequest subscribeRequest = new SubscribeWebSocketRequest("conn2", "ws/live/out", false);

        source.connect(connectRequest);
        source.subscribe(subscribeRequest);
        source.disconnect();

        verify(observer).connect(connectRequest);
        verify(observer).subscribe(subscribeRequest);
        verifyNoMoreInteractions(observer);
    }

    @Test
    void shouldReturnInactiveStatusWhenNoObservedChannels() {
        WebSocketObserver observer = mock(WebSocketObserver.class);
        IngestionSourceRegistry registry = mock(IngestionSourceRegistry.class);

        when(observer.observedChannels()).thenReturn(Set.of());

        WebSocketIngestionSource source = new WebSocketIngestionSource(observer, registry);

        IngestionSourceStatus status = source.status();

        assertThat(status.id()).isEqualTo("websocket");
        assertThat(status.protocol()).isEqualTo("WS");
        assertThat(status.connected()).isFalse();
        assertThat(status.observedChannels()).isEmpty();
        assertThat(status.message()).isEqualTo("No observed channels yet");
    }

    @Test
    void shouldReturnActiveStatusWhenObservedChannelsExist() {
        WebSocketObserver observer = mock(WebSocketObserver.class);
        IngestionSourceRegistry registry = mock(IngestionSourceRegistry.class);

        when(observer.observedChannels()).thenReturn(Set.of("ws/live/out"));

        WebSocketIngestionSource source = new WebSocketIngestionSource(observer, registry);

        IngestionSourceStatus status = source.status();

        assertThat(status.connected()).isTrue();
        assertThat(status.observedChannels()).containsExactly("ws/live/out");
        assertThat(status.message()).isEqualTo("Active / observed channels");
    }
}