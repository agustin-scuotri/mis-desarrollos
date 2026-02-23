package com.desarrollos.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "vencimientos")
public class Vencimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "documento_id")
    private DocumentoConvertido documento;

    private String fecha;
    private String importe;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DocumentoConvertido getDocumento() { return documento; }
    public void setDocumento(DocumentoConvertido documento) { this.documento = documento; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getImporte() { return importe; }
    public void setImporte(String importe) { this.importe = importe; }
}
