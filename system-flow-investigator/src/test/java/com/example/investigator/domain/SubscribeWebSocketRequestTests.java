package com.example.investigator.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SubscribeWebSocketRequestTests {

    @Test
    public void testConstructorAndAccessors() {
        // == Arrange & Act
        SubscribeWebSocketRequest request = new SubscribeWebSocketRequest(
                "myConnection", "my-channel", true
        );

        // == Assert
        assertEquals("myConnection", request.connectionName());
        assertEquals("my-channel", request.logicalChannel());
        assertTrue(request.persistToFile());
    }

    @Test
    public void testEquality_equalObjects() {
        // == Arrange
        SubscribeWebSocketRequest request1 = new SubscribeWebSocketRequest(
                "myConnection", "my-channel", true
        );
        SubscribeWebSocketRequest request2 = new SubscribeWebSocketRequest(
                "myConnection", "my-channel", true
        );

        // == Act & Assert
        assertEquals(request1, request2);
    }

    @Test
    public void testEquality_differentObjects() {
        // == Arrange
        SubscribeWebSocketRequest request1 = new SubscribeWebSocketRequest(
                "myConnection", "my-channel", true
        );
        SubscribeWebSocketRequest request2 = new SubscribeWebSocketRequest(
                "otherConnection", "other-channel", false
        );

        // == Act & Assert
        assertNotEquals(request1, request2);
    }

    @Test
    public void testHashCode_equalObjects() {
        // == Arrange
        SubscribeWebSocketRequest request1 = new SubscribeWebSocketRequest(
                "myConnection", "my-channel", true
        );
        SubscribeWebSocketRequest request2 = new SubscribeWebSocketRequest(
                "myConnection", "my-channel", true
        );

        // == Act & Assert
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    public void testToString_containsAllFields() {
        // == Arrange
        SubscribeWebSocketRequest request = new SubscribeWebSocketRequest(
                "myConnection", "my-channel", true
        );

        // == Act
        String result = request.toString();

        // == Assert
        assertTrue(result.contains("myConnection"));
        assertTrue(result.contains("my-channel"));
        assertTrue(result.contains("true"));
    }

    @Test
    public void testPersistToFile_false() {
        // == Arrange & Act
        SubscribeWebSocketRequest request = new SubscribeWebSocketRequest(
                "myConnection", "my-channel", false
        );

        // == Assert
        assertFalse(request.persistToFile());
    }

    @Test
    public void testNullFields() {
        // == Arrange & Act
        SubscribeWebSocketRequest request = new SubscribeWebSocketRequest(
                null, null, false
        );

        // == Assert
        assertNull(request.connectionName());
        assertNull(request.logicalChannel());
        assertFalse(request.persistToFile());
    }
}