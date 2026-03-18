package com.desarrollos.services;

public class TotalNegativoException extends RuntimeException {

    private final String total;

    public TotalNegativoException(String total) {
        super("Total negativo: " + total);
        this.total = total;
    }

    public String getTotal() { return total; }
}
