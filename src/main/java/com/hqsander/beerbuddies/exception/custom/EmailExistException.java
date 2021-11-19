package com.hqsander.beerbuddies.exception.custom;

public class EmailExistException extends Exception {
    public EmailExistException(String message) {
        super(message);
    }
}
