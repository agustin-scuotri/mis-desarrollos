package com.desarrollos.repositories;

import com.desarrollos.entities.DescuentoRecargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DescuentoRecargoRepository extends JpaRepository<DescuentoRecargo, Long> {
}
