package com.desarrollos.services;

/** Se lanza cuando la API de Claude responde con overloaded_error (saturación transitoria). */
public class ApiSaturadaException extends RuntimeException {
    public ApiSaturadaException() {
        super("overloaded_error");
    }
}
