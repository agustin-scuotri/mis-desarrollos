package com.desarrollos.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_convertidos")
public class DocumentoConvertido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
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
    private String fechaEmision;
    private String cae;
    private String fechaVencimientoCae;

    // Moneda y cotización
    private String moneda;
    private String cotizacion;

    @Column(length = 5000)
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
    public String getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(String fechaEmision) { this.fechaEmision = fechaEmision; }
    public String getCae() { return cae; }
    public void setCae(String cae) { this.cae = cae; }
    public String getFechaVencimientoCae() { return fechaVencimientoCae; }
    public void setFechaVencimientoCae(String fechaVencimientoCae) { this.fechaVencimientoCae = fechaVencimientoCae; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getCotizacion() { return cotizacion; }
    public void setCotizacion(String cotizacion) { this.cotizacion = cotizacion; }
    public String getJsonResultado() { return jsonResultado; }
    public void setJsonResultado(String jsonResultado) { this.jsonResultado = jsonResultado; }
    public LocalDateTime getFechaConversion() { return fechaConversion; }
    public void setFechaConversion(LocalDateTime fechaConversion) { this.fechaConversion = fechaConversion; }
}