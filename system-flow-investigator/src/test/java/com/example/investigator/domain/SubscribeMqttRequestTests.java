package com.example.investigator.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SubscribeMqttRequestTests {

    @Test
    public void testConstructorAndAccessors() {
        // == Arrange & Act
        SubscribeMqttRequest request = new SubscribeMqttRequest(
                "myConnection", "topic/#", true
        );

        // == Assert
        assertEquals("myConnection", request.connectionName());
        assertEquals("topic/#", request.topicFilter());
        assertTrue(request.persistToFile());
    }

    @Test
    public void testEquality_equalObjects() {
        // == Arrange
        SubscribeMqttRequest request1 = new SubscribeMqttRequest(
                "myConnection", "topic/#", true
        );
        SubscribeMqttRequest request2 = new SubscribeMqttRequest(
                "myConnection", "topic/#", true
        );

        // == Act & Assert
        assertEquals(request1, request2);
    }

    @Test
    public void testEquality_differentObjects() {
        // == Arrange
        SubscribeMqttRequest request1 = new SubscribeMqttRequest(
                "myConnection", "topic/#", true
        );
        SubscribeMqttRequest request2 = new SubscribeMqttRequest(
                "otherConnection", "other/#", false
        );

        // == Act & Assert
        assertNotEquals(request1, request2);
    }

    @Test
    public void testHashCode_equalObjects() {
        // == Arrange
        SubscribeMqttRequest request1 = new SubscribeMqttRequest(
                "myConnection", "topic/#", true
        );
        SubscribeMqttRequest request2 = new SubscribeMqttRequest(
                "myConnection", "topic/#", true
        );

        // == Act & Assert
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    public void testToString_containsAllFields() {
        // == Arrange
        SubscribeMqttRequest request = new SubscribeMqttRequest(
                "myConnection", "topic/#", true
        );

        // == Act
        String result = request.toString();

        // == Assert
        assertTrue(result.contains("myConnection"));
        assertTrue(result.contains("topic/#"));
        assertTrue(result.contains("true"));
    }

    @Test
    public void testPersistToFile_false() {
        // == Arrange & Act
        SubscribeMqttRequest request = new SubscribeMqttRequest(
                "myConnection", "topic/#", false
        );

        // == Assert
        assertFalse(request.persistToFile());
    }

    @Test
    public void testNullFields() {
        // == Arrange & Act
        SubscribeMqttRequest request = new SubscribeMqttRequest(
                null, null, false
        );

        // == Assert
        assertNull(request.connectionName());
        assertNull(request.topicFilter());
        assertFalse(request.persistToFile());
    }
}