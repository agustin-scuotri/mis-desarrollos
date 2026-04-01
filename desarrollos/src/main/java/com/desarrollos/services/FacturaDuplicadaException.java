package com.desarrollos.services;

public class FacturaDuplicadaException extends RuntimeException {

    private final String cuit;
    private final String codigoArca;
    private final String centroEmision;
    private final String numeroComprobante;

    public FacturaDuplicadaException(String cuit, String codigoArca,
                                     String centroEmision, String numeroComprobante) {
        super("Factura duplicada: CUIT=" + cuit + " | Cód.ARCA=" + codigoArca
                + " | Centro=" + centroEmision + " | Nro=" + numeroComprobante);
        this.cuit              = cuit;
        this.codigoArca        = codigoArca;
        this.centroEmision     = centroEmision;
        this.numeroComprobante = numeroComprobante;
    }

    public String getCuit()              { return cuit; }
    public String getCodigoArca()        { return codigoArca; }
    public String getCentroEmision()     { return centroEmision; }
    public String getNumeroComprobante() { return numeroComprobante; }
}
