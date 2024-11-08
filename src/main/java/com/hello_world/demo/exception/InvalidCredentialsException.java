
package com.hello_world.demo.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message != null ? message : "Invalid Credentials");
    }
}