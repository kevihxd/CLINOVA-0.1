package com.clinova.repository;

import com.clinova.entity.Soporte;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SoporteRepository extends JpaRepository<Soporte, Long> {
    List<Soporte> findByHojaVidaId(Long hojaVidaId);
}