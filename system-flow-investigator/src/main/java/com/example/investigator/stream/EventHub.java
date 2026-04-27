package com.example.investigator.stream;

import com.example.investigator.domain.ObservedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
public class EventHub {

    private final Sinks.Many<ObservedEvent> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public Flux<ObservedEvent> events() {
        return sink.asFlux();
    }

    public void publish(ObservedEvent event) {
        log.info("Publishing SSE event protocol={}, traceId={}", event.protocol(), event.traceId());

        Sinks.EmitResult result;

        synchronized (this) {
            result = sink.tryEmitNext(event);
        }

        if (result.isFailure()) {
            log.error(
                    "Failed to publish SSE event protocol={}, traceId={}, result={}",
                    event.protocol(),
                    event.traceId(),
                    result
            );
        }
    }
}