package com.clinova.repository;

import com.clinova.entity.RegistroVacuna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RegistroVacunaRepository extends JpaRepository<RegistroVacuna, Long> {
    boolean existsByVacunaId(Long vacunaId);
    List<RegistroVacuna> findByPersonaId(Long personaId);
}