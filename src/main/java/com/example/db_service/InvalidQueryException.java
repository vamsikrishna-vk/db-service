package com.example.db_service;

public class InvalidQueryException extends RuntimeException{
    public InvalidQueryException(String message) {
        super(message);
    }
}
