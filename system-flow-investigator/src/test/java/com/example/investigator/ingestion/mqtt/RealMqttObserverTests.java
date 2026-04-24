package com.example.investigator.ingestion.mqtt;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.SubscribeMqttRequest;
import com.example.investigator.ingestion.infra.ObservedEventPipeline;
import com.example.investigator.service.TraceIdExtractor;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealMqttObserverTests {

    private ObservedEventPipeline pipeline;
    private TraceIdExtractor traceIdExtractor;
    private MqttClientFactory mqttClientFactory;
    private MqttClient mqttClient;

    private RealMqttObserver observer;

    @BeforeEach
    void setUp() throws Exception {
        pipeline = mock(ObservedEventPipeline.class);
        traceIdExtractor = mock(TraceIdExtractor.class);
        mqttClientFactory = mock(MqttClientFactory.class);
        mqttClient = mock(MqttClient.class);

        when(mqttClientFactory.create(anyString(), anyString())).thenReturn(mqttClient);

        observer = new RealMqttObserver(
                pipeline,
                traceIdExtractor,
                mqttClientFactory,
                "localhost",
                1883,
                "test-client",
                "lab/flow/#",
                false
        );
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
        // == Arrange
        ConnectMqttRequest request = new ConnectMqttRequest(
                "default",
                "emqx",
                1883,
                "investigator-test",
                "user",
                "secret"
        );

        // Act
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

        observer.connect(request); // creates client
        observer.connect(request); // should return early

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
        when(traceIdExtractor.extract(anyString())).thenReturn("trace-123");

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

        MqttCallback callback = callbackCaptor.getValue();

        MqttMessage message = new MqttMessage("""
                {"traceId":"trace-123","timestamp":123456,"message":"hello"}
                """.getBytes());
        message.setQos(1);
        message.setRetained(false);

        callback.messageArrived("lab/flow/in", message);

        ArgumentCaptor<ObservedEvent> eventCaptor = ArgumentCaptor.forClass(ObservedEvent.class);
        verify(pipeline).accept(eventCaptor.capture(), eq(false));

        ObservedEvent event = eventCaptor.getValue();

        assertThat(event.protocol()).isEqualTo("MQTT");
        assertThat(event.channel()).isEqualTo("lab/flow/in");
        assertThat(event.payload()).contains("trace-123");
        assertThat(event.traceId()).isEqualTo("trace-123");

        assertThat(observer.observedChannels()).contains("lab/flow/in");
    }

    @Test
    void shouldPersistIncomingMessageWhenTopicWasSubscribedWithPersistenceEnabled() throws Exception {
        when(traceIdExtractor.extract(anyString())).thenReturn("trace-123");

        observer.subscribe(new SubscribeMqttRequest(
                "default",
                "lab/flow/in",
                true
        ));

        ArgumentCaptor<MqttCallback> callbackCaptor = ArgumentCaptor.forClass(MqttCallback.class);
        verify(mqttClient).setCallback(callbackCaptor.capture());

        MqttMessage message = new MqttMessage("""
                {"traceId":"trace-123","message":"hello"}
                """.getBytes());

        callbackCaptor.getValue().messageArrived("lab/flow/in", message);

        verify(pipeline).accept(any(ObservedEvent.class), eq(true));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenConnectFails() throws Exception {
        when(mqttClientFactory.create(anyString(), anyString()))
                .thenThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
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

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
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
        observer.stop();

        verify(mqttClient).disconnect();
        verify(mqttClient).close();
    }

    @Test
    void shouldOnlyCloseClientOnStopWhenNotConnected() throws Exception {
        ConnectMqttRequest request = new ConnectMqttRequest(
                "default",
                "emqx",
                1883,
                "investigator-test",
                null,
                null
        );

        when(mqttClient.isConnected()).thenReturn(false);

        observer.connect(request);
        observer.stop();

        verify(mqttClient, never()).disconnect();
        verify(mqttClient).close();
    }

    @Test
    void shouldIgnoreExceptionsDuringStop() throws Exception {
        ConnectMqttRequest request = new ConnectMqttRequest(
                "default",
                "emqx",
                1883,
                "investigator-test",
                null,
                null
        );

        when(mqttClient.isConnected()).thenReturn(true);
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION))
                .when(mqttClient)
                .disconnect();

        observer.connect(request);

        assertThatCode(() -> observer.stop())
                .doesNotThrowAnyException();
    }
}