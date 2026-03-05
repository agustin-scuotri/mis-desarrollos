package com.desarrollos.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        return repository.findAll();
    }

    public Archivo buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Archivo no encontrado"));
    }

    @Transactional
    @CacheEvict(value = "archivos-pendientes", allEntries = true)
    public void borrar(Archivo entidad) {
        repository.delete(entidad);
    }

    @Transactional
    @CacheEvict(value = "archivos-pendientes", allEntries = true)
    public Archivo guardar(Archivo entidad) throws Exception {
        if (entidad.getCodigo() == null || entidad.getCodigo().trim().isEmpty()) {
            throw new Exception("ERROR_CODIGO_NULO");
        }

        if (entidad.getId() == null && repository.existsByCodigo(entidad.getCodigo())) {
            throw new Exception("ERROR_CODIGO_EN_USO");
        }

        return repository.save(entidad);
    }

    @Cacheable("archivos-pendientes")
    public List<Archivo> listarNoConvertidos() {
        return repository.findByEstadoConversion("PENDIENTE");
    }

    @Transactional
    @CacheEvict(value = "archivos-pendientes", allEntries = true)
    public void actualizarEstado(Archivo archivo, String estado) {
        actualizarEstado(archivo, estado, null);
    }

    @Transactional
    @CacheEvict(value = "archivos-pendientes", allEntries = true)
    public void actualizarEstado(Archivo archivo, String estado, String mensajeError) {
        Archivo managed = repository.findById(archivo.getId())
                .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));
        managed.setEstadoConversion(estado);
        managed.setMensajeError(mensajeError);
        repository.save(managed);
    }

    // ── Paginación server-side para AbmArchivosView ───────────────────────────
    public List<Archivo> listarPaginado(int page, int size, String codigo, String nombre, String estado, Sort sort) {
        Pageable pageable = PageRequest.of(page, Math.max(size, 1), sort);
        return repository.findFiltrado(
                codigo != null ? codigo : "",
                nombre != null ? nombre.toLowerCase() : "",
                estado != null ? estado : "",
                pageable);
    }

    public long contarFiltrado(String codigo, String nombre, String estado) {
        return repository.countFiltrado(
                codigo != null ? codigo : "",
                nombre != null ? nombre.toLowerCase() : "",
                estado != null ? estado : "");
    }

    @Transactional
    public Archivo buscarPorIdConContenido(Long id) {
        Archivo archivo = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));
        if (archivo.getContenido() != null) {
            int len = archivo.getContenido().length;
        }
        return archivo;
    }
}
