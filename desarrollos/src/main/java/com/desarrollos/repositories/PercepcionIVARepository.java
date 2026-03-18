package com.desarrollos.repositories;

import com.desarrollos.entities.PercepcionIVA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PercepcionIVARepository extends JpaRepository<PercepcionIVA, Long> {
}
