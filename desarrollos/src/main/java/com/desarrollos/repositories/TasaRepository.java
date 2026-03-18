package com.desarrollos.repositories;

import com.desarrollos.entities.Tasa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TasaRepository extends JpaRepository<Tasa, Long> {
}
