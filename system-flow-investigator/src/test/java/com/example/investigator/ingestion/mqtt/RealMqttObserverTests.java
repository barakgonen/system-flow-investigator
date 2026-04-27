package com.example.investigator.ingestion.mqtt;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.SubscribeMqttRequest;
import com.example.investigator.ingestion.ObservedEventPublisher;
import com.example.investigator.service.SourceTimestampExtractor;
import com.example.investigator.service.TraceIdExtractor;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RealMqttObserverTests {

    private ObservedEventPublisher pipeline;
    private TraceIdExtractor traceIdExtractor;
    private SourceTimestampExtractor sourceTimestampExtractor;
    private MqttClientFactory mqttClientFactory;
    private MqttClient mqttClient;

    private RealMqttObserver observer;

    @BeforeEach
    void setUp() throws Exception {
        pipeline = mock(ObservedEventPublisher.class);
        traceIdExtractor = mock(TraceIdExtractor.class);
        sourceTimestampExtractor = mock(SourceTimestampExtractor.class);
        mqttClientFactory = mock(MqttClientFactory.class);
        mqttClient = mock(MqttClient.class);

        when(mqttClientFactory.create(anyString(), anyString()))
                .thenReturn(mqttClient);

        when(mqttClient.isConnected()).thenReturn(false);

        observer = new RealMqttObserver(
                pipeline,
                traceIdExtractor,
                sourceTimestampExtractor,
                mqttClientFactory,
                "localhost",
                1883,
                "test-client",
                "lab/flow/#",
                false
        );
    }

    @Test
    void shouldReturnSourceTypeMqtt() {
        assertThat(observer.sourceType()).isEqualTo("MQTT");
    }

    @Test
    void shouldConnectToMqttBrokerUsingRequestValues() throws Exception {
        ConnectMqttRequest request = new ConnectMqttRequest(
                "default",
                "emqx",
                1883,
                "investigator-test",
                null,
                null
        );

        observer.connect(request);

        verify(mqttClientFactory).create(eq("tcp://emqx:1883"), startsWith("investigator-test-"));
        verify(mqttClient).setCallback(any(MqttCallback.class));
        verify(mqttClient).connect(any(MqttConnectOptions.class));
    }

    @Test
    void shouldUseUsernameAndPasswordWhenProvided() throws Exception {
        ConnectMqttRequest request = new ConnectMqttRequest(
                "default",
                "emqx",
                1883,
                "investigator-test",
                "user",
                "secret"
        );

        observer.connect(request);

        ArgumentCaptor<MqttConnectOptions> captor = ArgumentCaptor.forClass(MqttConnectOptions.class);
        verify(mqttClient).connect(captor.capture());

        MqttConnectOptions options = captor.getValue();

        assertThat(options.getUserName()).isEqualTo("user");
        assertThat(options.getPassword()).containsExactly('s', 'e', 'c', 'r', 'e', 't');
        assertThat(options.isAutomaticReconnect()).isTrue();
        assertThat(options.isCleanSession()).isTrue();
    }

    @Test
    void shouldNotReconnectWhenClientAlreadyConnected() throws Exception {
        ConnectMqttRequest request = new ConnectMqttRequest(
                "default",
                "emqx",
                1883,
                "investigator-test",
                null,
                null
        );

        when(mqttClient.isConnected()).thenReturn(true);

        observer.connect(request);
        observer.connect(request);

        verify(mqttClientFactory, times(1)).create(anyString(), anyString());
        verify(mqttClient, times(1)).connect(any(MqttConnectOptions.class));
    }

    @Test
    void shouldSubscribeToTopicAndTrackObservedChannel() throws Exception {
        observer.subscribe(new SubscribeMqttRequest(
                "default",
                "lab/flow/#",
                false
        ));

        verify(mqttClient).subscribe("lab/flow/#", 1);

        assertThat(observer.observedChannels())
                .contains("lab/flow/#");
    }

    @Test
    void shouldConvertIncomingMqttMessageToObservedEvent() throws Exception {
        Instant sourceSentAt = Instant.parse("2026-04-24T10:15:30Z");

        when(traceIdExtractor.extract(anyString())).thenReturn("trace-123");
        when(sourceTimestampExtractor.extract(anyString())).thenReturn(sourceSentAt);

        observer.connect(new ConnectMqttRequest(
                "default",
                "emqx",
                1883,
                "investigator-test",
                null,
                null
        ));

        ArgumentCaptor<MqttCallback> callbackCaptor = ArgumentCaptor.forClass(MqttCallback.class);
        verify(mqttClient).setCallback(callbackCaptor.capture());

        MqttMessage message = new MqttMessage("""
                {
                  "traceId": "trace-123",
                  "timestamp": "2026-04-24T10:15:30Z",
                  "message": "hello"
                }
                """.getBytes());

        message.setQos(1);
        message.setRetained(false);

        callbackCaptor.getValue().messageArrived("lab/flow/in", message);

        ArgumentCaptor<ObservedEvent> eventCaptor = ArgumentCaptor.forClass(ObservedEvent.class);
        verify(pipeline).publish(eventCaptor.capture());

        ObservedEvent event = eventCaptor.getValue();

        assertThat(event.protocol()).isEqualTo("MQTT");
        assertThat(event.source()).isEqualTo("emqx:1883");
        assertThat(event.channel()).isEqualTo("lab/flow/in");
        assertThat(event.payload()).contains("trace-123");
        assertThat(event.traceId()).isEqualTo("trace-123");
        assertThat(event.sourceSentAt()).isEqualTo(sourceSentAt);
        assertThat(event.observedAt()).isNotNull();
        assertThat(event.metadata())
                .containsEntry("qos", "1")
                .containsEntry("retained", "false");

        assertThat(observer.observedChannels()).contains("lab/flow/in");
    }

    @Test
    void shouldPersistIncomingMessageWhenTopicWasSubscribedWithPersistenceEnabled() throws Exception {
        when(traceIdExtractor.extract(anyString())).thenReturn("trace-123");
        when(sourceTimestampExtractor.extract(anyString())).thenReturn(null);

        observer.subscribe(new SubscribeMqttRequest(
                "default",
                "lab/flow/in",
                true
        ));

        ArgumentCaptor<MqttCallback> callbackCaptor = ArgumentCaptor.forClass(MqttCallback.class);
        verify(mqttClient).setCallback(callbackCaptor.capture());

        MqttMessage message = new MqttMessage("""
                {
                  "traceId": "trace-123",
                  "message": "hello"
                }
                """.getBytes());

        callbackCaptor.getValue().messageArrived("lab/flow/in", message);

        verify(pipeline).publish(any(ObservedEvent.class));
    }

    @Test
    void shouldNotPersistIncomingMessageWhenTopicWasSubscribedWithPersistenceDisabled() throws Exception {
        when(traceIdExtractor.extract(anyString())).thenReturn("trace-123");
        when(sourceTimestampExtractor.extract(anyString())).thenReturn(null);

        observer.subscribe(new SubscribeMqttRequest(
                "default",
                "lab/flow/in",
                false
        ));

        ArgumentCaptor<MqttCallback> callbackCaptor = ArgumentCaptor.forClass(MqttCallback.class);
        verify(mqttClient).setCallback(callbackCaptor.capture());

        MqttMessage message = new MqttMessage("""
                {
                  "traceId": "trace-123",
                  "message": "hello"
                }
                """.getBytes());

        callbackCaptor.getValue().messageArrived("lab/flow/in", message);

        verify(pipeline).publish(any(ObservedEvent.class));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenConnectFails() throws Exception {
        when(mqttClientFactory.create(anyString(), anyString()))
                .thenThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION));

        assertThatThrownBy(() ->
                observer.connect(new ConnectMqttRequest(
                        "default",
                        "bad-host",
                        1883,
                        "client",
                        null,
                        null
                ))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to connect MQTT");
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenSubscribeFails() throws Exception {
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION))
                .when(mqttClient)
                .subscribe("bad/topic", 1);

        assertThatThrownBy(() ->
                observer.subscribe(new SubscribeMqttRequest(
                        "default",
                        "bad/topic",
                        false
                ))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed subscribing MQTT");
    }

    @Test
    void shouldDisconnectAndCloseClientOnStopWhenConnected() throws Exception {
        when(mqttClient.isConnected()).thenReturn(true);

        observer.connect(new ConnectMqttRequest(
                "default",
                "emqx",
                1883,
                "investigator-test",
                null,
                null
        ));

        observer.stop();

        verify(mqttClient).disconnect();
        verify(mqttClient).close();
    }

    @Test
    void shouldOnlyCloseClientOnStopWhenNotConnected() throws Exception {
        when(mqttClient.isConnected()).thenReturn(false);

        observer.connect(new ConnectMqttRequest(
                "default",
                "emqx",
                1883,
                "investigator-test",
                null,
                null
        ));

        observer.stop();

        verify(mqttClient, never()).disconnect();
        verify(mqttClient).close();
    }

    @Test
    void shouldIgnoreExceptionsDuringStop() throws Exception {
        when(mqttClient.isConnected()).thenReturn(true);

        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION))
                .when(mqttClient)
                .disconnect();

        observer.connect(new ConnectMqttRequest(
                "default",
                "emqx",
                1883,
                "investigator-test",
                null,
                null
        ));

        assertThatCode(() -> observer.stop())
                .doesNotThrowAnyException();
    }
}