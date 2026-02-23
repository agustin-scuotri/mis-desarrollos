package com.desarrollos.repositories;

import com.desarrollos.entities.ProductoConcepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoConceptoRepository extends JpaRepository<ProductoConcepto, Long> {
}
