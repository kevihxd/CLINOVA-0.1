package com.clinova.repository;

import com.clinova.entity.ComentarioActa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioActaRepository extends JpaRepository<ComentarioActa, Long> {
    List<ComentarioActa> findByActaIdOrderByFechaCreacionDesc(Long actaId);
}