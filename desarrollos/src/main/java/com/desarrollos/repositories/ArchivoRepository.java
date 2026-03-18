package com.desarrollos.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.desarrollos.entities.Archivo;

@Repository
public interface ArchivoRepository extends JpaRepository<Archivo, Long> {

    boolean existsByCodigo(String codigo);

    Optional<Archivo> findByCodigo(String codigo);
    List<Archivo> findByConvertidoFalse();
    List<Archivo> findByEstadoConversion(String estadoConversion);

    Optional<Archivo> findFirstByHashContenidoAndEstadoConversion(String hashContenido, String estadoConversion);

    @Query(value = "SELECT codigo FROM archivos ORDER BY CAST(codigo AS INTEGER) DESC LIMIT 1", nativeQuery = true)
    String findUltimoCodigo();

    // ── Para timeline y comparativa ───────────────────────────────────────────
    List<Archivo> findTop10ByOrderByFechaCreacionDesc();
    List<Archivo> findByEstadoConversionOrderByFechaCreacionDesc(String estado, Pageable pageable);
    long countByFechaCreacionBetween(java.time.LocalDateTime desde, java.time.LocalDateTime hasta);
    long countByEstadoConversionAndFechaCreacionBetween(String estado, java.time.LocalDateTime desde, java.time.LocalDateTime hasta);

    // ── Paginación con filtros ────────────────────────────────────────────────
    @Query("SELECT a FROM Archivo a WHERE " +
           "(:codigo = '' OR a.codigo LIKE CONCAT('%', :codigo, '%')) AND " +
           "(:nombre = '' OR LOWER(a.nombre) LIKE CONCAT('%', :nombre, '%')) AND " +
           "(:estado = '' OR a.estadoConversion = :estado)")
    List<Archivo> findFiltrado(@Param("codigo") String codigo,
                               @Param("nombre") String nombre,
                               @Param("estado") String estado,
                               Pageable pageable);

    @Query("SELECT COUNT(a) FROM Archivo a WHERE " +
           "(:codigo = '' OR a.codigo LIKE CONCAT('%', :codigo, '%')) AND " +
           "(:nombre = '' OR LOWER(a.nombre) LIKE CONCAT('%', :nombre, '%')) AND " +
           "(:estado = '' OR a.estadoConversion = :estado)")
    long countFiltrado(@Param("codigo") String codigo,
                       @Param("nombre") String nombre,
                       @Param("estado") String estado);
}