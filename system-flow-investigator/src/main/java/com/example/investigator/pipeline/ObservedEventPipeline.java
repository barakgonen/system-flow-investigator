package com.example.investigator.pipeline;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.ingestion.ObservedEventPublisher;
import com.example.investigator.storage.RecentEventStore;
import com.example.investigator.stream.EventHub;
import org.springframework.stereotype.Component;

@Component
public class ObservedEventPipeline implements ObservedEventPublisher {

    private final RecentEventStore recentEventStore;
    private final EventHub eventHub;

    public ObservedEventPipeline(RecentEventStore recentEventStore,
                                 EventHub eventHub) {
        this.recentEventStore = recentEventStore;
        this.eventHub = eventHub;
    }

    @Override
    public void publish(ObservedEvent event) {
        recentEventStore.add(event);
        eventHub.publish(event);
    }
}