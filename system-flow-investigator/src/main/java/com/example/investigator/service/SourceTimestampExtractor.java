package com.example.investigator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SourceTimestampExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Instant extract(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);

            JsonNode timestampNode = firstExisting(
                    root,
                    "timestamp",
                    "sourceSentAt",
                    "sentTime",
                    "sentAt"
            );

            if (timestampNode == null || timestampNode.isNull()) {
                return null;
            }

            if (timestampNode.isNumber()) {
                long value = timestampNode.asLong();

                // If looks like epoch millis
                if (value > 10_000_000_000L) {
                    return Instant.ofEpochMilli(value);
                }

                // Otherwise treat as epoch seconds
                return Instant.ofEpochSecond(value);
            }

            if (timestampNode.isTextual()) {
                return Instant.parse(timestampNode.asText());
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode firstExisting(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode node = root.get(field);
            if (node != null) {
                return node;
            }
        }
        return null;
    }
}