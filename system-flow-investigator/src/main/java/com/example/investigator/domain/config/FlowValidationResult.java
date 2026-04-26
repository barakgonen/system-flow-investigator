package com.example.investigator.domain.config;

import java.util.List;

public record FlowValidationResult(
        String status,
        String message,
        List<ExpectedFlowStepResult> steps,
        List<String> missingChannels,
        List<String> extraChannels,
        String lastCompletedChannel
) {
}