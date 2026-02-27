package com.desarrollos.repositories;

import com.desarrollos.entities.DocumentoConvertido;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentoConvertidoRepository extends JpaRepository<DocumentoConvertido, Long> {

    // Busca documentos previos del mismo archivo (para re-conversión limpia)
    List<DocumentoConvertido> findByArchivo_Id(Long archivoId);

    // ── Paginación con filtros (excluye PROCESADO_ERROR) ──────────────────────
    @Query("SELECT d FROM DocumentoConvertido d LEFT JOIN d.archivo a WHERE " +
           "(a IS NULL OR a.estadoConversion != 'PROCESADO_ERROR') AND " +
           "(:codigo = '' OR (a IS NOT NULL AND a.codigo LIKE CONCAT('%', :codigo, '%'))) AND " +
           "(:nombre = '' OR (a IS NOT NULL AND LOWER(a.nombre) LIKE CONCAT('%', :nombre, '%'))) AND " +
           "(:cuit = '' OR d.cuit LIKE CONCAT('%', :cuit, '%')) AND " +
           "(:centroEmision = '' OR LOWER(d.centroEmision) LIKE CONCAT('%', :centroEmision, '%')) AND " +
           "(:comprobante = '' OR d.numeroComprobante LIKE CONCAT('%', :comprobante, '%')) " +
           "ORDER BY d.fechaConversion DESC")
    List<DocumentoConvertido> findFiltrado(@Param("codigo") String codigo,
                                           @Param("nombre") String nombre,
                                           @Param("cuit") String cuit,
                                           @Param("centroEmision") String centroEmision,
                                           @Param("comprobante") String comprobante,
                                           Pageable pageable);

    @Query("SELECT COUNT(d) FROM DocumentoConvertido d LEFT JOIN d.archivo a WHERE " +
           "(a IS NULL OR a.estadoConversion != 'PROCESADO_ERROR') AND " +
           "(:codigo = '' OR (a IS NOT NULL AND a.codigo LIKE CONCAT('%', :codigo, '%'))) AND " +
           "(:nombre = '' OR (a IS NOT NULL AND LOWER(a.nombre) LIKE CONCAT('%', :nombre, '%'))) AND " +
           "(:cuit = '' OR d.cuit LIKE CONCAT('%', :cuit, '%')) AND " +
           "(:centroEmision = '' OR LOWER(d.centroEmision) LIKE CONCAT('%', :centroEmision, '%')) AND " +
           "(:comprobante = '' OR d.numeroComprobante LIKE CONCAT('%', :comprobante, '%'))")
    long countFiltrado(@Param("codigo") String codigo,
                       @Param("nombre") String nombre,
                       @Param("cuit") String cuit,
                       @Param("centroEmision") String centroEmision,
                       @Param("comprobante") String comprobante);

    // ── Fechas de conversión para gráfico ─────────────────────────────────────
    @Query("SELECT d.fechaConversion FROM DocumentoConvertido d WHERE d.fechaConversion >= :desde")
    List<LocalDateTime> findFechasDesde(@Param("desde") LocalDateTime desde);
}