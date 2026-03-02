package com.desarrollos.services;

public class ErrorFactura {

    public enum Tipo { DUPLICADA, TOTAL_NEGATIVO }

    private final int    numero;
    private final Tipo   tipo;
    private final String detalle;

    private ErrorFactura(int numero, Tipo tipo, String detalle) {
        this.numero  = numero;
        this.tipo    = tipo;
        this.detalle = detalle;
    }

    public static ErrorFactura duplicada(int numero, FacturaDuplicadaException ex) {
        return new ErrorFactura(numero, Tipo.DUPLICADA,
                "Duplicada — CUIT: " + ex.getCuit() + " | Nro: " + ex.getNumeroComprobante());
    }

    public static ErrorFactura totalNegativo(int numero, TotalNegativoException ex) {
        return new ErrorFactura(numero, Tipo.TOTAL_NEGATIVO,
                "Total negativo (" + ex.getTotal() + ")");
    }

    public int    getNumero()  { return numero; }
    public Tipo   getTipo()    { return tipo; }
    public String getDetalle() { return detalle; }
}
