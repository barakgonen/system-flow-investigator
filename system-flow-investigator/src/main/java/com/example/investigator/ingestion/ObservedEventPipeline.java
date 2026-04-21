package com.example.investigator.ingestion;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.storage.MessageFileSink;
import com.example.investigator.storage.RecentEventStore;
import com.example.investigator.stream.EventHub;
import org.springframework.stereotype.Component;

@Component
public class ObservedEventPipeline {

    private final RecentEventStore recentEventStore;
    private final EventHub eventHub;
    private final MessageFileSink messageFileSink;

    public ObservedEventPipeline(RecentEventStore recentEventStore,
                                 EventHub eventHub,
                                 MessageFileSink messageFileSink) {
        this.recentEventStore = recentEventStore;
        this.eventHub = eventHub;
        this.messageFileSink = messageFileSink;
    }

    public void accept(ObservedEvent event, boolean persistToFile) {
        recentEventStore.add(event);
        eventHub.publish(event);

        if (persistToFile) {
            messageFileSink.append(event);
        }
    }
}