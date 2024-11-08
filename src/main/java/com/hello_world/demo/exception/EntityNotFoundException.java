
package com.hello_world.demo.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String entity, String field, Object value) {
        super(String.format("%s not found with %s: %s", entity, field, value));
    }
}