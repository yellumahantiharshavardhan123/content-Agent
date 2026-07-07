package com.arjunsports.contentagent.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entityName, Object identifier) {
        return new ResourceNotFoundException(entityName + " not found with id: " + identifier);
    }
}
