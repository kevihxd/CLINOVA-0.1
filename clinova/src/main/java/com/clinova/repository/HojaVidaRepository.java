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

    @EntityGraph(attributePaths = {"cargos", "sedes", "usuario"})
    @Query("SELECT h FROM HojaVida h WHERE LOWER(h.cedula) = LOWER(:query) OR LOWER(CONCAT(COALESCE(h.nombres, ''), ' ', COALESCE(h.apellidos, ''))) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(h.nombres) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(h.apellidos) LIKE LOWER(CONCAT('%', :query, '%')) OR (h.usuario IS NOT NULL AND LOWER(h.usuario.username) = LOWER(:query)) ORDER BY h.id DESC")
    List<HojaVida> buscarPorCedulaONombre(String query);

    /**
     * Carga todas las hojas de vida con sus colecciones en una sola query (evita N+1).
     * Ordenadas de más reciente a más antigua.
     */
    @EntityGraph(attributePaths = {"cargos", "sedes", "usuario"})
    @Query("SELECT h FROM HojaVida h ORDER BY h.id DESC")
    List<HojaVida> findAllWithDetails();
}