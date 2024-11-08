
package com.hello_world.demo.exception;

public class EntityAlreadyExistsException extends RuntimeException {
    public EntityAlreadyExistsException(String entity, String field, Object value) {
        super(String.format("%s already exists with %s: %s", entity, field, value));
    }
}