package com.prpa.Shortz.model.exceptions;

public class NoGettersFoundForMatchingFields extends RuntimeException{

    public NoGettersFoundForMatchingFields(String message) {
        super(message);
    }
}
