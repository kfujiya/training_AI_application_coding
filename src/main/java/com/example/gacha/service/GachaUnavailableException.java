package com.example.gacha.service;

public class GachaUnavailableException extends RuntimeException {

    public GachaUnavailableException(String message) {
        super(message);
    }
}
