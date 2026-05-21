package com.clinova.repository;

import com.clinova.entity.HojaVidaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HojaVidaHistorialRepository extends JpaRepository<HojaVidaHistorial, Long> {
    List<HojaVidaHistorial> findByHojaVidaIdOrderByFechaDesc(Long hojaVidaId);
}
