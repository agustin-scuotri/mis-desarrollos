package com.desarrollos.repositories;

import com.desarrollos.entities.DocumentoConvertido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoConvertidoRepository extends JpaRepository<DocumentoConvertido, Long> {

    // Busca documentos previos del mismo archivo (para re-conversión limpia)
    List<DocumentoConvertido> findByArchivo_Id(Long archivoId);
}