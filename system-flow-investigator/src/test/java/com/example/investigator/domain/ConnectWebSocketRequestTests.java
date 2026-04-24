package com.example.investigator.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectWebSocketRequestTests {

    @Test
    public void testConstructorAndAccessors() {
        // == Arrange & Act
        ConnectWebSocketRequest request = new ConnectWebSocketRequest(
                "myConnection", "ws://localhost:8080/ws"
        );

        // == Assert
        assertEquals("myConnection", request.connectionName());
        assertEquals("ws://localhost:8080/ws", request.url());
    }

    @Test
    public void testEquality_equalObjects() {
        // == Arrange
        ConnectWebSocketRequest request1 = new ConnectWebSocketRequest(
                "myConnection", "ws://localhost:8080/ws"
        );
        ConnectWebSocketRequest request2 = new ConnectWebSocketRequest(
                "myConnection", "ws://localhost:8080/ws"
        );

        // == Act & Assert
        assertEquals(request1, request2);
    }

    @Test
    public void testEquality_differentObjects() {
        // == Arrange
        ConnectWebSocketRequest request1 = new ConnectWebSocketRequest(
                "myConnection", "ws://localhost:8080/ws"
        );
        ConnectWebSocketRequest request2 = new ConnectWebSocketRequest(
                "otherConnection", "ws://remotehost:9090/ws"
        );

        // == Act & Assert
        assertNotEquals(request1, request2);
    }

    @Test
    public void testHashCode_equalObjects() {
        // == Arrange
        ConnectWebSocketRequest request1 = new ConnectWebSocketRequest(
                "myConnection", "ws://localhost:8080/ws"
        );
        ConnectWebSocketRequest request2 = new ConnectWebSocketRequest(
                "myConnection", "ws://localhost:8080/ws"
        );

        // == Act & Assert
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    public void testToString_containsAllFields() {
        // == Arrange
        ConnectWebSocketRequest request = new ConnectWebSocketRequest(
                "myConnection", "ws://localhost:8080/ws"
        );

        // == Act
        String result = request.toString();

        // == Assert
        assertTrue(result.contains("myConnection"));
        assertTrue(result.contains("ws://localhost:8080/ws"));
    }

    @Test
    public void testNullFields() {
        // == Arrange & Act
        ConnectWebSocketRequest request = new ConnectWebSocketRequest(null, null);

        // == Assert
        assertNull(request.connectionName());
        assertNull(request.url());
    }
}