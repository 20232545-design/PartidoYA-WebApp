package com.partidoya.platform.teams.controllers;

import com.partidoya.platform.shared.exceptions.ErrorResource;
import com.partidoya.platform.teams.services.TeamImageStorageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TeamController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TeamExceptionHandler {
    @ExceptionHandler(TeamImageStorageException.class)
    public ResponseEntity<ErrorResource> handleImageStorage(TeamImageStorageException exception) {
        return new ResponseEntity<>(
                new ErrorResource("TEAM_IMAGE_STORAGE_UNAVAILABLE", exception.getMessage()),
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
