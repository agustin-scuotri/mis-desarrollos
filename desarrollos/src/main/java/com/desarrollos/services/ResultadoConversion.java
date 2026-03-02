package com.desarrollos.services;

import java.util.List;

import com.desarrollos.entities.DocumentoConvertido;

public class ResultadoConversion {

    private final List<DocumentoConvertido> exitosos;
    private final List<ErrorFactura>        errores;

    public ResultadoConversion(List<DocumentoConvertido> exitosos, List<ErrorFactura> errores) {
        this.exitosos = exitosos;
        this.errores  = errores;
    }

    public List<DocumentoConvertido> getExitosos() { return exitosos; }
    public List<ErrorFactura>        getErrores()  { return errores; }
}
