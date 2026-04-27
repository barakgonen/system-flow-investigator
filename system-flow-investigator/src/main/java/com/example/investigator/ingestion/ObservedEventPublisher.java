package com.example.investigator.ingestion;

import com.example.investigator.domain.ObservedEvent;

public interface ObservedEventPublisher {
    void publish(ObservedEvent event);
}