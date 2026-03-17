package com.clinova.repository;

import com.clinova.entity.HojaVida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HojaVidaRepository extends JpaRepository<HojaVida, Long> {
    Optional<HojaVida> findByCedula(String cedula);
}