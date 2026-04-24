package com.example.investigator.stream;

import com.example.investigator.domain.ObservedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

public class EventHubTests {

    private EventHub eventHub;

    @BeforeEach
    public void setUp() {
        eventHub = new EventHub();
    }

    @Test
    public void testStream_returnsNonNullFlux() {
        // == Act
        Flux<ObservedEvent> flux = eventHub.stream();

        // == Assert
        assertNotNull(flux);
    }

    @Test
    public void testPublish_eventIsReceivedOnStream() {
        // == Arrange
        ObservedEvent event = mock(ObservedEvent.class);

        // == Act & Assert
        StepVerifier.create(eventHub.stream())
                .then(() -> eventHub.publish(event))
                .expectNext(event)
                .thenCancel()
                .verify();
    }

    @Test
    public void testPublish_multipleEventsAreReceivedInOrder() {
        // == Arrange
        ObservedEvent event1 = mock(ObservedEvent.class);
        ObservedEvent event2 = mock(ObservedEvent.class);
        ObservedEvent event3 = mock(ObservedEvent.class);

        // == Act & Assert
        StepVerifier.create(eventHub.stream())
                .then(() -> {
                    eventHub.publish(event1);
                    eventHub.publish(event2);
                    eventHub.publish(event3);
                })
                .expectNext(event1, event2, event3)
                .thenCancel()
                .verify();
    }

    @Test
    public void testStream_multipleSubscribers_allReceiveEvent() throws InterruptedException {
        // == Arrange
        ObservedEvent event = mock(ObservedEvent.class);

        List<ObservedEvent> receivedBySubscriber2 = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        // Subscribe stream2 first so it's active before publish
        eventHub.stream().take(1).subscribe(e -> {
            receivedBySubscriber2.add(e);
            latch.countDown();
        });

        // == Act & Assert - stream1 via StepVerifier, publish happens inside .then()
        StepVerifier.create(eventHub.stream())
                .then(() -> eventHub.publish(event))
                .expectNext(event)
                .thenCancel()
                .verify();

        latch.await(1, TimeUnit.SECONDS);
        assertEquals(List.of(event), receivedBySubscriber2);
    }
}