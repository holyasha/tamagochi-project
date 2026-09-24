package com.example.tamagochi_api_contract.exeption;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(String.format("%s with id=%s not found", resourceName, resourceId));
    }
}
