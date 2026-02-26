package com.desarrollos.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.desarrollos.entities.Archivo;

@Repository
public interface ArchivoRepository extends JpaRepository<Archivo, Long> {

    boolean existsByCodigo(String codigo);

    Optional<Archivo> findByCodigo(String codigo);
    List<Archivo> findByConvertidoFalse();
    List<Archivo> findByEstadoConversion(String estadoConversion);

    @Query(value = "SELECT codigo FROM archivos ORDER BY CAST(codigo AS INTEGER) DESC LIMIT 1", nativeQuery = true)
    String findUltimoCodigo();
}