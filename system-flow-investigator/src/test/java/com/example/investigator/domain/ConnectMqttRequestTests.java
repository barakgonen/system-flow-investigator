package com.example.investigator.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectMqttRequestTests {

    @Test
    public void testConstructorAndAccessors() {
        // == Arrange & Act
        ConnectMqttRequest request = new ConnectMqttRequest(
                "myConnection", "localhost", 1883, "client-1", "user", "pass"
        );

        // == Assert
        assertEquals("myConnection", request.connectionName());
        assertEquals("localhost", request.host());
        assertEquals(1883, request.port());
        assertEquals("client-1", request.clientId());
        assertEquals("user", request.username());
        assertEquals("pass", request.password());
    }

    @Test
    public void testEquality_equalObjects() {
        // == Arrange
        ConnectMqttRequest request1 = new ConnectMqttRequest(
                "myConnection", "localhost", 1883, "client-1", "user", "pass"
        );
        ConnectMqttRequest request2 = new ConnectMqttRequest(
                "myConnection", "localhost", 1883, "client-1", "user", "pass"
        );

        // == Act & Assert
        assertEquals(request1, request2);
    }

    @Test
    public void testEquality_differentObjects() {
        // == Arrange
        ConnectMqttRequest request1 = new ConnectMqttRequest(
                "myConnection", "localhost", 1883, "client-1", "user", "pass"
        );
        ConnectMqttRequest request2 = new ConnectMqttRequest(
                "otherConnection", "remotehost", 8883, "client-2", "admin", "secret"
        );

        // == Act & Assert
        assertNotEquals(request1, request2);
    }

    @Test
    public void testHashCode_equalObjects() {
        // == Arrange
        ConnectMqttRequest request1 = new ConnectMqttRequest(
                "myConnection", "localhost", 1883, "client-1", "user", "pass"
        );
        ConnectMqttRequest request2 = new ConnectMqttRequest(
                "myConnection", "localhost", 1883, "client-1", "user", "pass"
        );

        // == Act & Assert
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    public void testToString_containsAllFields() {
        // == Arrange
        ConnectMqttRequest request = new ConnectMqttRequest(
                "myConnection", "localhost", 1883, "client-1", "user", "pass"
        );

        // == Act
        String result = request.toString();

        // == Assert
        assertTrue(result.contains("myConnection"));
        assertTrue(result.contains("localhost"));
        assertTrue(result.contains("1883"));
        assertTrue(result.contains("client-1"));
        assertTrue(result.contains("user"));
        assertTrue(result.contains("pass"));
    }

    @Test
    public void testNullFields() {
        // == Arrange & Act
        ConnectMqttRequest request = new ConnectMqttRequest(
                null, null, 0, null, null, null
        );

        // == Assert
        assertNull(request.connectionName());
        assertNull(request.host());
        assertEquals(0, request.port());
        assertNull(request.clientId());
        assertNull(request.username());
        assertNull(request.password());
    }
}