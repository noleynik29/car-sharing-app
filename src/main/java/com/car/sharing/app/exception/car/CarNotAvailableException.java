package com.car.sharing.app.exception.car;

public class CarNotAvailableException extends RuntimeException {
    public CarNotAvailableException(String message) {
        super(message);
    }
}
