package com.desarrollos.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documentos_convertidos")
public class DocumentoConvertido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "archivo_id")
    private Archivo archivo;

    // Emisor del comprobante (1.1)
    private String cuit;
    private String razonSocial;
    private String situacionIva;
    private String direccion;
    private String ciudad;
    private String codigoPostal;
    private String provincia;
    private String pais;
    private String telefono;
    private String mail;

    // Datos del comprobante (1.2)
    private String codigoArca;
    private String letra;
    private String centroEmision;
    private String numeroComprobante;
    private LocalDate fechaEmision;
    private String cae;
    private LocalDate fechaVencimientoCae;

    // Moneda y cotización
    private String moneda;
    private BigDecimal cotizacion;

    // Orden de compra (1.4)
    private String ordenCompra;

    // Productos / conceptos (1.3)
    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoConcepto> productosConceptos = new ArrayList<>();

    // Netos gravados e IVA por alícuota (1.5)
    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NetoGravado> netosGravados = new ArrayList<>();

    // Neto no gravado (1.6)
    private BigDecimal subTotalNoGravado;

    // Percepciones IIBB (1.7)
    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PercepcionIIBB> percepcionesIIBB = new ArrayList<>();

    // Percepciones IVA (1.8)
    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PercepcionIVA> percepcionesIVA = new ArrayList<>();

    // Tasas
    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tasa> tasas = new ArrayList<>();

    // Descuentos y recargos
    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DescuentoRecargo> descuentosRecargos = new ArrayList<>();

    // Total (1.9)
    private BigDecimal total;

    // Impuesto interno / otros tributos
    private BigDecimal impuestoInterno;

    // Vencimientos
    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vencimiento> vencimientos = new ArrayList<>();

    @Column(length = 20000)
    private String jsonResultado;

    @Column(name = "fecha_conversion")
    private LocalDateTime fechaConversion;

    public DocumentoConvertido() {
        this.fechaConversion = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Archivo getArchivo() { return archivo; }
    public void setArchivo(Archivo archivo) { this.archivo = archivo; }
    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getSituacionIva() { return situacionIva; }
    public void setSituacionIva(String situacionIva) { this.situacionIva = situacionIva; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }
    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }
    public String getCodigoArca() { return codigoArca; }
    public void setCodigoArca(String codigoArca) { this.codigoArca = codigoArca; }
    public String getLetra() { return letra; }
    public void setLetra(String letra) { this.letra = letra; }
    public String getCentroEmision() { return centroEmision; }
    public void setCentroEmision(String centroEmision) { this.centroEmision = centroEmision; }
    public String getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(String numeroComprobante) { this.numeroComprobante = numeroComprobante; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }
    public String getCae() { return cae; }
    public void setCae(String cae) { this.cae = cae; }
    public LocalDate getFechaVencimientoCae() { return fechaVencimientoCae; }
    public void setFechaVencimientoCae(LocalDate fechaVencimientoCae) { this.fechaVencimientoCae = fechaVencimientoCae; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public BigDecimal getCotizacion() { return cotizacion; }
    public void setCotizacion(BigDecimal cotizacion) { this.cotizacion = cotizacion; }
    public String getOrdenCompra() { return ordenCompra; }
    public void setOrdenCompra(String ordenCompra) { this.ordenCompra = ordenCompra; }
    public List<ProductoConcepto> getProductosConceptos() { return productosConceptos; }
    public void setProductosConceptos(List<ProductoConcepto> productosConceptos) { this.productosConceptos = productosConceptos; }
    public List<NetoGravado> getNetosGravados() { return netosGravados; }
    public void setNetosGravados(List<NetoGravado> netosGravados) { this.netosGravados = netosGravados; }
    public BigDecimal getSubTotalNoGravado() { return subTotalNoGravado; }
    public void setSubTotalNoGravado(BigDecimal subTotalNoGravado) { this.subTotalNoGravado = subTotalNoGravado; }
    public List<PercepcionIIBB> getPercepcionesIIBB() { return percepcionesIIBB; }
    public void setPercepcionesIIBB(List<PercepcionIIBB> percepcionesIIBB) { this.percepcionesIIBB = percepcionesIIBB; }
    public List<PercepcionIVA> getPercepcionesIVA() { return percepcionesIVA; }
    public void setPercepcionesIVA(List<PercepcionIVA> percepcionesIVA) { this.percepcionesIVA = percepcionesIVA; }
    public List<Tasa> getTasas() { return tasas; }
    public void setTasas(List<Tasa> tasas) { this.tasas = tasas; }
    public List<DescuentoRecargo> getDescuentosRecargos() { return descuentosRecargos; }
    public void setDescuentosRecargos(List<DescuentoRecargo> descuentosRecargos) { this.descuentosRecargos = descuentosRecargos; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public BigDecimal getImpuestoInterno() { return impuestoInterno; }
    public void setImpuestoInterno(BigDecimal impuestoInterno) { this.impuestoInterno = impuestoInterno; }
    public List<Vencimiento> getVencimientos() { return vencimientos; }
    public void setVencimientos(List<Vencimiento> vencimientos) { this.vencimientos = vencimientos; }
    public String getJsonResultado() { return jsonResultado; }
    public void setJsonResultado(String jsonResultado) { this.jsonResultado = jsonResultado; }
    public LocalDateTime getFechaConversion() { return fechaConversion; }
    public void setFechaConversion(LocalDateTime fechaConversion) { this.fechaConversion = fechaConversion; }
}