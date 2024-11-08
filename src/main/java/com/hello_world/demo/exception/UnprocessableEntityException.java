
package com.hello_world.demo.exception;

public class UnprocessableEntityException extends RuntimeException {
    public UnprocessableEntityException(String message) {
        super(message != null ? message : "Unprocessable Entity");
    }
}