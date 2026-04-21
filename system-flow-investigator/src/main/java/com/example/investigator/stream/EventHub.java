package com.example.investigator.stream;

import com.example.investigator.domain.ObservedEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class EventHub {

    private final Sinks.Many<ObservedEvent> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void publish(ObservedEvent event) {
        sink.tryEmitNext(event);
    }

    public Flux<ObservedEvent> stream() {
        return sink.asFlux();
    }
}