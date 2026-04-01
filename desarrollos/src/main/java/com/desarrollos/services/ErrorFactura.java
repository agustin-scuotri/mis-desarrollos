package com.desarrollos.services;

import java.util.List;

public class ErrorFactura {

    public enum Tipo { DUPLICADA, TOTAL_NEGATIVO, CAMPOS_FALTANTES }

    private final int          numero;
    private final Tipo         tipo;
    private final String       detalle;
    private final List<String> camposFaltantesList;

    private ErrorFactura(int numero, Tipo tipo, String detalle, List<String> camposFaltantesList) {
        this.numero              = numero;
        this.tipo                = tipo;
        this.detalle             = detalle;
        this.camposFaltantesList = camposFaltantesList;
    }

    public static ErrorFactura duplicada(int numero, FacturaDuplicadaException ex) {
        return new ErrorFactura(numero, Tipo.DUPLICADA,
                "Duplicada — CUIT: " + ex.getCuit() + " | Nro: " + ex.getNumeroComprobante(),
                null);
    }

    public static ErrorFactura totalNegativo(int numero, TotalNegativoException ex) {
        return new ErrorFactura(numero, Tipo.TOTAL_NEGATIVO,
                "Total negativo (" + ex.getTotal() + ")",
                null);
    }

    public static ErrorFactura camposFaltantes(int numero, List<String> faltantes) {
        return new ErrorFactura(numero, Tipo.CAMPOS_FALTANTES,
                "Campos obligatorios faltantes: " + String.join(", ", faltantes),
                List.copyOf(faltantes));
    }

    public int          getNumero()          { return numero; }
    public Tipo         getTipo()            { return tipo; }
    public String       getDetalle()         { return detalle; }
    public List<String> getCamposFaltantes() { return camposFaltantesList; }
}
