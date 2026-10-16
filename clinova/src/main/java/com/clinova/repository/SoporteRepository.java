package com.clinova.repository;

import com.clinova.entity.Soporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SoporteRepository extends JpaRepository<Soporte, Long> {
    List<Soporte> findByHojaVidaId(Long hojaVidaId);
    List<Soporte> findByHojaVidaIdOrderByIdDesc(Long hojaVidaId);

    @Query("SELECT s FROM Soporte s LEFT JOIN FETCH s.hojaVida WHERE s.id = :id")
    Optional<Soporte> findByIdWithHojaVida(@Param("id") Long id);
}