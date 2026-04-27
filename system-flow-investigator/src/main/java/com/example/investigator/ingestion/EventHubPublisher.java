package com.example.investigator.ingestion;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.stream.EventHub;
import org.springframework.stereotype.Component;

@Component
public class EventHubPublisher implements ObservedEventPublisher {

    private final EventHub eventHub;

    public EventHubPublisher(EventHub eventHub) {
        this.eventHub = eventHub;
    }

    @Override
    public void publish(ObservedEvent event) {
        eventHub.publish(event);
    }
}