package com.desarrollos.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desarrollos.entities.Archivo;
import com.desarrollos.repositories.ArchivoRepository;

@Service
public class ArchivoService {

    @Autowired
    private ArchivoRepository repository;

    public String obtenerProximoCodigo() {
        String ultimo = repository.findUltimoCodigo();
        
        if (ultimo == null || ultimo.trim().isEmpty()) {
            return "1";
        }
        
        try {
            int proximo = Integer.parseInt(ultimo) + 1;
            return String.valueOf(proximo); 
        } catch (NumberFormatException e) {
            return ultimo + "-1";
        }
    }
    
    public List<Archivo> listarTodos() {
        return repository.findAll(); // Trae todo lo que viste en tu base de datos
    }
    
 // En ArchivoService.java
    public Archivo buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Archivo no encontrado"));
    }
    
    @Transactional
    public void borrar(Archivo entidad) {
        // Podrías agregar validaciones aquí, por ejemplo:
        // si el archivo está siendo usado en otro proceso, no dejar borrar.
        repository.delete(entidad);
    }

    @Transactional
    public Archivo guardar(Archivo entidad) throws Exception {
        // Validamos que el código no sea nulo (Lógica que pediste)
        if (entidad.getCodigo() == null || entidad.getCodigo().trim().isEmpty()) {
            throw new Exception("ERROR_CODIGO_NULO"); 
        }

        // Validamos si el código está disponible
        if (entidad.getId() == null && repository.existsByCodigo(entidad.getCodigo())) {
            throw new Exception("ERROR_CODIGO_EN_USO");
        }

        return repository.save(entidad);
    }
    
    public List<Archivo> listarNoConvertidos() {
        return repository.findByConvertidoFalse();
    }
    
    @Transactional
    public Archivo buscarPorIdConContenido(Long id) {
        Archivo archivo = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));
        // Forzamos la carga del contenido
        if (archivo.getContenido() != null) {
            int len = archivo.getContenido().length;
        }
        return archivo;
    }
}