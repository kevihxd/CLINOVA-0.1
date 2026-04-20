package com.clinova.repository;

import com.clinova.entity.Vacuna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VacunaRepository extends JpaRepository<Vacuna, Long> {
    Optional<Vacuna> findByNombre(String nombre);
}