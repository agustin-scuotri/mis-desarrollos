package com.desarrollos.repositories;

import com.desarrollos.entities.DocumentoConvertido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DocumentoConvertidoRepository extends JpaRepository<DocumentoConvertido, Long> {

    // Borra documentos previos del mismo archivo (para re-conversión limpia)
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentoConvertido d WHERE d.archivo.id = :archivoId")
    void deleteByArchivoId(@Param("archivoId") Long archivoId);
}