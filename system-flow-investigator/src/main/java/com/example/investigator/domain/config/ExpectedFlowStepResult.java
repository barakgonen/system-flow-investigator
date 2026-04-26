package com.example.investigator.domain.config;

public record ExpectedFlowStepResult(
        int index,
        String protocol,
        String channel,
        String label,
        boolean found,
        Long deltaFromPreviousSourceMs,
        Long deltaFromPreviousObservedMs
) {
}