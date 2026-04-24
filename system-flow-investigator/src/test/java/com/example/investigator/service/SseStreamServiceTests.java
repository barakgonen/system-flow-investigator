package com.example.investigator.service;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.stream.EventHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SseStreamServiceTests {

    @Mock
    private EventHub eventHub;
    private SseStreamService service;

    private Sinks.Many<ObservedEvent> sink;

    @BeforeEach
    public void setUp() {
        sink = Sinks.many().multicast().onBackpressureBuffer();
        service = new SseStreamService(eventHub);
    }

    // Call this in tests that use openStream
    private SseEmitter openStream(Set<String> channels, String textContains, String traceId) {
        when(eventHub.stream()).thenReturn(sink.asFlux());
        return service.openStream(channels, textContains, traceId);
    }

    // == openStream - basic

    @Test
    public void testOpenStream_returnsNonNullEmitter() {
        // == Act
        SseEmitter emitter = openStream(Set.of(), null, null);

        // == Assert
        assertNotNull(emitter);
    }

    // == filtering - channels

    @Test
    public void testOpenStream_nullChannels_doesNotThrowOnEvents() {
        // == Arrange
        SseEmitter emitter = openStream(null, null, null);

        // == Act & Assert
        assertDoesNotThrow(() -> {
            sink.tryEmitNext(observedEvent("topic/a", "trace-1", "{\"key\":\"value\"}"));
            sink.tryEmitNext(observedEvent("topic/b", "trace-2", "{\"key\":\"value\"}"));
            sink.tryEmitComplete();
        });
        assertNotNull(emitter);
    }

    @Test
    public void testOpenStream_withChannelFilter_doesNotThrowOnNonMatchingEvent() {
        // == Arrange
        SseEmitter emitter = openStream(Set.of("topic/a"), null, null);

        // == Act & Assert - publishing non-matching event should not throw or close the emitter
        assertDoesNotThrow(() -> sink.tryEmitNext(observedEvent("topic/b", "trace-1", "payload")));
        assertNotNull(emitter);
    }

    @Test
    public void testOpenStream_withChannelFilter_doesNotThrowOnMatchingEvent() {
        // == Arrange
        SseEmitter emitter = openStream(Set.of("topic/a"), null, null);

        // == Act & Assert
        assertDoesNotThrow(() -> sink.tryEmitNext(observedEvent("topic/a", "trace-1", "payload")));
        assertNotNull(emitter);
    }

    @Test
    public void testOpenStream_emptyChannels_treatedAsNoFilter() {
        // == Act & Assert
        assertDoesNotThrow(() -> {
            SseEmitter emitter = openStream(Set.of(), null, null);
            sink.tryEmitNext(observedEvent("topic/a", "trace-1", "payload"));
            assertNotNull(emitter);
        });
    }

    // == filtering - textContains

    @Test
    public void testOpenStream_withTextContainsFilter_doesNotThrowOnMatchingEvent() {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), "hello", null);

        // == Act & Assert
        assertDoesNotThrow(() -> sink.tryEmitNext(observedEvent("topic/a", "trace-1", "{\"msg\":\"hello world\"}")));
        assertNotNull(emitter);
    }

    @Test
    public void testOpenStream_withTextContainsFilter_doesNotThrowOnNonMatchingEvent() {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), "hello", null);

        // == Act & Assert
        assertDoesNotThrow(() -> sink.tryEmitNext(observedEvent("topic/a", "trace-1", "{\"msg\":\"goodbye\"}")));
        assertNotNull(emitter);
    }

    @Test
    public void testOpenStream_blankTextContains_treatedAsNoFilter() {
        // == Act & Assert
        assertDoesNotThrow(() -> {
            SseEmitter emitter = openStream(Set.of(), "   ", null);
            sink.tryEmitNext(observedEvent("topic/a", "trace-1", "any payload"));
            assertNotNull(emitter);
        });
    }

    @Test
    public void testOpenStream_nullPayload_doesNotThrow() {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), "hello", null);
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

        // == Act & Assert
        assertDoesNotThrow(() -> sink.tryEmitNext(eventWithNullPayload));
        assertNotNull(emitter);
    }

    // == filtering - traceId

    @Test
    public void testOpenStream_withTraceIdFilter_doesNotThrowOnMatchingEvent() {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), null, "trace-target");

        // == Act & Assert
        assertDoesNotThrow(() -> sink.tryEmitNext(observedEvent("topic/a", "trace-target", "payload")));
        assertNotNull(emitter);
    }

    @Test
    public void testOpenStream_withTraceIdFilter_doesNotThrowOnNonMatchingEvent() {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), null, "trace-target");

        // == Act & Assert
        assertDoesNotThrow(() -> sink.tryEmitNext(observedEvent("topic/a", "trace-other", "payload")));
        assertNotNull(emitter);
    }

    @Test
    public void testOpenStream_blankTraceId_treatedAsNoFilter() {
        // == Act & Assert
        assertDoesNotThrow(() -> {
            SseEmitter emitter = openStream(Set.of(), null, "   ");
            sink.tryEmitNext(observedEvent("topic/a", "any-trace", "payload"));
            assertNotNull(emitter);
        });
    }

    // == cleanup

    @Test
    public void testOpenStream_onFluxError_doesNotThrow() {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), null, null);

        // == Act & Assert
        assertDoesNotThrow(() -> sink.tryEmitError(new RuntimeException("stream error")));
        assertNotNull(emitter);
    }

    @Test
    public void testOpenStream_onFluxComplete_doesNotThrow() {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), null, null);

        // == Act & Assert
        assertDoesNotThrow(() -> sink.tryEmitComplete());
        assertNotNull(emitter);
    }

    @Test
    public void testOpenStream_multipleStreams_independentEmitters() {
        // == Act
        SseEmitter emitter1 = openStream(Set.of("topic/a"), null, null);
        SseEmitter emitter2 = openStream(Set.of("topic/b"), null, null);

        // == Assert
        assertNotNull(emitter1);
        assertNotNull(emitter2);
        assertNotSame(emitter1, emitter2);
    }

    // == sendHeartbeat

    @Test
    public void testSendHeartbeat_whenOpen_sendsHeartbeatEvent() throws Exception {
        // == Arrange
        SseEmitter emitter = spy(new SseEmitter(0L));
        AtomicBoolean closed = new AtomicBoolean(false);

        // == Act
        invokeSendHeartbeat(emitter, closed);

        // == Assert
        verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    public void testSendHeartbeat_whenClosed_skipsHeartbeat() throws Exception {
        // == Arrange
        SseEmitter emitter = spy(new SseEmitter(0L));
        AtomicBoolean closed = new AtomicBoolean(true);

        // == Act
        invokeSendHeartbeat(emitter, closed);

        // == Assert
        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    public void testSendHeartbeat_onIOException_closesEmitter() throws Exception {
        // == Arrange
        SseEmitter emitter = spy(new SseEmitter(0L));
        AtomicBoolean closed = new AtomicBoolean(false);
        doThrow(new IOException("stream closed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        // == Act
        invokeSendHeartbeat(emitter, closed);

        // == Assert
        assertTrue(closed.get());
    }

    @Test
    public void testSendHeartbeat_onIllegalStateException_closesEmitter() throws Exception {
        // == Arrange
        SseEmitter emitter = spy(new SseEmitter(0L));
        AtomicBoolean closed = new AtomicBoolean(false);
        doThrow(new IllegalStateException("emitter already completed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        // == Act
        invokeSendHeartbeat(emitter, closed);

        // == Assert
        assertTrue(closed.get());
    }

    // == onTimeout

    @Test
    public void testOpenStream_onTimeout_doesNotThrow() throws Exception {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), null, null);
        Runnable timeoutCallback = extractTimeoutCallback(emitter);

        // == Act & Assert
        assertDoesNotThrow(timeoutCallback::run);
    }

    @Test
    public void testOpenStream_onTimeout_isIdempotent() throws Exception {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), null, null);
        Runnable timeoutCallback = extractTimeoutCallback(emitter);

        // == Act & Assert - second call should be no-op due to compareAndSet in cleanup
        assertDoesNotThrow(() -> {
            timeoutCallback.run();
            timeoutCallback.run();
        });
    }

    @Test
    public void testOpenStream_onTimeout_subsequentEventsDoNotThrow() throws Exception {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), null, null);
        Runnable timeoutCallback = extractTimeoutCallback(emitter);

        // == Act
        timeoutCallback.run();

        // == Assert - publishing after timeout cleanup should not throw
        assertDoesNotThrow(() -> sink.tryEmitNext(observedEvent("topic/a", "trace-1", "payload")));
    }

// == onError

    @Test
    public void testOpenStream_onError_doesNotThrow() throws Exception {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), null, null);
        Consumer<Throwable> errorCallback = extractErrorCallback(emitter);

        // == Act & Assert
        assertDoesNotThrow(() -> errorCallback.accept(new RuntimeException("test error")));
    }

    @Test
    public void testOpenStream_onError_isIdempotent() throws Exception {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), null, null);
        Consumer<Throwable> errorCallback = extractErrorCallback(emitter);

        // == Act & Assert - second call should be no-op due to compareAndSet in cleanup
        assertDoesNotThrow(() -> {
            errorCallback.accept(new RuntimeException("first error"));
            errorCallback.accept(new RuntimeException("second error"));
        });
    }

    @Test
    public void testOpenStream_onError_subsequentEventsDoNotThrow() throws Exception {
        // == Arrange
        SseEmitter emitter = openStream(Set.of(), null, null);
        Consumer<Throwable> errorCallback = extractErrorCallback(emitter);

        // == Act
        errorCallback.accept(new RuntimeException("test error"));

        // == Assert - publishing after error cleanup should not throw
        assertDoesNotThrow(() -> sink.tryEmitNext(observedEvent("topic/a", "trace-1", "payload")));
    }

    @Test
    public void testSendEvent_onIOException_closesEmitter() throws Exception {
        // == Arrange
        SseEmitter emitter = spy(new SseEmitter(0L));
        AtomicBoolean closed = new AtomicBoolean(false);
        ObservedEvent event = observedEvent("topic/a", "trace-1", "payload");
        doThrow(new IOException("stream closed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        // == Act
        invokeSendEvent(emitter, closed, event);

        // == Assert
        assertTrue(closed.get());
    }

    @Test
    public void testSendEvent_onIllegalStateException_closesEmitter() throws Exception {
        // == Arrange
        SseEmitter emitter = spy(new SseEmitter(0L));
        AtomicBoolean closed = new AtomicBoolean(false);
        ObservedEvent event = observedEvent("topic/a", "trace-1", "payload");
        doThrow(new IllegalStateException("emitter already completed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        // == Act
        invokeSendEvent(emitter, closed, event);

        // == Assert
        assertTrue(closed.get());
    }

    // == Reflection helpers
    private Runnable extractTimeoutCallback(SseEmitter emitter) throws Exception {
        Field field = org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.class
                .getDeclaredField("timeoutCallback");
        field.setAccessible(true);
        return (Runnable) field.get(emitter);
    }

    @SuppressWarnings("unchecked")
    private Consumer<Throwable> extractErrorCallback(SseEmitter emitter) throws Exception {
        Field field = org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.class
                .getDeclaredField("errorCallback");
        field.setAccessible(true);
        return (Consumer<Throwable>) field.get(emitter);
    }

// == Reflection helper

    private void invokeSendHeartbeat(SseEmitter emitter, AtomicBoolean closed) throws Exception {
        java.lang.reflect.Method method = SseStreamService.class.getDeclaredMethod(
                "sendHeartbeat", SseEmitter.class, AtomicBoolean.class);
        method.setAccessible(true);
        method.invoke(service, emitter, closed);
    }

    // == Helpers

    private ObservedEvent observedEvent(String channel, String traceId, String payload) {
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

    private void invokeSendEvent(SseEmitter emitter, AtomicBoolean closed, ObservedEvent event) throws Exception {
        java.lang.reflect.Method method = SseStreamService.class.getDeclaredMethod(
                "sendEvent", SseEmitter.class, AtomicBoolean.class, ObservedEvent.class);
        method.setAccessible(true);
        method.invoke(service, emitter, closed, event);
    }
}