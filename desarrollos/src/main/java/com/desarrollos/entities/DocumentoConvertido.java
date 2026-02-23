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

    private String nombre;
    private String apellido;
    private String dni;

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
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getJsonResultado() { return jsonResultado; }
    public void setJsonResultado(String jsonResultado) { this.jsonResultado = jsonResultado; }
    public LocalDateTime getFechaConversion() { return fechaConversion; }
    public void setFechaConversion(LocalDateTime fechaConversion) { this.fechaConversion = fechaConversion; }
}