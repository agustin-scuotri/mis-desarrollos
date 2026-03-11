package com.desarrollos.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;

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

    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuento;
    private BigDecimal subTotal;
    private String alicuotaIva;
    private String ordenCompra;
    private String remito;
    private String numeroDespacho;
    private String fechaDespacho;
    private String registroOficializacion;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DocumentoConvertido getDocumento() { return documento; }
    public void setDocumento(DocumentoConvertido documento) { this.documento = documento; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }
    public BigDecimal getSubTotal() { return subTotal; }
    public void setSubTotal(BigDecimal subTotal) { this.subTotal = subTotal; }
    public String getAlicuotaIva() { return alicuotaIva; }
    public void setAlicuotaIva(String alicuotaIva) { this.alicuotaIva = alicuotaIva; }
    public String getOrdenCompra() { return ordenCompra; }
    public void setOrdenCompra(String ordenCompra) { this.ordenCompra = ordenCompra; }
    public String getRemito() { return remito; }
    public void setRemito(String remito) { this.remito = remito; }
    public String getNumeroDespacho() { return numeroDespacho; }
    public void setNumeroDespacho(String numeroDespacho) { this.numeroDespacho = numeroDespacho; }
    public String getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(String fechaDespacho) { this.fechaDespacho = fechaDespacho; }
    public String getRegistroOficializacion() { return registroOficializacion; }
    public void setRegistroOficializacion(String registroOficializacion) { this.registroOficializacion = registroOficializacion; }
}
