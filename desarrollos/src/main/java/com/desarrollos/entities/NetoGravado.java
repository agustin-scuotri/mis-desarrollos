package com.desarrollos.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "netos_gravados")
public class NetoGravado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "documento_id")
    private DocumentoConvertido documento;

    private String alicuota;
    private String importeNetoGravado;
    private String iva;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DocumentoConvertido getDocumento() { return documento; }
    public void setDocumento(DocumentoConvertido documento) { this.documento = documento; }
    public String getAlicuota() { return alicuota; }
    public void setAlicuota(String alicuota) { this.alicuota = alicuota; }
    public String getImporteNetoGravado() { return importeNetoGravado; }
    public void setImporteNetoGravado(String importeNetoGravado) { this.importeNetoGravado = importeNetoGravado; }
    public String getIva() { return iva; }
    public void setIva(String iva) { this.iva = iva; }
}
