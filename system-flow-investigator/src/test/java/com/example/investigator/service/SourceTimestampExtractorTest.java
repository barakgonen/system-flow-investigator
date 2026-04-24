package com.example.investigator.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTimestampExtractorTest {

    private final SourceTimestampExtractor extractor = new SourceTimestampExtractor();

    @Test
    void shouldExtractEpochMillisTimestamp() {
        Instant result = extractor.extract("""
                {"timestamp": 1710000000123}
                """);

        assertThat(result).isEqualTo(Instant.ofEpochMilli(1710000000123L));
    }

    @Test
    void shouldExtractEpochSecondsTimestamp() {
        Instant result = extractor.extract("""
                {"timestamp": 1710000000}
                """);

        assertThat(result).isEqualTo(Instant.ofEpochSecond(1710000000L));
    }

    @Test
    void shouldExtractIsoTimestamp() {
        Instant result = extractor.extract("""
                {"timestamp": "2026-04-24T10:15:30Z"}
                """);

        assertThat(result).isEqualTo(Instant.parse("2026-04-24T10:15:30Z"));
    }

    @Test
    void shouldSupportAlternativeFieldNames() {
        Instant result = extractor.extract("""
                {"sourceSentAt": "2026-04-24T10:15:30Z"}
                """);

        assertThat(result).isEqualTo(Instant.parse("2026-04-24T10:15:30Z"));
    }

    @Test
    void shouldReturnNullWhenTimestampMissing() {
        assertThat(extractor.extract("""
                {"traceId": "abc"}
                """)).isNull();
    }

    @Test
    void shouldReturnNullWhenPayloadInvalid() {
        assertThat(extractor.extract("not-json")).isNull();
    }

    @Test
    void shouldReturnNullWhenPayloadIsNull() {
        assertThat(extractor.extract(null)).isNull();
    }
}