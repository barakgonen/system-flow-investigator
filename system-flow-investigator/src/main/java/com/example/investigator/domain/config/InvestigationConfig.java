package com.example.investigator.domain.config;

import java.util.List;

public record InvestigationConfig(
        String name,
        String description,
        List<ExpectedFlowStep> steps,
        FlowValidationRules rules
) {
}