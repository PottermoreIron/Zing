package com.pot.gateway.config;

public class GatewayConfigurationException extends RuntimeException {

    public GatewayConfigurationException(String message) {
        super(message);
    }

    public GatewayConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}