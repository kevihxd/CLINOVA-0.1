package com.clinova.repository;

import com.clinova.entity.HojaVida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HojaVidaRepository extends JpaRepository<HojaVida, Long> {

    Optional<HojaVida> findByCedula(String cedula);
    Optional<HojaVida> findByUsuario_Id(Long usuarioId);

}