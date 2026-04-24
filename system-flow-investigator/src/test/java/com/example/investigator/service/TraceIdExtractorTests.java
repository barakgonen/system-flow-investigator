package com.example.investigator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TraceIdExtractorTests {

    private TraceIdExtractor extractor;

    @BeforeEach
    public void setUp() {
        extractor = new TraceIdExtractor();
    }

    // == null / blank / invalid input

    @Test
    public void testExtract_nullPayload_returnsNull() {
        assertNull(extractor.extract(null));
    }

    @Test
    public void testExtract_blankPayload_returnsNull() {
        assertNull(extractor.extract("   "));
    }

    @Test
    public void testExtract_emptyPayload_returnsNull() {
        assertNull(extractor.extract(""));
    }

    @Test
    public void testExtract_invalidJson_returnsNull() {
        assertNull(extractor.extract("not-json"));
    }

    @Test
    public void testExtract_malformedJson_returnsNull() {
        assertNull(extractor.extract("{traceId: missing-quotes}"));
    }

    // == no matching field

    @Test
    public void testExtract_noMatchingField_returnsNull() {
        assertNull(extractor.extract("{\"someOtherField\": \"value\"}"));
    }

    @Test
    public void testExtract_emptyJsonObject_returnsNull() {
        assertNull(extractor.extract("{}"));
    }

    @Test
    public void testExtract_matchingFieldWithNullValue_returnsNull() {
        assertNull(extractor.extract("{\"traceId\": null}"));
    }

    // == candidate fields

    @Test
    public void testExtract_traceId_returnsValue() {
        assertEquals("trace-123", extractor.extract("{\"traceId\": \"trace-123\"}"));
    }

    @Test
    public void testExtract_correlationId_returnsValue() {
        assertEquals("corr-456", extractor.extract("{\"correlationId\": \"corr-456\"}"));
    }

    @Test
    public void testExtract_requestId_returnsValue() {
        assertEquals("req-789", extractor.extract("{\"requestId\": \"req-789\"}"));
    }

    @Test
    public void testExtract_eventId_returnsValue() {
        assertEquals("evt-101", extractor.extract("{\"eventId\": \"evt-101\"}"));
    }

    @Test
    public void testExtract_transactionId_returnsValue() {
        assertEquals("txn-202", extractor.extract("{\"transactionId\": \"txn-202\"}"));
    }

    // == priority

    @Test
    public void testExtract_multipleMatchingFields_returnsFirstCandidateByPriority() {
        // traceId takes priority over correlationId
        assertEquals("trace-123", extractor.extract(
                "{\"correlationId\": \"corr-456\", \"traceId\": \"trace-123\"}"
        ));
    }

    @Test
    public void testExtract_correlationId_takesOverRequestId_whenTraceIdAbsent() {
        assertEquals("corr-456", extractor.extract(
                "{\"requestId\": \"req-789\", \"correlationId\": \"corr-456\"}"
        ));
    }

    // == edge cases

    @Test
    public void testExtract_nestedJson_doesNotMatchNestedFields() {
        // field is nested, not at root level — should not be found
        assertNull(extractor.extract("{\"nested\": {\"traceId\": \"trace-123\"}}"));
    }

    @Test
    public void testExtract_numericTraceId_returnsAsString() {
        assertEquals("12345", extractor.extract("{\"traceId\": 12345}"));
    }

    @Test
    public void testExtract_booleanTraceId_returnsAsString() {
        assertEquals("true", extractor.extract("{\"traceId\": true}"));
    }
}