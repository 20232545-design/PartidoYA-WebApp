package com.partidoya.platform.teams.services;

public class TeamImageStorageException extends RuntimeException {
    public TeamImageStorageException(String message) {
        super(message);
    }

    public TeamImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
