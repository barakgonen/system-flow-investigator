package com.example.investigator.ingestion.mqtt;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.SubscribeMqttRequest;
import com.example.investigator.ingestion.IngestionSourceRegistry;
import com.example.investigator.ingestion.IngestionSourceStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MqttIngestionSourceTests {

    @Test
    void shouldExposeMetadata() {
        MqttObserver observer = mock(MqttObserver.class);
        IngestionSourceRegistry registry = mock(IngestionSourceRegistry.class);

        MqttIngestionSource source = new MqttIngestionSource(observer, registry);

        assertThat(source.id()).isEqualTo("mqtt");
        assertThat(source.displayName()).isEqualTo("MQTT Broker");
        assertThat(source.protocol()).isEqualTo("MQTT");
    }

    @Test
    void shouldRegisterItself() {
        MqttObserver observer = mock(MqttObserver.class);
        IngestionSourceRegistry registry = mock(IngestionSourceRegistry.class);

        MqttIngestionSource source = new MqttIngestionSource(observer, registry);

        source.register();

        verify(registry).register(source);
    }

    @Test
    void shouldDelegateConnectAndSubscribe() {
        MqttObserver observer = mock(MqttObserver.class);
        IngestionSourceRegistry registry = mock(IngestionSourceRegistry.class);

        MqttIngestionSource source = new MqttIngestionSource(observer, registry);

        ConnectMqttRequest connectRequest = new ConnectMqttRequest("conn1", "localhost", 1883, "client-1", "user", "pass");
        SubscribeMqttRequest subscribeRequest = new SubscribeMqttRequest("lab/flow/#", "1", false);

        source.connect(connectRequest);
        source.subscribe(subscribeRequest);
        source.disconnect();

        verify(observer).connect(connectRequest);
        verify(observer).subscribe(subscribeRequest);
        verifyNoMoreInteractions(observer);
    }

    @Test
    void shouldReturnInactiveStatusWhenNoObservedTopics() {
        MqttObserver observer = mock(MqttObserver.class);
        IngestionSourceRegistry registry = mock(IngestionSourceRegistry.class);

        when(observer.observedChannels()).thenReturn(Set.of());

        MqttIngestionSource source = new MqttIngestionSource(observer, registry);

        IngestionSourceStatus status = source.status();

        assertThat(status.id()).isEqualTo("mqtt");
        assertThat(status.protocol()).isEqualTo("MQTT");
        assertThat(status.connected()).isFalse();
        assertThat(status.observedChannels()).isEmpty();
        assertThat(status.message()).isEqualTo("No observed topics yet");
    }

    @Test
    void shouldReturnActiveStatusWhenObservedTopicsExist() {
        MqttObserver observer = mock(MqttObserver.class);
        IngestionSourceRegistry registry = mock(IngestionSourceRegistry.class);

        when(observer.observedChannels()).thenReturn(Set.of("lab/flow/in"));

        MqttIngestionSource source = new MqttIngestionSource(observer, registry);

        IngestionSourceStatus status = source.status();

        assertThat(status.connected()).isTrue();
        assertThat(status.observedChannels()).containsExactly("lab/flow/in");
        assertThat(status.message()).isEqualTo("Active / observed topics");
    }
}