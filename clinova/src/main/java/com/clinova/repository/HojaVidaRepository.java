package com.clinova.repository;

import com.clinova.entity.HojaVida;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HojaVidaRepository extends JpaRepository<HojaVida, Long> {
    Optional<HojaVida> findByCedula(String cedula);
    Optional<HojaVida> findByUsuario_Id(Long usuarioId);
    Optional<HojaVida> findByKawakId(Long kawakId);

    /**
     * Carga todas las hojas de vida con sus colecciones en una sola query (evita N+1).
     * Ordenadas de más reciente a más antigua.
     */
    @EntityGraph(attributePaths = {"cargos", "sedes"})
    @Query("SELECT h FROM HojaVida h ORDER BY h.id DESC")
    List<HojaVida> findAllWithDetails();
}