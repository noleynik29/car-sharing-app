package com.car.sharing.app.exception.car;

public class CarAlreadyReturnedException extends RuntimeException {
    public CarAlreadyReturnedException(String message) {
        super(message);
    }
}
