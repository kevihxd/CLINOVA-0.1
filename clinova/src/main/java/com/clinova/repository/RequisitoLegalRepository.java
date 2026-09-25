package com.clinova.repository;

import com.clinova.entity.RequisitoLegal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RequisitoLegalRepository extends JpaRepository<RequisitoLegal, Long> {

    @Query("SELECT COUNT(r) FROM RequisitoLegal r WHERE r.calificacion = 'Cumple'")
    long countByCumple();

    @Query("SELECT COUNT(r) FROM RequisitoLegal r WHERE r.calificacion = 'No Cumple'")
    long countByNoCumple();

    @Query("SELECT COUNT(r) FROM RequisitoLegal r WHERE r.calificacion = 'Cumple Parcialmente'")
    long countByCumpleParcialmente();

    @Query("SELECT COUNT(r) FROM RequisitoLegal r WHERE r.calificacion IS NOT NULL AND r.calificacion <> 'No Aplica'")
    long countEvaluables();
}
