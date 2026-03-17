package com.clinova.repository;

import com.clinova.entity.ExperienciaLaboral;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExperienciaLaboralRepository extends JpaRepository<ExperienciaLaboral, Long> {
    List<ExperienciaLaboral> findByHojaVidaId(Long hojaVidaId);
}