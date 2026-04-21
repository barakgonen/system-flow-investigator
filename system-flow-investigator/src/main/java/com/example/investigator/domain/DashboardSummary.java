package com.example.investigator.domain;

import java.util.List;

public record DashboardSummary(
        boolean mqttConnected,
        int observedTopicCount,
        int observedWebSocketChannelCount,
        int recentEventCount,
        List<String> latestTopics,
        List<String> latestTraceIds
) {
}