package com.clinova.repository;

import com.clinova.entity.ApoyoTerapeuticoIngreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApoyoTerapeuticoIngresoRepository extends JpaRepository<ApoyoTerapeuticoIngreso, Long> {

    Optional<ApoyoTerapeuticoIngreso> findByNumeroIngresoIgnoreCase(String numeroIngreso);

    @Query("""
        SELECT i FROM ApoyoTerapeuticoIngreso i
        WHERE (:q IS NULL OR :q = ''
            OR LOWER(i.numeroIngreso) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(COALESCE(i.documentoPaciente, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(i.nombrePaciente) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:estado IS NULL OR :estado = '' OR UPPER(i.estado) = UPPER(:estado))
        ORDER BY i.fechaActualizacion DESC, i.id DESC
    """)
    List<ApoyoTerapeuticoIngreso> buscar(@Param("q") String q, @Param("estado") String estado);
}
