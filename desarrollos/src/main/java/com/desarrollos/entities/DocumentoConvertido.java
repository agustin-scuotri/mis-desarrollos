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

    // Emisor del comprobante
    private String cuit;
    private String razonSocial;
    private String situacionIva;
    private String domicilio;
    private String telefono;
    private String mail;

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
    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }
    public String getJsonResultado() { return jsonResultado; }
    public void setJsonResultado(String jsonResultado) { this.jsonResultado = jsonResultado; }
    public LocalDateTime getFechaConversion() { return fechaConversion; }
    public void setFechaConversion(LocalDateTime fechaConversion) { this.fechaConversion = fechaConversion; }
}