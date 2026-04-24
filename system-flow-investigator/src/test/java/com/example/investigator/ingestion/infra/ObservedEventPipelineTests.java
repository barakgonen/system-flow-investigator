package com.example.investigator.ingestion.infra;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.storage.MessageFileSink;
import com.example.investigator.storage.RecentEventStore;
import com.example.investigator.stream.EventHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ObservedEventPipelineTests {

    @Mock private RecentEventStore recentEventStore;
    @Mock private EventHub eventHub;
    @Mock private MessageFileSink messageFileSink;

    private ObservedEventPipeline pipeline;

    @BeforeEach
    public void setUp() {
        pipeline = new ObservedEventPipeline(recentEventStore, eventHub, messageFileSink);
    }

    @Test
    public void testAccept_alwaysAddsToRecentEventStore() {
        // == Arrange
        ObservedEvent event = Mockito.mock(ObservedEvent.class);

        // == Act
        pipeline.accept(event, false);

        // == Assert
        verify(recentEventStore).add(event);
    }

    @Test
    public void testAccept_alwaysPublishesToEventHub() {
        // == Arrange
        ObservedEvent event = Mockito.mock(ObservedEvent.class);

        // == Act
        pipeline.accept(event, false);

        // == Assert
        verify(eventHub).publish(event);
    }

    @Test
    public void testAccept_persistToFileTrue_appendsToMessageFileSink() {
        // == Arrange
        ObservedEvent event = Mockito.mock(ObservedEvent.class);

        // == Act
        pipeline.accept(event, true);

        // == Assert
        verify(messageFileSink).append(event);
    }

    @Test
    public void testAccept_persistToFileFalse_doesNotAppendToMessageFileSink() {
        // == Arrange
        ObservedEvent event = Mockito.mock(ObservedEvent.class);

        // == Act
        pipeline.accept(event, false);

        // == Assert
        verify(messageFileSink, never()).append(event);
    }
}