package com.desarrollos.repositories;

import com.desarrollos.entities.Vencimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VencimientoRepository extends JpaRepository<Vencimiento, Long> {
}
