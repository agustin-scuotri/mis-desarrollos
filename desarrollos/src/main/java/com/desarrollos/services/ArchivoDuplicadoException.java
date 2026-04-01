package com.desarrollos.services;

public class ArchivoDuplicadoException extends RuntimeException {

    private final String codigoOriginal;
    private final String nombreOriginal;

    public ArchivoDuplicadoException(String codigoOriginal, String nombreOriginal) {
        super("El contenido de este archivo ya fue procesado anteriormente.");
        this.codigoOriginal = codigoOriginal;
        this.nombreOriginal = nombreOriginal;
    }

    public String getCodigoOriginal() { return codigoOriginal; }
    public String getNombreOriginal() { return nombreOriginal; }
}
