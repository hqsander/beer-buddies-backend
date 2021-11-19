package com.hqsander.beerbuddies.exception.custom;

public class UsernameExistException extends Exception {
    public UsernameExistException(String message) {
        super(message);
    }
}
