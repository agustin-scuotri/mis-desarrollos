package com.desarrollos.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;

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
    private BigDecimal importe;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DocumentoConvertido getDocumento() { return documento; }
    public void setDocumento(DocumentoConvertido documento) { this.documento = documento; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }
}
