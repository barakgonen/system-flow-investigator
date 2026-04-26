package com.example.investigator.domain.config;

public record ExpectedFlowStep(
        int index,
        String protocol,
        String channel,
        String label
) {
}