package com.desarrollos.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "percepciones_iva")
public class PercepcionIVA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "documento_id")
    private DocumentoConvertido documento;

    private String alicuota;
    private String importe;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DocumentoConvertido getDocumento() { return documento; }
    public void setDocumento(DocumentoConvertido documento) { this.documento = documento; }
    public String getAlicuota() { return alicuota; }
    public void setAlicuota(String alicuota) { this.alicuota = alicuota; }
    public String getImporte() { return importe; }
    public void setImporte(String importe) { this.importe = importe; }
}
