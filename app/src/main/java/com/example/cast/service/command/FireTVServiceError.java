package com.example.cast.service.command;

public class FireTVServiceError extends ServiceCommandError {

    public FireTVServiceError(String message) {
        super(message);
    }

    public FireTVServiceError(String message, Throwable e) {
        super(message);
        this.payload = e;
    }
}
