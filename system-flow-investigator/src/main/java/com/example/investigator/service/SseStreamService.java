package com.example.investigator.service;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.stream.EventHub;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SseStreamService {

    private final EventHub eventHub;
    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newScheduledThreadPool(1);

    public SseStreamService(EventHub eventHub) {
        this.eventHub = eventHub;
    }

    public SseEmitter openStream(Set<String> channels,
                                 String textContains,
                                 String traceId) {

        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean closed = new AtomicBoolean(false);

        Disposable subscription = eventHub.stream()
                .filter(event -> channels == null || channels.isEmpty() || channels.contains(event.channel()))
                .filter(event -> textContains == null || textContains.isBlank() || safe(event.payload()).contains(textContains))
                .filter(event -> traceId == null || traceId.isBlank() || traceId.equals(event.traceId()))
                .subscribe(
                        event -> sendEvent(emitter, closed, event),
                        error -> closeEmitter(emitter, closed),
                        () -> closeEmitter(emitter, closed)
                );

        ScheduledFuture<?> heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(
                () -> sendHeartbeat(emitter, closed),
                10, 10, TimeUnit.SECONDS
        );

        Runnable cleanup = () -> {
            if (closed.compareAndSet(false, true)) {
                subscription.dispose();
                heartbeatTask.cancel(true);
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            cleanup.run();
            emitter.complete();
        });
        emitter.onError(error -> {
            cleanup.run();
            safeComplete(emitter);
        });

        return emitter;
    }

    private void sendEvent(SseEmitter emitter,
                           AtomicBoolean closed,
                           ObservedEvent event) {
        if (closed.get()) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(event));
        } catch (IOException | IllegalStateException e) {
            closeEmitter(emitter, closed);
        }
    }

    private void sendHeartbeat(SseEmitter emitter,
                               AtomicBoolean closed) {
        if (closed.get()) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data(Instant.now().toString()));
        } catch (IOException | IllegalStateException e) {
            closeEmitter(emitter, closed);
        }
    }

    private void closeEmitter(SseEmitter emitter, AtomicBoolean closed) {
        if (closed.compareAndSet(false, true)) {
            safeComplete(emitter);
        }
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}