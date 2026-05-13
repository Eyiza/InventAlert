package com.inventalert.identityService.exception.handler;

import com.inventalert.identityService.dto.response.ErrorResponse;
import com.inventalert.identityService.exception.AssignmentNotFoundException;
import com.inventalert.identityService.exception.ComplaintNotFoundException;
import com.inventalert.identityService.exception.CompanyNotFoundException;
import com.inventalert.identityService.exception.EmailAlreadyExistsException;
import com.inventalert.identityService.exception.InvalidResetTokenException;
import com.inventalert.identityService.exception.UserAlreadyActiveException;
import com.inventalert.identityService.exception.UserAlreadyDeactivatedException;
import com.inventalert.identityService.exception.UserNotFoundException;
import com.inventalert.identityService.exception.WarehouseManagerConflictException;
import com.inventalert.identityService.exception.WarehouseRequiredException;
import com.inventalert.identityService.security.exception.DeactivatedUserException;
import com.inventalert.identityService.security.exception.InvalidCredentialsException;
import com.inventalert.identityService.security.exception.SuspendedCompanyException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCompanyNotFound(CompanyNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailConflict(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyDeactivatedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyDeactivated(UserAlreadyDeactivatedException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyActive(UserAlreadyActiveException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(AssignmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAssignmentNotFound(AssignmentNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ComplaintNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleComplaintNotFound(ComplaintNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(WarehouseRequiredException.class)
    public ResponseEntity<ErrorResponse> handleWarehouseRequired(WarehouseRequiredException ex) {
        return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidResetToken(InvalidResetTokenException ex) {
        return ResponseEntity.status(400).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(WarehouseManagerConflictException.class)
    public ResponseEntity<ErrorResponse> handleManagerConflict(WarehouseManagerConflictException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(401).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DeactivatedUserException.class)
    public ResponseEntity<ErrorResponse> handleDeactivatedUser(DeactivatedUserException ex) {
        return ResponseEntity.status(403).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(SuspendedCompanyException.class)
    public ResponseEntity<ErrorResponse> handleSuspendedCompany(SuspendedCompanyException ex) {
        return ResponseEntity.status(403).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));
        return ResponseEntity.status(400).body(new ErrorResponse(message));
    }
}
