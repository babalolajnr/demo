package com.hello_world.demo.exception;

import com.hello_world.demo.model.dto.ApiError;
import com.hello_world.demo.model.dto.ApiValidationError;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST);
        apiError.setMessage("Validation error");

        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                apiError.addSubError(new ApiValidationError(
                        fieldError.getObjectName(),
                        fieldError.getField(),
                        fieldError.getRejectedValue(),
                        fieldError.getDefaultMessage()));
            }
        });

        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(ResponseStatusException.class)
    protected ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
        ApiError apiError = new ApiError(HttpStatus.valueOf(ex.getStatusCode().value()));
        apiError.setMessage(ex.getReason());
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleAllExceptions(Exception ex) {
        ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, ex);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    protected ResponseEntity<Object> handleUserAlreadyExists(UnprocessableEntityException ex) {
        ApiError apiError = new ApiError(HttpStatus.CONFLICT);
        apiError.setMessage(ex.getMessage());
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    protected ResponseEntity<Object> handleInvalidCredentials(InvalidCredentialsException ex) {
        ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED);
        apiError.setMessage(ex.getMessage());
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException ex) {
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND);
        apiError.setMessage(ex.getMessage());
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    protected ResponseEntity<Object> handleEntityAlreadyExists(EntityAlreadyExistsException ex) {
        ApiError apiError = new ApiError(HttpStatus.CONFLICT);
        apiError.setMessage(ex.getMessage());
        return buildResponseEntity(apiError);
    }

    private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(apiError, apiError.getStatus());
    }
}