package com.desarrollos.repositories;

import com.desarrollos.entities.NetoGravado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NetoGravadoRepository extends JpaRepository<NetoGravado, Long> {
}
