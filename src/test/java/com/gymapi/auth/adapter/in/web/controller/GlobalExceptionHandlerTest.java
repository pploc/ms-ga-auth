package com.gymapi.auth.adapter.in.web.controller;

import com.gymapi.auth.domain.exception.RoleNotFoundException;
import com.gymapi.auth.domain.exception.SystemRoleDeletionException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRoleNotFoundException() {
        RoleNotFoundException ex = new RoleNotFoundException("not found");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleRoleNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("not found", response.getBody().getMessage());
    }

    @Test
    void handleSystemRoleDeletionException() {
        SystemRoleDeletionException ex = new SystemRoleDeletionException("cannot delete");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleSystemRoleDeletionException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("cannot delete", response.getBody().getMessage());
    }

    @Test
    void handleGenericException() {
        Exception ex = new Exception("Generic error");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }

    @Test
    void handleValidationException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "name", "must not be null");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("name: must not be null"));
    }

    @Test
    void handlePermissionNotFoundException() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handlePermissionNotFoundException(
                new com.gymapi.auth.domain.exception.PermissionNotFoundException(""));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleUserRoleNotFoundException() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler
                .handleUserRoleNotFoundException(new com.gymapi.auth.domain.exception.UserRoleNotFoundException(""));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleDuplicateRoleException() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler
                .handleDuplicateRoleException(new com.gymapi.auth.domain.exception.DuplicateRoleException(""));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleDuplicatePermissionException() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDuplicatePermissionException(
                new com.gymapi.auth.domain.exception.DuplicatePermissionException(""));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleIllegalStateException() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler
                .handleIllegalStateException(new IllegalStateException(""));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void errorResponse_Getters() {
        GlobalExceptionHandler.ErrorResponse er = new GlobalExceptionHandler.ErrorResponse();
        OffsetDateTime now = OffsetDateTime.now();
        er.setStatus(200);
        er.setError("OK");
        er.setMessage("Msg");
        er.setTimestamp(now);

        assertEquals(200, er.getStatus());
        assertEquals("OK", er.getError());
        assertEquals("Msg", er.getMessage());
        assertEquals(now, er.getTimestamp());
    }
}
