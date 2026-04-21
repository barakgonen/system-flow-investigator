package com.example.investigator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TraceIdExtractor {

    private static final List<String> CANDIDATE_FIELDS = List.of(
            "traceId",
            "correlationId",
            "requestId",
            "eventId",
            "transactionId"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extract(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            for (String field : CANDIDATE_FIELDS) {
                JsonNode value = root.get(field);
                if (value != null && !value.isNull()) {
                    return value.asText();
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
}