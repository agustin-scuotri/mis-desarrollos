package com.desarrollos.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "productos_conceptos")
public class ProductoConcepto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "documento_id")
    private DocumentoConvertido documento;

    private String sku;

    @Column(length = 1000)
    private String descripcion;

    private String cantidad;
    private String precioUnitario;
    private String descuento;
    private String subTotal;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DocumentoConvertido getDocumento() { return documento; }
    public void setDocumento(DocumentoConvertido documento) { this.documento = documento; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCantidad() { return cantidad; }
    public void setCantidad(String cantidad) { this.cantidad = cantidad; }
    public String getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(String precioUnitario) { this.precioUnitario = precioUnitario; }
    public String getDescuento() { return descuento; }
    public void setDescuento(String descuento) { this.descuento = descuento; }
    public String getSubTotal() { return subTotal; }
    public void setSubTotal(String subTotal) { this.subTotal = subTotal; }
}
