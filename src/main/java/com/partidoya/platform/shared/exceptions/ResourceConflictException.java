package com.partidoya.platform.shared.exceptions;

public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String resourceType, String reason) {
        super("%s conflict: %s".formatted(resourceType, reason));
    }
}
