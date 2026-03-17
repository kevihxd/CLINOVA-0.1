package com.clinova.repository;

import com.clinova.entity.Educacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EducacionRepository extends JpaRepository<Educacion, Long> {
    List<Educacion> findByHojaVidaId(Long hojaVidaId);
}