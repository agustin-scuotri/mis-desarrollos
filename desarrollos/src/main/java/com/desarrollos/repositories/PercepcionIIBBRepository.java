package com.desarrollos.repositories;

import com.desarrollos.entities.PercepcionIIBB;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PercepcionIIBBRepository extends JpaRepository<PercepcionIIBB, Long> {
}
