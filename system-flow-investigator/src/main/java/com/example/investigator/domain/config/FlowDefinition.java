package com.example.investigator.domain.config;

import java.util.List;

public record FlowDefinition(
        String id,
        String name,
        String description,
        List<ExpectedFlowStep> steps
) {
}