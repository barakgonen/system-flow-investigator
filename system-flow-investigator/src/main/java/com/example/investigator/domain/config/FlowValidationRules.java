package com.example.investigator.domain.config;

public record FlowValidationRules(
        long maxStepDurationMs,
        boolean allowExtraEvents
) {
}