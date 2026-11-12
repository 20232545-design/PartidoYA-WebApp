package com.partidoya.platform.shared.exceptions;

import com.partidoya.platform.shared.exceptions.ForbiddenActionException;
import com.partidoya.platform.shared.exceptions.ResourceConflictException;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import com.partidoya.platform.shared.exceptions.ErrorResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResource> handleNotFound(ResourceNotFoundException ex) {
        return new ResponseEntity<>(new ErrorResource("NOT_FOUND", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResource> handleNoResource(NoResourceFoundException ex) {
        return new ResponseEntity<>(new ErrorResource("NOT_FOUND", "Endpoint not found"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResource> handleConflict(ResourceConflictException ex) {
        return new ResponseEntity<>(new ErrorResource("CONFLICT", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResource> handleBadRequest(IllegalArgumentException ex) {
        return new ResponseEntity<>(new ErrorResource("VALIDATION_ERROR", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    /** Cuerpo JSON mal formado o con valores que no encajan en el tipo (por ejemplo, un enum desconocido). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResource> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return new ResponseEntity<>(
                new ErrorResource("VALIDATION_ERROR", "El cuerpo de la solicitud no es válido."),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<ErrorResource> handleForbidden(ForbiddenActionException ex) {
        return new ResponseEntity<>(new ErrorResource("FORBIDDEN", ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResource> handleIllegalState(IllegalStateException ex) {
        return new ResponseEntity<>(new ErrorResource("BUSINESS_RULE_VIOLATION", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResource> handleUnexpected(Exception ex) {
        log.error("Unexpected error handling request", ex);
        return new ResponseEntity<>(
                new ErrorResource("UNEXPECTED_ERROR", "Ocurrió un error inesperado. Inténtalo nuevamente."),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
