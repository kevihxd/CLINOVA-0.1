package com.clinova.repository;

import com.clinova.entity.GrupoDistribucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GrupoDistribucionRepository extends JpaRepository<GrupoDistribucion, Long> {
    Optional<GrupoDistribucion> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);
}
