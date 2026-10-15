package com.ems.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(Long id) {
        super("Employee not found with id " + id);
    }
}
