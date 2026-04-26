package com.example.investigator.service;

import com.example.investigator.domain.CorrelatedEvent;
import com.example.investigator.domain.config.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FlowValidationServiceTests {

    private InvestigationConfigService configService;
    private FlowValidationService service;

    @BeforeEach
    void setUp() {
        configService = mock(InvestigationConfigService.class);
        service = new FlowValidationService(configService);
    }

    @Test
    void shouldReturnNullWhenFlowIdIsMissing() {
        FlowValidationResult result = service.validate(List.of(
                event("MQTT", "lab/flow/in", null)
        ), null);

        assertThat(result).isNull();
        verifyNoInteractions(configService);
    }

    @Test
    void shouldReturnCompleteWhenAllExpectedStepsFound() {
        when(configService.getConfig()).thenReturn(defaultConfig(true));

        FlowValidationResult result = service.validate(List.of(
                event("MQTT", "lab/flow/in", null),
                event("MQTT", "lab/flow/out", 5L),
                event("WS", "ws/live/out", 3L)
        ), "main-lab-flow");

        assertThat(result.status()).isEqualTo("COMPLETE");
        assertThat(result.missingChannels()).isEmpty();
        assertThat(result.extraChannels()).isEmpty();
        assertThat(result.lastCompletedChannel()).isEqualTo("ws/live/out");
        assertThat(result.steps()).extracting(ExpectedFlowStepResult::found)
                .containsExactly(true, true, true);
    }

    @Test
    void shouldReturnNoConfigWhenFlowIdDoesNotExist() {
        when(configService.getConfig()).thenReturn(defaultConfig(true));

        FlowValidationResult result = service.validate(List.of(
                event("MQTT", "lab/flow/in", null)
        ), "missing-flow");

        assertThat(result.status()).isEqualTo("NO_CONFIG");
        assertThat(result.message()).contains("missing-flow");
        assertThat(result.steps()).isEmpty();
        assertThat(result.extraChannels()).containsExactly("lab/flow/in");
    }

    @Test
    void shouldReturnBrokenWhenMiddleStepMissing() {
        when(configService.getConfig()).thenReturn(defaultConfig(true));

        FlowValidationResult result = service.validate(List.of(
                event("MQTT", "lab/flow/in", null),
                event("WS", "ws/live/out", 8L)
        ), "main-lab-flow");

        assertThat(result.status()).isEqualTo("BROKEN");
        assertThat(result.missingChannels()).containsExactly("lab/flow/out");
        assertThat(result.lastCompletedChannel()).isEqualTo("ws/live/out");
        assertThat(result.message()).contains("lab/flow/in", "lab/flow/out");
    }

    @Test
    void shouldReturnBrokenWhenFirstStepMissing() {
        when(configService.getConfig()).thenReturn(defaultConfig(true));

        FlowValidationResult result = service.validate(List.of(
                event("MQTT", "lab/flow/out", null)
        ), "main-lab-flow");

        assertThat(result.status()).isEqualTo("BROKEN");
        assertThat(result.missingChannels()).contains("lab/flow/in", "ws/live/out");
        assertThat(result.message()).contains("first expected step");
    }

    @Test
    void shouldDetectExtraChannels() {
        when(configService.getConfig()).thenReturn(defaultConfig(true));

        FlowValidationResult result = service.validate(List.of(
                event("MQTT", "lab/flow/in", null),
                event("MQTT", "lab/flow/out", 5L),
                event("WS", "ws/live/out", 3L),
                event("MQTT", "lab/flow/other", 1L)
        ), "main-lab-flow");

        assertThat(result.status()).isEqualTo("COMPLETE");
        assertThat(result.extraChannels()).containsExactly("lab/flow/other");
    }

    @Test
    void shouldReturnCompleteWithUnexpectedEventsWhenExtrasNotAllowed() {
        when(configService.getConfig()).thenReturn(defaultConfig(false));

        FlowValidationResult result = service.validate(List.of(
                event("MQTT", "lab/flow/in", null),
                event("MQTT", "lab/flow/out", 5L),
                event("WS", "ws/live/out", 3L),
                event("MQTT", "lab/flow/other", 1L)
        ), "main-lab-flow");

        assertThat(result.status()).isEqualTo("COMPLETE_WITH_UNEXPECTED_EVENTS");
        assertThat(result.extraChannels()).containsExactly("lab/flow/other");
    }

    @Test
    void shouldReturnNoConfigWhenSelectedFlowHasNoSteps() {
        InvestigationConfig config = new InvestigationConfig(
                "empty",
                "empty",
                List.of(
                        new FlowDefinition(
                                "empty-flow",
                                "Empty Flow",
                                "No steps",
                                List.of()
                        )
                ),
                new FlowValidationRules(50, true)
        );

        when(configService.getConfig()).thenReturn(config);

        FlowValidationResult result = service.validate(List.of(
                event("MQTT", "lab/flow/in", null)
        ), "empty-flow");

        assertThat(result.status()).isEqualTo("NO_CONFIG");
        assertThat(result.steps()).isEmpty();
        assertThat(result.extraChannels()).containsExactly("lab/flow/in");
    }

    @Test
    void shouldValidateSelectedFlowOnly() {
        InvestigationConfig config = new InvestigationConfig(
                "Multi Flow Config",
                "test",
                List.of(
                        new FlowDefinition(
                                "first-flow",
                                "First Flow",
                                "first",
                                List.of(
                                        new ExpectedFlowStep(1, "MQTT", "a/in", "A"),
                                        new ExpectedFlowStep(2, "WS", "a/out", "B")
                                )
                        ),
                        new FlowDefinition(
                                "second-flow",
                                "Second Flow",
                                "second",
                                List.of(
                                        new ExpectedFlowStep(1, "MQTT", "b/in", "B"),
                                        new ExpectedFlowStep(2, "MQTT", "b/out", "D")
                                )
                        )
                ),
                new FlowValidationRules(50, true)
        );

        when(configService.getConfig()).thenReturn(config);

        FlowValidationResult result = service.validate(List.of(
                event("MQTT", "b/in", null),
                event("MQTT", "b/out", 4L)
        ), "second-flow");

        assertThat(result.status()).isEqualTo("COMPLETE");
        assertThat(result.missingChannels()).isEmpty();
        assertThat(result.extraChannels()).isEmpty();
        assertThat(result.steps()).extracting(ExpectedFlowStepResult::channel)
                .containsExactly("b/in", "b/out");
    }

    private InvestigationConfig defaultConfig(boolean allowExtraEvents) {
        return new InvestigationConfig(
                "Main Investigation",
                "test",
                List.of(
                        new FlowDefinition(
                                "main-lab-flow",
                                "Main Lab Flow",
                                "Producer to websocket flow",
                                List.of(
                                        new ExpectedFlowStep(1, "MQTT", "lab/flow/in", "Producer"),
                                        new ExpectedFlowStep(2, "MQTT", "lab/flow/out", "Consumer"),
                                        new ExpectedFlowStep(3, "WS", "ws/live/out", "WS")
                                )
                        )
                ),
                new FlowValidationRules(50, allowExtraEvents)
        );
    }

    private CorrelatedEvent event(String protocol, String channel, Long deltaSource) {
        return new CorrelatedEvent(
                1,
                protocol,
                "source",
                channel,
                Instant.parse("2026-04-24T10:00:00Z"),
                Instant.parse("2026-04-24T10:00:01Z"),
                deltaSource,
                deltaSource,
                "{}",
                Map.of()
        );
    }
}