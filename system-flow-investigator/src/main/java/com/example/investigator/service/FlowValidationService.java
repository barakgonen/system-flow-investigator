package com.example.investigator.service;

import com.example.investigator.domain.CorrelatedEvent;
import com.example.investigator.domain.config.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FlowValidationService {

    private final InvestigationConfigService configService;

    public FlowValidationService(InvestigationConfigService configService) {
        this.configService = configService;
    }

    public FlowValidationResult validate(List<CorrelatedEvent> events, String flowId) {
        if (flowId == null || flowId.isBlank()) {
            return null;
        }

        InvestigationConfig config = configService.getConfig();

        FlowDefinition flow = findFlow(config, flowId);

        if (flow == null) {
            return new FlowValidationResult(
                    "NO_CONFIG",
                    "Flow configuration was not found: " + flowId,
                    List.of(),
                    List.of(),
                    extractExtraChannels(events, List.of()),
                    null
            );
        }

        if (flow.steps() == null || flow.steps().isEmpty()) {
            return new FlowValidationResult(
                    "NO_CONFIG",
                    "Flow has no expected steps configured: " + flowId,
                    List.of(),
                    List.of(),
                    extractExtraChannels(events, List.of()),
                    null
            );
        }

        List<ExpectedFlowStep> expectedSteps = flow.steps().stream()
                .sorted(Comparator.comparingInt(ExpectedFlowStep::index))
                .toList();

        List<ExpectedFlowStepResult> stepResults = new ArrayList<>();
        List<String> missingChannels = new ArrayList<>();

        String lastCompletedChannel = null;
        int firstMissingIndex = -1;

        for (int i = 0; i < expectedSteps.size(); i++) {
            ExpectedFlowStep step = expectedSteps.get(i);

            CorrelatedEvent matchingEvent = events.stream()
                    .filter(event -> matches(step, event))
                    .findFirst()
                    .orElse(null);

            boolean found = matchingEvent != null;

            if (found) {
                lastCompletedChannel = step.channel();
            } else {
                missingChannels.add(step.channel());

                if (firstMissingIndex == -1) {
                    firstMissingIndex = i;
                }
            }

            stepResults.add(new ExpectedFlowStepResult(
                    step.index(),
                    step.protocol(),
                    step.channel(),
                    step.label(),
                    found,
                    matchingEvent == null ? null : matchingEvent.deltaFromPreviousSourceMs(),
                    matchingEvent == null ? null : matchingEvent.deltaFromPreviousObservedMs()
            ));
        }

        List<String> extraChannels = extractExtraChannels(events, expectedSteps);

        FlowValidationRules rules = config.rules() == null
                ? new FlowValidationRules(50, true)
                : config.rules();

        String status;
        String message;

        if (firstMissingIndex == -1) {
            if (!extraChannels.isEmpty() && !rules.allowExtraEvents()) {
                status = "COMPLETE_WITH_UNEXPECTED_EVENTS";
                message = "Flow completed, but unexpected channels were observed.";
            } else {
                status = "COMPLETE";
                message = "Flow completed successfully.";
            }
        } else if (firstMissingIndex == 0) {
            status = "BROKEN";
            message = "Flow did not reach the first expected step: " + expectedSteps.get(0).channel();
        } else {
            ExpectedFlowStep previous = expectedSteps.get(firstMissingIndex - 1);
            ExpectedFlowStep missing = expectedSteps.get(firstMissingIndex);

            status = "BROKEN";
            message = "Flow likely stopped after " + previous.channel() + ", before " + missing.channel() + ".";
        }

        return new FlowValidationResult(
                status,
                message,
                stepResults,
                missingChannels,
                extraChannels,
                lastCompletedChannel
        );
    }

    private FlowDefinition findFlow(InvestigationConfig config, String flowId) {
        if (config == null || config.flows() == null) {
            return null;
        }

        return config.flows().stream()
                .filter(flow -> Objects.equals(flowId, flow.id()))
                .findFirst()
                .orElse(null);
    }

    private boolean matches(ExpectedFlowStep step, CorrelatedEvent event) {
        boolean protocolMatches = step.protocol() == null
                || step.protocol().isBlank()
                || step.protocol().equalsIgnoreCase(event.protocol());

        boolean channelMatches = step.channel() != null
                && step.channel().equals(event.channel());

        return protocolMatches && channelMatches;
    }

    private List<String> extractExtraChannels(List<CorrelatedEvent> events, List<ExpectedFlowStep> expectedSteps) {
        Set<String> expectedChannels = new LinkedHashSet<>();

        for (ExpectedFlowStep step : expectedSteps) {
            expectedChannels.add(step.channel());
        }

        Set<String> extra = new LinkedHashSet<>();

        for (CorrelatedEvent event : events) {
            if (!expectedChannels.contains(event.channel())) {
                extra.add(event.channel());
            }
        }

        return new ArrayList<>(extra);
    }
}