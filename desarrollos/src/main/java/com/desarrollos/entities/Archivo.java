package com.desarrollos.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "archivos")
public class Archivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{archivo.codigo.nulo}")
    @Column(unique = true)
    private String codigo;

    @org.hibernate.annotations.Formula("CAST(codigo AS INTEGER)")
    private Integer codigoNumerico;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @Lob
    @org.hibernate.annotations.JdbcType(org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType.class)
    @Column(name = "contenido", columnDefinition = "bytea")
    private byte[] contenido;

    private String nombreOriginal;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    private boolean convertido;

    @Column(name = "estado_conversion")
    private String estadoConversion = "PENDIENTE";

    @Column(name = "mensaje_error", length = 2000)
    private String mensajeError;

    // ── Relación con DocumentoConvertido (un archivo puede tener múltiples facturas) ──
    @OneToMany(mappedBy = "archivo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentoConvertido> documentosConvertidos = new ArrayList<>();

    public Archivo() {
        this.fechaCreacion = LocalDateTime.now();
        this.convertido = false;
    }

    // --- GETTERS Y SETTERS ---
    public byte[] getContenido() { return contenido; }
    public void setContenido(byte[] contenido) { this.contenido = contenido; }
    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public boolean isConvertido() { return convertido; }
    public void setConvertido(boolean convertido) { this.convertido = convertido; }
    public String getEstadoConversion() { return estadoConversion; }
    public void setEstadoConversion(String estadoConversion) { this.estadoConversion = estadoConversion; }
    public String getMensajeError() { return mensajeError; }
    public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }
    public List<DocumentoConvertido> getDocumentosConvertidos() { return documentosConvertidos; }
    public void setDocumentosConvertidos(List<DocumentoConvertido> documentosConvertidos) { this.documentosConvertidos = documentosConvertidos; }
}